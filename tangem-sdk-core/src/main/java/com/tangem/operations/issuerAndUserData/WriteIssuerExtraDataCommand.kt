package com.tangem.operations.issuerAndUserData

import com.tangem.*
import com.tangem.common.CompletionResult
import com.tangem.common.SuccessResponse
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.card.Card
import com.tangem.common.core.*
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag
import com.tangem.crypto.DefaultIssuerDataVerifier
import com.tangem.crypto.IssuerDataToVerify
import com.tangem.crypto.IssuerDataVerifier
import com.tangem.operations.Command

/**
 * This command writes Issuer Extra Data field and its issuer’s signature.
 * Issuer Extra Data is never changed or parsed from within the Tangem COS.
 * The issuer defines purpose of use, format and payload of Issuer Data.
 * For example, this field may contain a photo or biometric information for ID card products.
 * Because of the large size of Issuer_Extra_Data, a series of these commands have to be executed
 * to write entire Issuer_Extra_Data.
 * @property issuerData Data provided by issuer.
 * @property startingSignature Issuer’s signature with Issuer Data Private Key of [cardId],
 * @property finalizingSignature Issuer’s signature with Issuer Data Private Key of [cardId],
 * @property issuerDataCounter An optional counter that protect issuer data against replay attack.
 * @property issuerPublicKey A public key of an issuer.
 */
@Deprecated(message = "Use files instead")
class WriteIssuerExtraDataCommand(
    private val issuerData: ByteArray,
    private val startingSignature: ByteArray,
    private val finalizingSignature: ByteArray,
    private val issuerDataCounter: Int? = null,
    private var issuerPublicKey: ByteArray? = null,
    verifier: IssuerDataVerifier = DefaultIssuerDataVerifier()
) : Command<SuccessResponse>(), IssuerDataVerifier by verifier {

    private var mode: IssuerExtraDataMode = IssuerExtraDataMode.ReadOrStartWrite
    private var offset: Int = 0

    override fun performPreCheck(card: Card): TangemSdkError? {
        if (issuerData.size > MAX_SIZE) {
            return TangemSdkError.DataSizeTooLarge()
        }
        if (card.settings.isIssuerDataProtectedAgainstReplay && issuerDataCounter == null) {
            return TangemSdkError.MissingCounter()
        }

        issuerPublicKey = issuerPublicKey ?: card.issuer.publicKey
        if (!verifySignatures(issuerPublicKey!!, card.cardId)) {
            return TangemSdkError.VerificationFailed()
        }
        return null
    }

    override fun run(session: CardSession, callback: CompletionCallback<SuccessResponse>) {
        Log.command(this)
        writeData(session, callback)
    }

    override fun mapError(card: Card?, error: TangemError): TangemError {
        if (card?.settings?.isIssuerDataProtectedAgainstReplay == false) {
            when (error) {
                is TangemSdkError.InvalidParams -> return TangemSdkError.DataCannotBeWritten()
                is TangemSdkError.InvalidState -> return TangemSdkError.OverwritingDataIsProhibited()
            }
        }
        return error
    }

    private fun verifySignatures(publicKey: ByteArray, cardId: String): Boolean {
        val firstData = IssuerDataToVerify(cardId, null, issuerDataCounter, issuerData.size)
        val secondData = IssuerDataToVerify(cardId, issuerData, issuerDataCounter)

        return verify(publicKey, startingSignature, firstData) &&
                verify(publicKey, finalizingSignature, secondData)
    }

    private fun writeData(session: CardSession, callback: CompletionCallback<SuccessResponse>) {
        if (mode == IssuerExtraDataMode.WritePart) {
            session.viewDelegate.onDelay(issuerData.size, offset, SINGLE_WRITE_SIZE)
        }
        transceive(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    when (mode) {
                        IssuerExtraDataMode.ReadOrStartWrite -> {
                            mode = IssuerExtraDataMode.WritePart
                            writeData(session, callback)
                            return@transceive
                        }
                        IssuerExtraDataMode.WritePart -> {
                            offset += SINGLE_WRITE_SIZE
                            if (offset >= issuerData.size) {
                                mode = IssuerExtraDataMode.FinalizeWrite
                            }
                            writeData(session, callback)
                            return@transceive
                        }
                        IssuerExtraDataMode.FinalizeWrite -> {
                            callback(CompletionResult.Success(result.data))
                        }
                    }
                }
                is CompletionResult.Failure -> {
                    if (session.environment.config.handleErrors) {
                        val error = mapError(session.environment.card, result.error)
                        callback(CompletionResult.Failure(error))
                    }
                    callback(CompletionResult.Failure(result.error))
                }
            }
        }


    }

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.Pin, environment.accessCode.value)
        tlvBuilder.append(TlvTag.CardId, environment.card?.cardId)
        tlvBuilder.append(TlvTag.InteractionMode, mode)
        when (mode) {
            IssuerExtraDataMode.ReadOrStartWrite -> {
                tlvBuilder.append(TlvTag.Size, issuerData.size)
                tlvBuilder.append(TlvTag.IssuerDataSignature, startingSignature)
                tlvBuilder.append(TlvTag.IssuerDataCounter, issuerDataCounter)
            }
            IssuerExtraDataMode.WritePart -> {
                tlvBuilder.append(TlvTag.IssuerData, getDataToWrite())
                tlvBuilder.append(TlvTag.Offset, offset)
            }
            IssuerExtraDataMode.FinalizeWrite -> {
                tlvBuilder.append(TlvTag.IssuerDataSignature, finalizingSignature)
            }
        }

        return CommandApdu(Instruction.WriteIssuerData, tlvBuilder.serialize())
    }

    private fun getDataToWrite(): ByteArray = issuerData.copyOfRange(offset, offset + calculatePartSize())

    private fun calculatePartSize(): Int {
        val bytesLeft = issuerData.size - offset
        return if (bytesLeft < SINGLE_WRITE_SIZE) bytesLeft else SINGLE_WRITE_SIZE
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): SuccessResponse {
        val tlvData = apdu.getTlvData(environment.encryptionKey) ?: throw TangemSdkError.DeserializeApduFailed()

        return SuccessResponse(TlvDecoder(tlvData).decode(TlvTag.CardId))
    }

    companion object {
        const val SINGLE_WRITE_SIZE = 900
        const val MAX_SIZE = 32 * 1024
    }
}
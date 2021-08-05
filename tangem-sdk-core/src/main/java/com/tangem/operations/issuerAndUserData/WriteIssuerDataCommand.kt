package com.tangem.operations.issuerAndUserData

import com.tangem.common.SuccessResponse
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.card.Card
import com.tangem.common.core.SessionEnvironment
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag
import com.tangem.crypto.DefaultIssuerDataVerifier
import com.tangem.crypto.IssuerDataToVerify
import com.tangem.crypto.IssuerDataVerifier
import com.tangem.operations.Command

/**
 * This command writes 512-byte Issuer Data field and its issuer’s signature.
 * Issuer Data is never changed or parsed from within the Tangem COS. The issuer defines purpose of use,
 * format and payload of Issuer Data. For example, this field may contain information about
 * wallet balance signed by the issuer or additional issuer’s attestation data.
 * @property cardId CID, Unique Tangem card ID number.
 * @property issuerData Data provided by issuer.
 * @property issuerDataSignature Issuer’s signature of [issuerData] with Issuer Data Private Key (which is kept on card).
 * @property issuerDataCounter An optional counter that protect issuer data against replay attack.
 */
@Deprecated(message = "Use files instead")
class WriteIssuerDataCommand(
    private val issuerData: ByteArray,
    private val issuerDataSignature: ByteArray,
    private val issuerDataCounter: Int? = null,
    private var issuerPublicKey: ByteArray? = null,
    verifier: IssuerDataVerifier = DefaultIssuerDataVerifier()
) : Command<SuccessResponse>(), IssuerDataVerifier by verifier {

    override fun performPreCheck(card: Card): TangemSdkError? {
        if (issuerData.size > MAX_SIZE) {
            return TangemSdkError.DataSizeTooLarge()
        }
        if (card.settings.isIssuerDataProtectedAgainstReplay && issuerDataCounter == null) {
            return TangemSdkError.MissingCounter()
        }

        issuerPublicKey = issuerPublicKey ?: card.issuer.publicKey
        if (!verifySignature(issuerPublicKey!!, card.cardId)) {
            return TangemSdkError.VerificationFailed()
        }

        return null
    }

    private fun verifySignature(issuerPublicKey: ByteArray, cardId: String): Boolean {
        val data = IssuerDataToVerify(cardId, issuerData, issuerDataCounter)
        return verify(issuerPublicKey, issuerDataSignature, data)
    }

    override fun mapError(card: Card?, error: TangemError): TangemError {
        if (error is TangemSdkError.InvalidParams && card?.settings?.isIssuerDataProtectedAgainstReplay == true) {
            return TangemSdkError.DataCannotBeWritten()
        }
        return error
    }

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.Pin, environment.accessCode.value)
        tlvBuilder.append(TlvTag.CardId, environment.card?.cardId)
        tlvBuilder.append(TlvTag.IssuerData, issuerData)
        tlvBuilder.append(TlvTag.IssuerDataSignature, issuerDataSignature)
        tlvBuilder.append(TlvTag.IssuerDataCounter, issuerDataCounter)

        return CommandApdu(Instruction.WriteIssuerData, tlvBuilder.serialize())
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): SuccessResponse {
        val tlvData = apdu.getTlvData(environment.encryptionKey) ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)
        return SuccessResponse(decoder.decode(TlvTag.CardId))
    }

    companion object {
        const val MAX_SIZE = 512
    }
}
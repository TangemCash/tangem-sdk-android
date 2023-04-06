package com.tangem.operations.issuerAndUserData

import com.squareup.moshi.JsonClass
import com.tangem.*
import com.tangem.common.CompletionResult
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.card.Card
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.CardSession
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.SessionEnvironment
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.guard
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag
import com.tangem.crypto.DefaultIssuerDataVerifier
import com.tangem.crypto.IssuerDataToVerify
import com.tangem.crypto.IssuerDataVerifier
import com.tangem.operations.Command
import com.tangem.operations.CommandResponse
import java.io.ByteArrayOutputStream

enum class IssuerExtraDataMode(val code: Byte) {
    /**
     * This mode is required to read issuer extra data from the card. This mode is required to initiate
     * writing issuer extra data to the card.
     */
    ReadOrStartWrite(code = 1),

    /**
     * With this mode, the command writes part of issuer extra data
     * (block of a size [WriteIssuerExtraDataCommand.SINGLE_WRITE_SIZE]) to the card.
     */
    WritePart(code = 2),

    /**
     * This mode is used after the issuer extra data was fully written to the card.
     * Under this mode the command provides the issuer signature
     * to confirm the validity of data that was written to card.
     */
    FinalizeWrite(code = 3),
    ;

    companion object {
        private val values = values()
        fun byCode(code: Byte): IssuerExtraDataMode? = values.find { it.code == code }
    }
}

@JsonClass(generateAdapter = true)
class ReadIssuerExtraDataResponse(

    /**
     * CID, Unique Tangem card ID number.
     */
    val cardId: String,

    /**
     * Size of all Issuer_Extra_Data field.
     */
    val size: Int?,

    /**
     * Data defined by issuer.
     */
    val issuerData: ByteArray,

    /**
     * Issuer’s signature of [issuerData] with Issuer Data Private Key (which is kept on card).
     * Issuer’s signature of SHA256-hashed [cardId] concatenated with [issuerData]:
     * SHA256([cardId] | [issuerData]).
     * When flag [Settings.ProtectIssuerDataAgainstReplay] set in [SettingsMask] then signature of
     * SHA256-hashed CID Issuer_Data concatenated with and [issuerDataCounter]:
     * SHA256([cardId] | [issuerData] | [issuerDataCounter]).
     */
    val issuerDataSignature: ByteArray?,

    /**
     * An optional counter that protects issuer data against replay attack.
     * When flag [Settings.ProtectIssuerDataAgainstReplay] set in [SettingsMask]
     * then this value is mandatory and must increase on each execution of [WriteIssuerDataCommand].
     */
    val issuerDataCounter: Int?,
) : CommandResponse

/**
 * This command retrieves Issuer Extra Data field and its issuer’s signature.
 * Issuer Extra Data is never changed or parsed from within the Tangem COS. The issuer defines purpose of use,
 * format and payload of Issuer Data. . For example, this field may contain photo or
 * biometric information for ID card product. Because of the large size of Issuer_Extra_Data,
 * a series of these commands have to be executed to read the entire Issuer_Extra_Data.
 */
@Deprecated(message = "Use files instead")
class ReadIssuerExtraDataCommand(
    private var issuerPublicKey: ByteArray? = null,
    verifier: IssuerDataVerifier = DefaultIssuerDataVerifier(),
) : Command<ReadIssuerExtraDataResponse>(), IssuerDataVerifier by verifier {

    private val issuerData = ByteArrayOutputStream()
    private var offset: Int = 0
    private var issuerDataSize: Int = 0

    override fun performPreCheck(card: Card): TangemSdkError? {
        if (card.firmwareVersion >= FirmwareVersion.MultiWalletAvailable) {
            return TangemSdkError.NotSupportedFirmwareVersion()
        }

        return null
    }

    override fun run(session: CardSession, callback: CompletionCallback<ReadIssuerExtraDataResponse>) {
        val card = session.environment.card.guard {
            callback(CompletionResult.Failure(TangemSdkError.MissingPreflightRead()))
            return
        }

        issuerPublicKey = issuerPublicKey ?: card.issuer.publicKey
        readData(session, callback)
    }

    private fun readData(session: CardSession, callback: CompletionCallback<ReadIssuerExtraDataResponse>) {
        if (issuerDataSize != 0) {
            session.viewDelegate.onDelay(issuerDataSize, offset, WriteIssuerExtraDataCommand.SINGLE_WRITE_SIZE)
        }
        super.run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    if (result.data.size != null) {
                        if (result.data.size == 0) {
                            callback(CompletionResult.Success(result.data))
                            return@run
                        }
                        issuerDataSize = result.data.size
                    }
                    issuerData.write(result.data.issuerData)
                    if (result.data.issuerDataSignature == null) {
                        offset = issuerData.size()
                        readData(session, callback)
                    } else {
                        completeTask(result.data, callback)
                    }
                }
                is CompletionResult.Failure -> {
                    callback(CompletionResult.Failure(result.error))
                }
            }
        }
    }

    private fun completeTask(
        data: ReadIssuerExtraDataResponse,
        callback: CompletionCallback<ReadIssuerExtraDataResponse>,
    ) {
        val dataToVerify = IssuerDataToVerify(data.cardId, issuerData.toByteArray(), data.issuerDataCounter)
        if (verify(issuerPublicKey!!, data.issuerDataSignature!!, dataToVerify)) {
            val finalResult = ReadIssuerExtraDataResponse(
                data.cardId,
                issuerDataSize,
                issuerData.toByteArray(),
                data.issuerDataSignature,
                data.issuerDataCounter,
            )
            callback(CompletionResult.Success(finalResult))
        } else {
            callback(CompletionResult.Failure(TangemSdkError.VerificationFailed()))
        }
    }

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.Pin, environment.accessCode.value)
        tlvBuilder.append(TlvTag.CardId, environment.card?.cardId)
        tlvBuilder.append(TlvTag.InteractionMode, IssuerExtraDataMode.ReadOrStartWrite)
        tlvBuilder.append(TlvTag.Offset, offset)
        return CommandApdu(Instruction.ReadIssuerData, tlvBuilder.serialize())
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): ReadIssuerExtraDataResponse {
        val tlvData = apdu.getTlvData() ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)
        return ReadIssuerExtraDataResponse(
            cardId = decoder.decode(TlvTag.CardId),
            size = decoder.decodeOptional(TlvTag.Size),
            issuerData = decoder.decodeOptional(TlvTag.IssuerData) ?: byteArrayOf(),
            issuerDataSignature = decoder.decodeOptional(TlvTag.IssuerDataSignature),
            issuerDataCounter = decoder.decodeOptional(TlvTag.IssuerDataCounter),
        )
    }

    companion object {
        /**
         * This mode value specifies that this command retrieves Issuer EXTRA data from the card
         * (with value 0 the command will get instead simple Issuer Data from the card).
         */
        const val EXTRA_DATA_MODE = 1
    }
}

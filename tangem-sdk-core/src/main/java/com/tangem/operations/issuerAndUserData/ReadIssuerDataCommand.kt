package com.tangem.operations.issuerAndUserData

import com.squareup.moshi.JsonClass
import com.tangem.common.CompletionResult
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
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

@JsonClass(generateAdapter = true)
class ReadIssuerDataResponse(

    /**
     * CID, Unique Tangem card ID number.
     */
    val cardId: String,

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
    val issuerDataSignature: ByteArray,

    /**
     * An optional counter that protect issuer data against replay attack.
     * When flag [Settings.ProtectIssuerDataAgainstReplay] set in [SettingsMask]
     * then this value is mandatory and must increase on each execution of [WriteIssuerDataCommand].
     */
    val issuerDataCounter: Int?
) : CommandResponse


/**
 * This command returns 512-byte Issuer Data field and its issuer’s signature.
 * Issuer Data is never changed or parsed from within the Tangem COS. The issuer defines purpose of use,
 * format and payload of Issuer Data. For example, this field may contain information about
 * wallet balance signed by the issuer or additional issuer’s attestation data.
 * @property cardId CID, Unique Tangem card ID number.
 */
@Deprecated(message = "Use files instead")
class ReadIssuerDataCommand(
    private var issuerPublicKey: ByteArray? = null,
    verifier: IssuerDataVerifier = DefaultIssuerDataVerifier()
) : Command<ReadIssuerDataResponse>(), IssuerDataVerifier by verifier {

    override fun run(session: CardSession, callback: CompletionCallback<ReadIssuerDataResponse>) {
        val card = session.environment.card.guard {
            callback(CompletionResult.Failure(TangemSdkError.MissingPreflightRead()))
            return
        }

        issuerPublicKey = issuerPublicKey ?: card.issuer.publicKey
        super.run(session) { result ->
            when (result) {
                is CompletionResult.Failure -> callback(result)
                is CompletionResult.Success -> {
                    val data = result.data
                    if (data.issuerData.isEmpty()) {
                        callback(result)
                        return@run
                    }
                    val issuerDataToVerify = IssuerDataToVerify(data.cardId, data.issuerData, data.issuerDataCounter)
                    if (verify(issuerPublicKey!!, data.issuerDataSignature, issuerDataToVerify)) {
                        callback(result)
                    } else {
                        callback(CompletionResult.Failure(TangemSdkError.VerificationFailed()))
                    }
                }
            }
        }
    }

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.Pin, environment.accessCode.value)
        tlvBuilder.append(TlvTag.CardId, environment.card?.cardId)

        return CommandApdu(Instruction.ReadIssuerData, tlvBuilder.serialize())
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): ReadIssuerDataResponse {
        val tlvData = apdu.getTlvData(environment.encryptionKey) ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)
        return ReadIssuerDataResponse(
                decoder.decode(TlvTag.CardId),
                decoder.decode(TlvTag.IssuerData),
                decoder.decode(TlvTag.IssuerDataSignature),
                decoder.decodeOptional(TlvTag.IssuerDataCounter)
        )
    }
}
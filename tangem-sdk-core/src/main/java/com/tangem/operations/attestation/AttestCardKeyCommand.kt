package com.tangem.operations.attestation

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
import com.tangem.crypto.CryptoUtils
import com.tangem.operations.Command
import com.tangem.operations.CommandResponse

/**
 * Deserialized response from the Tangem card after `AttestCardKeyCommand`.
 */
@JsonClass(generateAdapter = true)
class AttestCardKeyResponse(
    val cardId: String,
    val salt: ByteArray,
    val cardSignature: ByteArray,
    val challenge: ByteArray,
) : CommandResponse {

    fun verify(cardPublicKey: ByteArray): Boolean {
        return CryptoUtils.verify(cardPublicKey, challenge + salt, cardSignature)
    }
}

/**
 * @property challenge Optional challenge. If null, it will be created automatically and returned in command response
 */
class AttestCardKeyCommand(
    private var challenge: ByteArray? = null,
) : Command<AttestCardKeyResponse>() {

    override fun run(session: CardSession, callback: CompletionCallback<AttestCardKeyResponse>) {
        challenge = challenge ?: CryptoUtils.generateRandomBytes(length = 16)
        val cardPublicKey = session.environment.card?.cardPublicKey.guard {
            callback(CompletionResult.Failure(TangemSdkError.MissingPreflightRead()))
            return
        }

        super.run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    val verified = result.data.verify(cardPublicKey)
                    if (verified) {
                        callback(CompletionResult.Success(result.data))
                    } else {
                        callback(CompletionResult.Failure(TangemSdkError.CardVerificationFailed()))
                    }
                }
                is CompletionResult.Failure -> callback(result)
            }
        }
    }

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val builder = TlvBuilder()
        builder.append(TlvTag.Pin, environment.accessCode.value)
        builder.append(TlvTag.CardId, environment.card?.cardId)
        builder.append(TlvTag.Challenge, challenge)
        return CommandApdu(Instruction.AttestCardKey, builder.serialize())
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): AttestCardKeyResponse {
        val tlv = apdu.getTlvData() ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlv)
        return AttestCardKeyResponse(
            cardId = decoder.decode(TlvTag.CardId),
            salt = decoder.decode(TlvTag.Salt),
            cardSignature = decoder.decode(TlvTag.CardSignature),
            challenge = challenge!!,
        )
    }
}

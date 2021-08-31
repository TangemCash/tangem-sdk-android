package com.tangem.operations.backup

import com.squareup.moshi.JsonClass
import com.tangem.common.CompletionResult
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.backup.ResetPin_CardToReset
import com.tangem.common.card.Card
import com.tangem.common.core.CardSession
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.SessionEnvironment
import com.tangem.common.core.TangemSdkError
import com.tangem.common.files.AuthorizeMode
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag
import com.tangem.operations.Command
import com.tangem.operations.CommandResponse
import com.tangem.operations.PreflightReadMode

/**
 * Response from the Tangem card after `CreateWalletCommand`.
 */
@JsonClass(generateAdapter = true)
class GetResetPinTokenResponse(
    /**
     * Unique Tangem card ID number
     */
    val cardId: String,
    /**
     * Session for backup
     */
    val cardToReset: ResetPin_CardToReset
) : CommandResponse

/**
 */
class GetResetPinTokenCommand : Command<GetResetPinTokenResponse>() {

    override fun requiresPasscode(): Boolean = false
    override fun preflightReadMode(): PreflightReadMode {
        return PreflightReadMode.ReadCardOnly
    }

    override fun performPreCheck(card: Card): TangemSdkError? {
        if (card.backupStatus!=Card.BackupStatus.Active) {
            return TangemSdkError.InvalidState()
        }
        return null
    }

    override fun run(session: CardSession, callback: CompletionCallback<GetResetPinTokenResponse>) {
        val card = session.environment.card ?: throw TangemSdkError.MissingPreflightRead()

        super.run(session) { result ->
            when (result) {
                is CompletionResult.Success -> callback(CompletionResult.Success(result.data))
                is CompletionResult.Failure -> callback(result)
            }
        }
    }

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.CardId, environment.card?.cardId)
        tlvBuilder.append(TlvTag.AuthorizeMode, AuthorizeMode.Token_Get.rawValue)

        return CommandApdu(Instruction.Authorize, tlvBuilder.serialize())
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): GetResetPinTokenResponse {
        val tlvData = apdu.getTlvData(environment.encryptionKey) ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)
        val cardToReset = ResetPin_CardToReset(
            token = decoder.decode(TlvTag.Challenge),
            attestSignature = decoder.decode(TlvTag.Backup_AttestSignature),
            backupKey = decoder.decode(TlvTag.Backup_MasterKey)
        )
        return GetResetPinTokenResponse(decoder.decode(TlvTag.CardId), cardToReset)
    }
}


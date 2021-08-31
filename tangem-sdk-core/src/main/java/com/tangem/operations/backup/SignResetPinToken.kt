package com.tangem.operations.backup

import com.squareup.moshi.JsonClass
import com.tangem.common.CompletionResult
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.backup.BackupMaster
import com.tangem.common.backup.BackupSession
import com.tangem.common.backup.ResetPinSession
import com.tangem.common.backup.ResetPin_CardToConfirm
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
import java.security.PublicKey

/**
 * Response from the Tangem card after `CreateWalletCommand`.
 */
@JsonClass(generateAdapter = true)
class SignResetPinTokenResponse(
    /**
     * Unique Tangem card ID number
     */
    val cardId: String,
    /**
     * Session for backup
     */
    val cartToConfirm: ResetPin_CardToConfirm
) : CommandResponse

/**
 */
class SignResetPinTokenCommand(private val resetPinSession: ResetPinSession
) : Command<SignResetPinTokenResponse>() {

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

    override fun run(session: CardSession, callback: CompletionCallback<SignResetPinTokenResponse>) {
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
        tlvBuilder.append(TlvTag.AuthorizeMode, AuthorizeMode.Token_Sign.rawValue)
        tlvBuilder.append(TlvTag.Challenge, resetPinSession.cardToReset!!.token)
        tlvBuilder.append(TlvTag.Backup_MasterKey, resetPinSession.cardToReset!!.backupKey)
        tlvBuilder.append(TlvTag.Backup_AttestSignature, resetPinSession.cardToReset!!.attestSignature)

        return CommandApdu(Instruction.Authorize, tlvBuilder.serialize())
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): SignResetPinTokenResponse {
        val tlvData = apdu.getTlvData(environment.encryptionKey) ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)
        val cardToConfirm= ResetPin_CardToConfirm(salt= decoder.decode(TlvTag.Salt),
            backupKey =decoder.decode(TlvTag.Backup_SlaveKey),
            authorizeSignature = decoder.decode(TlvTag.Backup_AttestSignature))
        return SignResetPinTokenResponse(decoder.decode(TlvTag.CardId), cardToConfirm)
    }
}


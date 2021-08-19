package com.tangem.operations.backup

import com.squareup.moshi.JsonClass
import com.tangem.common.CompletionResult
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.backup.BackupMaster
import com.tangem.common.card.Card
import com.tangem.common.core.CardSession
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.SessionEnvironment
import com.tangem.common.core.TangemSdkError
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag
import com.tangem.operations.Command
import com.tangem.operations.CommandResponse

/**
 * Response from the Tangem card after `CreateWalletCommand`.
 */
@JsonClass(generateAdapter = true)
class GetMasterKeyResponse(
    /**
     * Unique Tangem card ID number
     */
    val cardId: String,
    /**
     * Session for backup
     */
    val master: BackupMaster
) : CommandResponse

/**
 */
class GetMasterKeyCommand(
) : Command<GetMasterKeyResponse>() {

    override fun requiresPasscode(): Boolean = false

    override fun performPreCheck(card: Card): TangemSdkError? {
        if (card.wallets.isEmpty()) {
            return TangemSdkError.BackupCannotBeCreated()
        }
        if (!card.settings.isBackupEnabled) {
            return TangemSdkError.BackupCannotBeCreated()
        }
        return null
    }

    override fun run(session: CardSession, callback: CompletionCallback<GetMasterKeyResponse>) {
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
        tlvBuilder.append(TlvTag.Pin, environment.accessCode.value)
        tlvBuilder.append(TlvTag.CardId, environment.card?.cardId)

        return CommandApdu(Instruction.Backup_GetMasterKey, tlvBuilder.serialize())
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): GetMasterKeyResponse {
        val tlvData = apdu.getTlvData(environment.encryptionKey) ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)
        val master = BackupMaster(
            backupKey = decoder.decode(TlvTag.Backup_MasterKey),
            cardKey = environment.card!!.cardPublicKey
        )
        return GetMasterKeyResponse(decoder.decode(TlvTag.CardId), master)
    }
}


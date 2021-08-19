package com.tangem.operations.backup

import com.squareup.moshi.JsonClass
import com.tangem.common.CompletionResult
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.backup.BackupSession
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
 */
@JsonClass(generateAdapter = true)
class WriteBackupDataResponse(
    /**
     * Unique Tangem card ID number
     */
    val cardId: String,
    val state: Card.BackupStatus
) : CommandResponse

/**
 */
class WriteBackupDataCommand(private val backupSession: BackupSession) : Command<WriteBackupDataResponse>() {

    override fun requiresPasscode(): Boolean = true

    override fun performPreCheck(card: Card): TangemSdkError? {
        if (card.backupStatus==Card.BackupStatus.NoBackup) {
            return TangemSdkError.BackupCannotBeCreated()
        }
        if (!backupSession.slaves.containsKey(card.cardId)) return TangemSdkError.BackupSlaveCardRequired()
        if (backupSession.slaves[card.cardId]!!.encryptedData==null) return TangemSdkError.BackupInvalidCommandSequence()

        return null
    }

    override fun run(session: CardSession, callback: CompletionCallback<WriteBackupDataResponse>) {
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
        tlvBuilder.append(TlvTag.Pin, environment.accessCode.value)
        tlvBuilder.append(TlvTag.Salt,backupSession.slaves[environment.card?.cardId]?.encryptionSalt)
        tlvBuilder.append(TlvTag.IssuerData, backupSession.slaves[environment.card?.cardId]?.encryptedData)

        return CommandApdu(Instruction.Backup_WriteData, tlvBuilder.serialize())
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): WriteBackupDataResponse {
        val tlvData = apdu.getTlvData(environment.encryptionKey) ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)

        return WriteBackupDataResponse(decoder.decode(TlvTag.CardId), decoder.decode(TlvTag.Backup_State))
    }
}


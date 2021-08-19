package com.tangem.operations.backup

import com.squareup.moshi.JsonClass
import com.tangem.common.CompletionResult
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.backup.BackupSession
import com.tangem.common.backup.BackupSlave
import com.tangem.common.card.Card
import com.tangem.common.core.CardSession
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.SessionEnvironment
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.toByteArray
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag
import com.tangem.crypto.CryptoUtils
import com.tangem.operations.Command
import com.tangem.operations.CommandResponse

/**
 * Response from the Tangem card after `CreateWalletCommand`.
 */
@JsonClass(generateAdapter = true)
class ReadBackupDataResponse(
    /**
     * Unique Tangem card ID number
     */
    val cardId: String,
    val encryptedData: ByteArray,
    val encryptionSalt: ByteArray
) : CommandResponse

/**
 */
class ReadBackupDataCommand(private val backupSession: BackupSession, private val slaveBackupKey: ByteArray) : Command<ReadBackupDataResponse>() {

    override fun requiresPasscode(): Boolean = false

    override fun performPreCheck(card: Card): TangemSdkError? {
        if (card.backupStatus==Card.BackupStatus.NoBackup ) {
            return TangemSdkError.BackupCannotBeCreated()
        }
        if (!backupSession.master.cardKey.contentEquals(card.cardPublicKey)) return TangemSdkError.BackupMasterCardRequired()

        return null
    }

    override fun run(session: CardSession, callback: CompletionCallback<ReadBackupDataResponse>) {
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
        tlvBuilder.append(TlvTag.Backup_SlaveKey, slaveBackupKey)

        return CommandApdu(Instruction.Backup_ReadData, tlvBuilder.serialize())
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): ReadBackupDataResponse {
        val tlvData = apdu.getTlvData(environment.encryptionKey) ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)

        return ReadBackupDataResponse(decoder.decode(TlvTag.CardId), decoder.decode(TlvTag.IssuerData), decoder.decode(TlvTag.Salt))
    }
}


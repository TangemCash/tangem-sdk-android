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
class GetSlaveKeyResponse(
    /**
     * Unique Tangem card ID number
     */
    val cardId: String,
    val slave: BackupSlave
) : CommandResponse

/**
 */
class GetSlaveKeyCommand(private val backupSession: BackupSession) : Command<GetSlaveKeyResponse>() {

    override fun requiresPasscode(): Boolean = false

    override fun performPreCheck(card: Card): TangemSdkError? {
        if (card.wallets.isNotEmpty()) {
            return TangemSdkError.BackupCannotBeCreated()
        }
        if (!card.settings.isBackupEnabled) {
            return TangemSdkError.BackupCannotBeCreated()
        }
        if (backupSession.slaves.containsKey(card.cardId)) return TangemSdkError.BackupSlaveCardAlreadyInList()

        return null
    }

    override fun run(session: CardSession, callback: CompletionCallback<GetSlaveKeyResponse>) {
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
        tlvBuilder.append(TlvTag.Backup_MasterKey, backupSession.master!!.backupKey)

        return CommandApdu(Instruction.Backup_GetSlaveKey, tlvBuilder.serialize())
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): GetSlaveKeyResponse {
        val tlvData = apdu.getTlvData(environment.encryptionKey) ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)

        val slave = BackupSlave(
            cardKey = environment.card!!.cardPublicKey,
            backupKey = decoder.decode(TlvTag.Backup_SlaveKey),
            attestSignature = decoder.decode(TlvTag.CardSignature)
        )
        val prefix = "BACKUP_SLAVE".toByteArray(Charsets.UTF_8)
        val dataAttest = prefix + backupSession.master.backupKey + slave.backupKey
        if (!CryptoUtils.verify(slave.cardKey, dataAttest, slave.attestSignature)) {
            throw TangemSdkError.BackupInvalidSignature()
        }

        return GetSlaveKeyResponse(decoder.decode(TlvTag.CardId), slave)
    }
}


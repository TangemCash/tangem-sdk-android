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
class LinkSlaveCardsResponse(
    /**
     * Unique Tangem card ID number
     */
    val cardId: String,
    val attestSignature: ByteArray
) : CommandResponse

/**
 */
class LinkSlaveCardsCommand(private val backupSession: BackupSession) : Command<LinkSlaveCardsResponse>() {

    override fun requiresPasscode(): Boolean = true

    override fun performPreCheck(card: Card): TangemSdkError? {
        if (card.wallets.isEmpty()) {
            return TangemSdkError.BackupCannotBeCreated()
        }
        if (!card.settings.isBackupEnabled) {
            return TangemSdkError.BackupCannotBeCreated()
        }
        if (!backupSession.master.cardKey.contentEquals(card.cardPublicKey)) return TangemSdkError.BackupMasterCardRequired()
        if (backupSession.slaves.isEmpty()) return TangemSdkError.BackupSlaveCardRequired()
        if (backupSession.slaves.count()>2) return TangemSdkError.BackupToMuchSlaveCards()

        return null
    }

    override fun run(session: CardSession, callback: CompletionCallback<LinkSlaveCardsResponse>) {
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
        tlvBuilder.append(TlvTag.Pin2, environment.passcode.value)
        tlvBuilder.append(TlvTag.Backup_Count, backupSession.slaves.count())
        tlvBuilder.append(TlvTag.NewPin, backupSession.newPIN)
        tlvBuilder.append(TlvTag.NewPin2, backupSession.newPIN2)
        var i=0
        for(s in backupSession.slaves)
        {
            val tlvCardLink =TlvBuilder()
            tlvCardLink.append(TlvTag.FileIndex, i)
            tlvCardLink.append(TlvTag.Backup_SlaveKey, s.value.backupKey)
            tlvCardLink.append(TlvTag.Certificate, s.value.certificate)
            tlvCardLink.append(TlvTag.CardSignature, s.value.attestSignature)
            tlvBuilder.append(TlvTag.Backup_CardLink, tlvCardLink.serialize())
            i++
        }

        return CommandApdu(Instruction.Backup_LinkSlaveCards, tlvBuilder.serialize())
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): LinkSlaveCardsResponse {
        val tlvData = apdu.getTlvData(environment.encryptionKey) ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)

        val attestSignature: ByteArray = decoder.decode(TlvTag.Backup_AttestSignature)

        val prefix = "BACKUP".toByteArray(Charsets.UTF_8)
        var dataAttest = prefix +byteArrayOf(backupSession.slaves.count().toByte())+ backupSession.master.backupKey;
        for(s in backupSession.slaves)
        {
            dataAttest+=s.value.backupKey
        }
        dataAttest+=backupSession.newPIN!!
        dataAttest+=backupSession.newPIN2!!
        if (!CryptoUtils.verify(backupSession.master.cardKey, dataAttest, attestSignature)) {
            throw TangemSdkError.BackupInvalidSignature()
        }

        return LinkSlaveCardsResponse(decoder.decode(TlvTag.CardId), attestSignature)
    }
}


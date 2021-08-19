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
class LinkMasterCardResponse(
    /**
     * Unique Tangem card ID number
     */
    val cardId: String,
    val state: Card.BackupStatus
) : CommandResponse

/**
 */
class LinkMasterCardCommand(private val backupSession: BackupSession) : Command<LinkMasterCardResponse>() {

    override fun requiresPasscode(): Boolean = true

    override fun performPreCheck(card: Card): TangemSdkError? {
        if (card.wallets.isNotEmpty()) {
            return TangemSdkError.BackupCannotBeCreated()
        }
        if (!card.settings.isBackupEnabled) {
            return TangemSdkError.BackupCannotBeCreated()
        }
        if (backupSession.attestSignature==null) return TangemSdkError.BackupMasterCardRequired()
        if (!backupSession.slaves.containsKey(card.cardId)) return TangemSdkError.BackupSlaveCardRequired()
        if (backupSession.slaves.count()>2) return TangemSdkError.BackupToMuchSlaveCards()

        return null
    }

    override fun run(session: CardSession, callback: CompletionCallback<LinkMasterCardResponse>) {
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
        tlvBuilder.append(TlvTag.Backup_MasterKey, backupSession.master.backupKey)
        tlvBuilder.append(TlvTag.Certificate, backupSession.master.certificate)
        tlvBuilder.append(TlvTag.Backup_AttestSignature, backupSession.attestSignature)
        tlvBuilder.append(TlvTag.NewPin, backupSession.newPIN)
        tlvBuilder.append(TlvTag.NewPin2, backupSession.newPIN2)
        tlvBuilder.append(TlvTag.SettingsMask, environment.card!!.settings.settingsMask)
        var i=0
        for(s in backupSession.slaves)
        {
            val tlvCardLink =TlvBuilder()
            tlvCardLink.append(TlvTag.FileIndex, i)
            tlvCardLink.append(TlvTag.Backup_SlaveKey, s.value.backupKey)
            tlvBuilder.append(TlvTag.Backup_CardLink, tlvCardLink.serialize())
            i++
        }

        return CommandApdu(Instruction.Backup_LinkMasterCard, tlvBuilder.serialize())
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): LinkMasterCardResponse {
        val tlvData = apdu.getTlvData(environment.encryptionKey) ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)

        return LinkMasterCardResponse(decoder.decode(TlvTag.CardId), decoder.decode(TlvTag.Backup_State))
    }
}


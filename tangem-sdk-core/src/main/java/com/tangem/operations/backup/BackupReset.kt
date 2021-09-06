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
import com.tangem.operations.PreflightReadMode

/**
 * Response from the Tangem card after `CreateWalletCommand`.
 */
@JsonClass(generateAdapter = true)
class BackupResetResponse(
    /**
     * Unique Tangem card ID number
     */
    val cardId: String,
    val state: Card.BackupStatus,
    val settingsMask: Card.SettingsMask,
    val isDefaultPIN: Boolean,
    val isDefaultPIN2: Boolean
) : CommandResponse

/**
 */
class BackupResetCommand() : Command<BackupResetResponse>() {

    override fun requiresPasscode(): Boolean = true
    override fun preflightReadMode(): PreflightReadMode {
        return PreflightReadMode.ReadCardOnly
    }

    override fun performPreCheck(card: Card): TangemSdkError? {
        if (card.backupStatus!=Card.BackupStatus.Active) {
            return TangemSdkError.BackupNotActive()
        }

        return null
    }

    override fun run(session: CardSession, callback: CompletionCallback<BackupResetResponse>) {
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
        return CommandApdu(Instruction.Backup_Reset, tlvBuilder.serialize())
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): BackupResetResponse {
        val tlvData = apdu.getTlvData(environment.encryptionKey) ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)

        return BackupResetResponse(decoder.decode(TlvTag.CardId),
            Card.BackupStatus.byCode(decoder.decode(TlvTag.Backup_State))!!, decoder.decode(TlvTag.SettingsMask),
            decoder.decode(TlvTag.PinIsDefault), decoder.decode(TlvTag.Pin2IsDefault))
    }
}


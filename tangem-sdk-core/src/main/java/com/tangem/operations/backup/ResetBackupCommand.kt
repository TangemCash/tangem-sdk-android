package com.tangem.operations.backup

import com.squareup.moshi.JsonClass
import com.tangem.common.CompletionResult
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.card.Card
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.CardSession
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.SessionEnvironment
import com.tangem.common.core.TangemSdkError
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag
import com.tangem.operations.Command
import com.tangem.operations.CommandResponse
import com.tangem.operations.PreflightReadMode

/**
 * Response from the Tangem card after `ResetBackupCommand`.
 */
@JsonClass(generateAdapter = true)
data class ResetBackupResponse(
    val cardId: String,
    internal val backupStatus: Card.BackupRawStatus,
    internal val settingsMask: Card.SettingsMask,
    internal val isDefaultAccessCode: Boolean,
    internal val isDefaultPasscode: Boolean,
) : CommandResponse

class ResetBackupCommand : Command<ResetBackupResponse>() {

    override fun requiresPasscode(): Boolean = true
    override fun preflightReadMode(): PreflightReadMode = PreflightReadMode.FullCardRead

    override fun performPreCheck(card: Card): TangemSdkError? {
        if (card.firmwareVersion < FirmwareVersion.BackupAvailable) {
            return TangemSdkError.BackupFailedFirmware()
        }
        if (card.backupStatus?.canResetBackup != true) {
            return TangemSdkError.NoActiveBackup()
        }
        if (card.wallets.any { it.hasBackup }) {
            return TangemSdkError.ResetBackupFailedHasBackedUpWallets()
        }

        return null
    }

    override fun run(session: CardSession, callback: CompletionCallback<ResetBackupResponse>) {
        transceive(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    if (result.data.backupStatus != Card.BackupRawStatus.NoBackup) {
                        callback(CompletionResult.Failure(TangemSdkError.UnknownError()))
                        return@transceive
                    }
                    val card = session.environment.card
                    session.environment.card = card?.copy(
                        backupStatus = Card.BackupStatus.NoBackup,
                        isAccessCodeSet = !result.data.isDefaultAccessCode,
                        isPasscodeSet = !result.data.isDefaultPasscode,
                        settings = card.settings.updated(result.data.settingsMask),
                    )
                    callback(CompletionResult.Success(result.data))
                }
                is CompletionResult.Failure -> callback(result)
            }
        }
    }

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.CardId, environment.card?.cardId)
        tlvBuilder.append(TlvTag.Pin, environment.accessCode.value)
        tlvBuilder.append(TlvTag.Pin2, environment.passcode.value)

        return CommandApdu(Instruction.BackupReset, tlvBuilder.serialize())
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): ResetBackupResponse {
        val tlvData = apdu.getTlvData()
            ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)

        return ResetBackupResponse(
            cardId = decoder.decode(TlvTag.CardId),
            backupStatus = decoder.decode(TlvTag.BackupStatus),
            isDefaultAccessCode = decoder.decode(TlvTag.PinIsDefault),
            isDefaultPasscode = decoder.decode(TlvTag.Pin2IsDefault),
            settingsMask = decoder.decode(TlvTag.SettingsMask),
        )
    }
}

private val Card.BackupStatus.canResetBackup: Boolean
    get() = this is Card.BackupStatus.Active || this is Card.BackupStatus.CardLinked

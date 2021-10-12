package com.tangem.operations.files

import com.tangem.common.SuccessResponse
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.card.Card
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.SessionEnvironment
import com.tangem.common.core.TangemSdkError
import com.tangem.common.files.FileDataMode
import com.tangem.common.files.FileSettings
import com.tangem.common.files.FileSettingsChange
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag
import com.tangem.operations.Command

/**
 * This command allows to change settings of a file written to the card with [WriteFileCommand].
 * Passcode (PIN2) is required for this operation.
 * [FileSettings] change access level to a file - it can be [FileSettings.Private],
 * accessible only with PIN2, or [FileSettings.Public], accessible without PIN2
 *
 * @property data contains index of a file that is to be changed and desired settings.
 */
class ChangeFileSettingsCommand(
    private val data: FileSettingsChange
) : Command<SuccessResponse>() {

    override fun requiresPasscode(): Boolean = true

    override fun performPreCheck(card: Card): TangemSdkError? {
        if (card.firmwareVersion < FirmwareVersion.FilesAvailable) {
            return TangemSdkError.NotSupportedFirmwareVersion()
        }
        return null
    }

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.Pin, environment.accessCode.value)
        tlvBuilder.append(TlvTag.Pin2, environment.passcode.value)
        tlvBuilder.append(TlvTag.CardId, environment.card?.cardId)
        tlvBuilder.append(TlvTag.WriteFileMode, FileDataMode.ChangeFileSettings)
        tlvBuilder.append(TlvTag.FileIndex, data.fileIndex)
        tlvBuilder.append(TlvTag.FileSettings, data.settings)

        return CommandApdu(Instruction.WriteFileData, tlvBuilder.serialize())
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): SuccessResponse {
        val tlvData = apdu.getTlvData(environment.encryptionKey) ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)
        return SuccessResponse(decoder.decode(TlvTag.CardId))
    }
}


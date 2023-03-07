package com.tangem.operations.files

import com.tangem.common.SuccessResponse
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.card.Card
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.SessionEnvironment
import com.tangem.common.core.TangemSdkError
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag
import com.tangem.operations.Command

/**
 * This command allows to delete data written to the card with [WriteFileCommand].
 * Passcode (PIN2) is required to delete the files.
 *
 * @property fileIndex index of a file to be deleted.
 */
class DeleteFileCommand(
    private val fileIndex: Int,
) : Command<SuccessResponse>() {

    override fun requiresPasscode(): Boolean = true

    override fun performPreCheck(card: Card): TangemSdkError? {
        if (card.firmwareVersion < FirmwareVersion.FilesAvailable) {
            return TangemSdkError.NotSupportedFirmwareVersion()
        }
        if (!card.settings.isFilesAllowed) {
            return TangemSdkError.FilesDisabled()
        }

        return null
    }

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.CardId, environment.card?.cardId)
        tlvBuilder.append(TlvTag.Pin, environment.accessCode.value)
        tlvBuilder.append(TlvTag.Pin2, environment.passcode.value)
        tlvBuilder.append(TlvTag.InteractionMode, FileDataMode.DeleteFile)
        tlvBuilder.append(TlvTag.FileIndex, fileIndex)

        return CommandApdu(Instruction.WriteFileData, tlvBuilder.serialize())
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): SuccessResponse {
        val tlvData = apdu.getTlvData() ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)
        return SuccessResponse(decoder.decode(TlvTag.CardId))
    }
}
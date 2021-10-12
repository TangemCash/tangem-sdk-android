package com.tangem.operations.files

import com.squareup.moshi.JsonClass
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.card.Card
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.SessionEnvironment
import com.tangem.common.core.TangemSdkError
import com.tangem.common.files.FileDataMode
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag
import com.tangem.operations.Command
import com.tangem.operations.CommandResponse

@JsonClass(generateAdapter = true)
class ReadFileChecksumResponse(
    val cardId: String,
    val checksum: ByteArray,
    val fileIndex: Int?
) : CommandResponse

/**
 * This command allows to read a SHA256 hash of a data written to the card
 * with [WriteFileCommand] using PIN2.
 * This may be used to check integrity of a file.
 *TestHealthTask
 * @property fileIndex index of a file
 * @property readPrivateFiles if set to true, then the command will get hashes of private files.
 */
class ReadFileChecksumCommand(
    private val fileIndex: Int = 0,
    private val readPrivateFiles: Boolean = false
) : Command<ReadFileChecksumResponse>() {

    override fun requiresPasscode(): Boolean = readPrivateFiles

    override fun performPreCheck(card: Card): TangemSdkError? {
        if (card.firmwareVersion < FirmwareVersion.FilesAvailable) {
            return TangemSdkError.NotSupportedFirmwareVersion()
        }
        return null
    }

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.Pin, environment.accessCode.value)
        if (readPrivateFiles) tlvBuilder.append(TlvTag.Pin2, environment.passcode.value)
        tlvBuilder.append(TlvTag.CardId, environment.card?.cardId)
        tlvBuilder.append(TlvTag.FileIndex, fileIndex)
        tlvBuilder.append(TlvTag.WriteFileMode, FileDataMode.ReadFileHash)

        return CommandApdu(Instruction.ReadFileData, tlvBuilder.serialize())
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): ReadFileChecksumResponse {
        val tlvData = apdu.getTlvData(environment.encryptionKey) ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)
        return ReadFileChecksumResponse(
                decoder.decode(TlvTag.CardId),
                decoder.decode(TlvTag.CodeHash),
                decoder.decodeOptional(TlvTag.FileIndex),
        )
    }
}
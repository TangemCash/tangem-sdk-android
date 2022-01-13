package com.tangem.operations.files

import com.squareup.moshi.JsonClass
import com.tangem.*
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
import com.tangem.common.extensions.guard
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag
import com.tangem.operations.Command
import com.tangem.operations.CommandResponse
import java.io.ByteArrayOutputStream

@JsonClass(generateAdapter = true)
class ReadFileResponse(
    val cardId: String,
    val size: Int?,
    val fileData: ByteArray,
    val fileIndex: Int,
    val fileSettings: FileSettings,
    val fileDataSignature: ByteArray?,
    val fileDataCounter: Int?,
    val walletIndex: Int?,
) : CommandResponse

/**
 * This command allows to read data written to the card with [WriteFileCommand].
 * If the files are private, then Passcode (PIN2) is required to read the files.
 *
 * @property fileIndex index of a file
 */
internal class ReadFileCommand(
    private val fileIndex: Int,
    private val fileName: String? = null,
    private val walletPublicKey: ByteArray? = null,
) : Command<ReadFileResponse>() {

    /**
     * if true, user code or security delay will be requested
     */
    var shouldReadPrivateFiles: Boolean = false

    private var walletIndex: Int? = null

    private val fileData = ByteArrayOutputStream()
    private var offset: Int = 0
    private var dataSize: Int = 0
    private var fileSettings: FileSettings? = null

    override fun requiresPasscode(): Boolean = shouldReadPrivateFiles

    override fun performPreCheck(card: Card): TangemSdkError? {
        if (card.firmwareVersion < FirmwareVersion.FilesAvailable) {
            return TangemSdkError.NotSupportedFirmwareVersion()
        }

        return null
    }

    override fun run(session: CardSession, callback: CompletionCallback<ReadFileResponse>) {
        Log.command(this)
        val card = session.environment.card.guard {
            callback(CompletionResult.Failure(TangemSdkError.MissingPreflightRead()))
            return
        }
        if (walletPublicKey != null) {
            walletIndex = card.wallet(walletPublicKey)?.index.guard {
                callback(CompletionResult.Failure(TangemSdkError.WalletNotFound()))
                return
            }
        }

        readFileData(session, callback)
    }

    private fun readFileData(session: CardSession, callback: CompletionCallback<ReadFileResponse>) {
        if (dataSize != 0) {
            session.viewDelegate.onDelay(dataSize, offset, WriteFileCommand.SINGLE_WRITE_SIZE)
        }
        transceive(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    if (result.data.size != null) {
                        if (result.data.size == 0) {
                            callback(CompletionResult.Success(result.data))
                            return@transceive
                        }
                        dataSize = result.data.size
                        fileSettings = result.data.fileSettings
                    }

                    fileData.write(result.data.fileData)
                    if (fileData.size() < dataSize) {
                        offset = fileData.size()
                        readFileData(session, callback)
                    } else {
                        completeTask(result.data, callback)
                    }
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }

    private fun completeTask(data: ReadFileResponse, callback: CompletionCallback<ReadFileResponse>) {
        val finalResult = ReadFileResponse(
            cardId = data.cardId,
            size = dataSize,
            fileData = fileData.toByteArray(),
            fileIndex = data.fileIndex,
            fileSettings = fileSettings ?: data.fileSettings,
            fileDataSignature = data.fileDataSignature,
            fileDataCounter = data.fileDataCounter,
            walletIndex = this.walletIndex,
        )
        callback(CompletionResult.Success(finalResult))
    }

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val card = environment.card ?: throw TangemSdkError.MissingPreflightRead()

        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.CardId, card.cardId)
        tlvBuilder.append(TlvTag.FileIndex, fileIndex)
        tlvBuilder.append(TlvTag.FileTypeName, fileName)
        tlvBuilder.append(TlvTag.WalletIndex, walletIndex)
        tlvBuilder.append(TlvTag.Offset, offset)

        if (shouldReadPrivateFiles) {
            tlvBuilder.append(TlvTag.Pin, environment.accessCode.value)
            tlvBuilder.append(TlvTag.Pin2, environment.passcode.value)

        } else if (card.firmwareVersion.doubleValue < 4) {
            tlvBuilder.append(TlvTag.Pin, environment.accessCode.value)
        }

        return CommandApdu(Instruction.ReadFileData, tlvBuilder.serialize())
    }

    override fun deserialize(
        environment: SessionEnvironment,
        apdu: ResponseApdu
    ): ReadFileResponse {
        val tlvData = apdu.getTlvData(environment.encryptionKey) ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)
        return ReadFileResponse(
            cardId = decoder.decode(TlvTag.CardId),
            size = decoder.decodeOptional(TlvTag.Size),
            fileData = decoder.decodeOptional(TlvTag.IssuerData) ?: byteArrayOf(),
            fileIndex = decoder.decodeOptional(TlvTag.FileIndex) ?: 0,
            fileSettings = FileSettings(decoder.decode(TlvTag.FileSettings)),
            fileDataSignature = decoder.decodeOptional(TlvTag.IssuerDataSignature),
            fileDataCounter = decoder.decodeOptional(TlvTag.IssuerDataCounter),
            walletIndex = decoder.decodeOptional(TlvTag.WalletIndex)
        )
    }
}
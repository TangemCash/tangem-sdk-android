package com.tangem.operations.files

import com.squareup.moshi.JsonClass
import com.tangem.Log
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

@JsonClass(generateAdapter = true)
class ReadFileResponse(
    var cardId: String,
    var size: Int?,
    var offset: Int?,
    var fileData: ByteArray,
    var fileIndex: Int,
    var settings: FileSettings?,
    var ownerIndex: Int?,
    var ownerPublicKey: ByteArray?,
    var walletIndex: Int?,
) : CommandResponse {

    internal val isReadComplete: Boolean
        get() {
            val size = size ?: return true
            return fileData.size == size
        }

    internal fun update(response: ReadFileResponse) {
        cardId = response.cardId
        fileIndex = response.fileIndex

        response.size?.let { size = it }
        response.settings?.let { settings = it }
        response.ownerIndex?.let { ownerIndex = it }
        response.ownerPublicKey?.let { ownerPublicKey = it }
        response.walletIndex?.let { walletIndex = it }
        response.offset?.let { offset = it }

        fileData += response.fileData
    }

    companion object {
        fun empty() = ReadFileResponse(
            "",
            null,
            null,
            byteArrayOf(),
            0,
            null,
            null,
            null,
            null
        )

    }
}

/**
 * Command that read single file at specified index. Reading private file will prompt user to input a passcode.
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

    private var aggregatedResponse: ReadFileResponse = ReadFileResponse.empty()

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

        readFileData(session) { result ->
            when (result) {
                is CompletionResult.Success -> callback(CompletionResult.Success(aggregatedResponse))
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }

    private fun readFileData(session: CardSession, callback: CompletionCallback<Unit>) {
//        if (aggregatedResponse.fileData.isNotEmpty()) {
//            session.viewDelegate.onDelay(
//                aggregatedResponse.size ?: 0,
//                aggregatedResponse.fileData.size,
//                WriteFileCommand.SINGLE_WRITE_SIZE)
//        }
        transceive(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    aggregatedResponse.update(result.data)
                    if (aggregatedResponse.isReadComplete) {
                        callback(CompletionResult.Success(Unit))
                    } else {
                        readFileData(session, callback)
                    }
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val card = environment.card ?: throw TangemSdkError.MissingPreflightRead()

        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.CardId, card.cardId)
        tlvBuilder.append(TlvTag.FileIndex, fileIndex)
        tlvBuilder.append(TlvTag.FileTypeName, fileName)
        tlvBuilder.append(TlvTag.WalletIndex, walletIndex)
        tlvBuilder.append(TlvTag.Offset, aggregatedResponse.fileData.size)

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
        val settings: FileSettings? = decoder.decodeOptional<ByteArray>(TlvTag.FileSettings)?.let { FileSettings(it) }

        return ReadFileResponse(
            cardId = decoder.decode(TlvTag.CardId),
            size = decoder.decodeOptional(TlvTag.Size),
            offset = decoder.decodeOptional(TlvTag.Offset),
            fileData = decoder.decodeOptional(TlvTag.IssuerData) ?: byteArrayOf(),
            fileIndex = decoder.decodeOptional(TlvTag.FileIndex) ?: 0,
            settings = settings,
            ownerIndex = decoder.decodeOptional(TlvTag.FileOwnerIndex),
            ownerPublicKey = decoder.decodeOptional(TlvTag.IssuerPublicKey),
            walletIndex = decoder.decodeOptional(TlvTag.WalletIndex)
        )
    }
}
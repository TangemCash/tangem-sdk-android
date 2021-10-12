package com.tangem.operations.files

import com.squareup.moshi.JsonClass
import com.tangem.Log
import com.tangem.common.CompletionResult
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.card.Card
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.*
import com.tangem.common.files.DataToWrite
import com.tangem.common.files.FileDataMode
import com.tangem.common.files.FileDataProtectedByPasscode
import com.tangem.common.files.FileDataProtectedBySignature
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag
import com.tangem.crypto.DefaultIssuerDataVerifier
import com.tangem.crypto.IssuerDataToVerify
import com.tangem.crypto.IssuerDataVerifier
import com.tangem.operations.Command
import com.tangem.operations.CommandResponse
import com.tangem.operations.files.WriteFileCommand.Companion.MAX_SIZE

/**
 * Deserialized response for [WriteFileCommand]
 */
@JsonClass(generateAdapter = true)
class WriteFileResponse(
    val cardId: String,
    val fileIndex: Int? = null
) : CommandResponse

/**
 * This command allows to write data up to [MAX_SIZE] to a card.
 * There are two secure ways to write files.
 * 1) Data can be signed by Issuer (the one specified on card during personalization) -
 * [FileDataProtectedBySignature].
 * 2) Data can be protected by Passcode (PIN2). [FileDataProtectedByPasscode] In this case,
 * Passcode (PIN2) is required for the command.
 */
class WriteFileCommand(
    private val dataToWrite: DataToWrite,
    verifier: IssuerDataVerifier = DefaultIssuerDataVerifier()
) : Command<WriteFileResponse>(), IssuerDataVerifier by verifier {

    private var mode: FileDataMode = FileDataMode.InitiateWritingFile
    private var offset: Int = 0
    private var fileIndex: Int = 0

    override fun requiresPasscode(): Boolean = dataToWrite.requiredPasscode

    override fun run(session: CardSession, callback: CompletionCallback<WriteFileResponse>) {
        Log.command(this)
        writeFileData(session, callback)
    }

    override fun performPreCheck(card: Card): TangemSdkError? {
        if (card.firmwareVersion < FirmwareVersion.FilesAvailable
                || card.firmwareVersion < dataToWrite.minFirmwareVersion) {
            return TangemSdkError.NotSupportedFirmwareVersion()
        }
        if (dataToWrite.data.size > MAX_SIZE) {
            return TangemSdkError.DataSizeTooLarge()
        }

        if (dataToWrite is FileDataProtectedBySignature) {
            if (!isCounterValid(dataToWrite.counter, card)) {
                return TangemSdkError.MissingCounter()
            }
            if (!verifySignatures(card.issuer.publicKey, card.cardId)) {
                return TangemSdkError.VerificationFailed()
            }
        }
        return null
    }

    override fun mapError(card: Card?, error: TangemError): TangemError {
        val card = card ?: return error

        if (error is TangemSdkError.InvalidParams && isCounterRequired(card)) {
            return TangemSdkError.DataCannotBeWritten()
        }
        if (error is TangemSdkError.InvalidState && card.settings.isIssuerDataProtectedAgainstReplay) {
            return TangemSdkError.OverwritingDataIsProhibited()
        }
        return error
    }

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.Pin, environment.accessCode.value)
        tlvBuilder.append(TlvTag.CardId, environment.card?.cardId)
        tlvBuilder.append(TlvTag.WriteFileMode, mode)

        when (mode) {
            FileDataMode.InitiateWritingFile -> {
                dataToWrite.addStartingTlvData(tlvBuilder, environment)
                tlvBuilder.append(TlvTag.Size, dataToWrite.data.size)
            }
            FileDataMode.WriteFile -> {
                tlvBuilder.append(TlvTag.IssuerData, getDataToWrite())
                tlvBuilder.append(TlvTag.Offset, offset)
                tlvBuilder.append(TlvTag.FileIndex, fileIndex)
            }
            FileDataMode.ConfirmWritingFile -> {
                dataToWrite.addFinalizingTlvData(tlvBuilder, environment)
                tlvBuilder.append(TlvTag.FileIndex, fileIndex)
            }
        }
        return CommandApdu(Instruction.WriteFileData, tlvBuilder.serialize())
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): WriteFileResponse {
        val tlvData = apdu.getTlvData(environment.encryptionKey) ?: throw TangemSdkError.DeserializeApduFailed()
        val decoder = TlvDecoder(tlvData)
        return WriteFileResponse(decoder.decode(TlvTag.CardId), decoder.decodeOptional(TlvTag.FileIndex)
        )
    }

    private fun writeFileData(session: CardSession, callback: CompletionCallback<WriteFileResponse>) {
        if (mode == FileDataMode.WriteFile) {
            session.viewDelegate.onDelay(dataToWrite.data.size, offset, SINGLE_WRITE_SIZE)
        }
        transceive(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    when (mode) {
                        FileDataMode.InitiateWritingFile -> {
                            fileIndex = result.data.fileIndex ?: 0
                            mode = FileDataMode.WriteFile
                            writeFileData(session, callback)
                        }
                        FileDataMode.WriteFile -> {
                            offset += SINGLE_WRITE_SIZE
                            if (offset >= dataToWrite.data.size) {
                                mode = FileDataMode.ConfirmWritingFile
                            }
                            writeFileData(session, callback)
                        }
                        FileDataMode.ConfirmWritingFile -> {
                            callback(CompletionResult.Success(WriteFileResponse(result.data.cardId, fileIndex)))
                        }
                    }
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }

    private fun getDataToWrite(): ByteArray = dataToWrite.data.copyOfRange(offset, offset + calculatePartSize())

    private fun calculatePartSize(): Int {
        val bytesLeft = dataToWrite.data.size - offset
        return if (bytesLeft < SINGLE_WRITE_SIZE) bytesLeft else SINGLE_WRITE_SIZE
    }

    private fun isCounterValid(issuerDataCounter: Int?, card: Card): Boolean {
        return if (isCounterRequired(card)) issuerDataCounter != null else true
    }

    private fun isCounterRequired(card: Card): Boolean {
        return if (dataToWrite.requiredPasscode) {
            false
        } else {
            card.settings.isIssuerDataProtectedAgainstReplay
        }
    }

    private fun verifySignatures(publicKey: ByteArray, cardId: String): Boolean {
        if (dataToWrite !is FileDataProtectedBySignature) return true

        val firstData = IssuerDataToVerify(cardId, null, dataToWrite.counter, dataToWrite.data.size)
        val secondData = IssuerDataToVerify(cardId, dataToWrite.data, dataToWrite.counter)

        val startingSignatureVerified = verify(publicKey, dataToWrite.startingSignature, firstData)
        val finalizingSignatureVerified = verify(publicKey, dataToWrite.finalizingSignature, secondData)

        return startingSignatureVerified && finalizingSignatureVerified
    }

    companion object {
        const val SINGLE_WRITE_SIZE = 900
        const val MAX_SIZE = 48 * 1024
    }
}
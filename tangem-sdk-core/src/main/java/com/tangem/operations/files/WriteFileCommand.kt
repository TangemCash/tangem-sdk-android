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
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.guard
import com.tangem.common.extensions.ifNotNullOr
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag
import com.tangem.crypto.DefaultIssuerDataVerifier
import com.tangem.crypto.IssuerDataToVerify
import com.tangem.crypto.IssuerDataVerifier
import com.tangem.operations.Command
import com.tangem.operations.CommandResponse

/**
 * Deserialized response for [WriteFileCommand]
 */
@JsonClass(generateAdapter = true)
class WriteFileResponse(
    val cardId: String,
    val fileIndex: Int? = null,
) : CommandResponse

/**
 * Command for writing file on card
 */
class WriteFileCommand private constructor(
    verifier: IssuerDataVerifier = DefaultIssuerDataVerifier(),
) : Command<WriteFileResponse>(), IssuerDataVerifier by verifier {

    private lateinit var data: ByteArray
    private var startingSignature: ByteArray? = null
    private var finalizingSignature: ByteArray? = null
    private var counter: Int? = null
    private var fileVisibility: FileVisibility? = null
    private var walletPublicKey: ByteArray? = null

    private var isWritingByUserCodes = false
    private var walletIndex: Int? = null
    private var mode: FileDataMode = FileDataMode.InitiateWritingFile
    private var offset: Int = 0
    private var fileIndex: Int = 0

    /**
     * Constructor for writing the file by the file owner
     * @property data: Data to write
     * @property startingSignature: Starting signature of the file data. You can use `FileHashHelper` to generate
     * signatures or use it as a reference to create the signature yourself
     * @property finalizingSignature: Finalizing signature of the file data. You can use `FileHashHelper` to generate
     * signatures or use it as a reference to create the signature yourself
     * @property counter: File counter to prevent replay attack
     * @property fileVisibility: Optional visibility setting for the file. COS 4.0+
     * @property walletPublicKey: Optional link to the card's wallet. COS 4.0+
     */
    constructor(
        data: ByteArray,
        startingSignature: ByteArray,
        finalizingSignature: ByteArray,
        counter: Int,
        fileVisibility: FileVisibility? = null,
        walletPublicKey: ByteArray? = null,
    ) : this() {
        this.data = data
        this.startingSignature = startingSignature
        this.finalizingSignature = finalizingSignature
        this.counter = counter
        this.fileVisibility = fileVisibility
        this.walletPublicKey = walletPublicKey
        isWritingByUserCodes = false
    }

    /**
     * Constructor for writing the file by the user
     * @property data: Data to write
     * @property fileVisibility: Optional visibility setting for the file. COS 4.0+
     * @property walletPublicKey: Optional link to the card's wallet. COS 4.0+
     */
    constructor(
        data: ByteArray,
        fileVisibility: FileVisibility? = null,
        walletPublicKey: ByteArray? = null,
    ) : this() {
        isWritingByUserCodes = true

        this.data = data
        this.walletPublicKey = walletPublicKey
        this.fileVisibility = fileVisibility
        this.startingSignature = null
        this.finalizingSignature = null
        this.counter = null
    }

    override fun requiresPasscode(): Boolean = isWritingByUserCodes

    override fun run(session: CardSession, callback: CompletionCallback<WriteFileResponse>) {
        Log.command(this)
        val card = session.environment.card.guard {
            callback(CompletionResult.Failure(TangemSdkError.MissingPreflightRead()))
            return
        }
        if (walletPublicKey != null) {
            walletIndex = card.wallet(walletPublicKey!!)?.index.guard {
                callback(CompletionResult.Failure(TangemSdkError.WalletNotFound()))
                return
            }
        }

        writeFileData(session, callback)
    }

    @Suppress("MagicNumber")
    override fun performPreCheck(card: Card): TangemSdkError? {
        val firmwareVersion = card.firmwareVersion
        if (firmwareVersion < FirmwareVersion.FilesAvailable) {
            return TangemSdkError.NotSupportedFirmwareVersion()
        }
        if (!card.settings.isFilesAllowed) {
            return TangemSdkError.FilesDisabled()
        }
        if (isWritingByUserCodes && firmwareVersion.doubleValue < 3.34) {
            return TangemSdkError.NotSupportedFirmwareVersion()
        }
        if (fileVisibility != null && firmwareVersion.doubleValue < 4) {
            return TangemSdkError.FileSettingsUnsupported()
        }
        if (walletPublicKey != null && firmwareVersion.doubleValue < 4) {
            return TangemSdkError.FileSettingsUnsupported()
        }
        if (data.size > MAX_SIZE) {
            return TangemSdkError.DataSizeTooLarge()
        }
        if (!verifySignatures(card.issuer.publicKey, card.cardId)) {
            return TangemSdkError.VerificationFailed()
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
        tlvBuilder.append(TlvTag.CardId, environment.card?.cardId)
        tlvBuilder.append(TlvTag.Pin, environment.accessCode.value)
        tlvBuilder.append(TlvTag.InteractionMode, mode)

        when (mode) {
            FileDataMode.InitiateWritingFile -> {
                tlvBuilder.append(TlvTag.Size, data.size)

                ifNotNullOr(
                    startingSignature,
                    counter,
                    { signature, counter ->
                        tlvBuilder.append(TlvTag.IssuerDataSignature, signature)
                        tlvBuilder.append(TlvTag.IssuerDataCounter, counter)
                    },
                    {
                        tlvBuilder.append(TlvTag.Pin2, environment.passcode.value)
                    },
                )
                tlvBuilder.append(TlvTag.WalletIndex, walletIndex)

                fileVisibility?.let {
                    val card = environment.card ?: throw TangemSdkError.MissingPreflightRead()
                    tlvBuilder.append(TlvTag.FileSettings, it.serializeValue(card.firmwareVersion))
                }
            }
            FileDataMode.WriteFile -> {
                val partSize = kotlin.math.min(SINGLE_WRITE_SIZE, data.size - offset)
                val dataChunk = data.copyOfRange(offset, offset + partSize)

                tlvBuilder.append(TlvTag.IssuerData, dataChunk)
                tlvBuilder.append(TlvTag.Offset, offset)
                tlvBuilder.append(TlvTag.FileIndex, fileIndex)
            }
            FileDataMode.ConfirmWritingFile -> {
                tlvBuilder.append(TlvTag.FileIndex, fileIndex)
                if (finalizingSignature != null) {
                    tlvBuilder.append(TlvTag.IssuerDataSignature, finalizingSignature)
                } else {
                    tlvBuilder.append(TlvTag.CodeHash, data.calculateSha256())
                    tlvBuilder.append(TlvTag.Pin2, environment.passcode.value)
                }
            }
        }

        return CommandApdu(Instruction.WriteFileData, tlvBuilder.serialize())
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): WriteFileResponse {
        val tlvData = apdu.getTlvData() ?: throw TangemSdkError.DeserializeApduFailed()
        val decoder = TlvDecoder(tlvData)
        return WriteFileResponse(
            cardId = decoder.decode(TlvTag.CardId),
            fileIndex = decoder.decodeOptional(TlvTag.FileIndex),
        )
    }

    private fun writeFileData(session: CardSession, callback: CompletionCallback<WriteFileResponse>) {
        if (mode == FileDataMode.WriteFile) {
            session.viewDelegate.onDelay(data.size, offset, SINGLE_WRITE_SIZE)
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
                            if (offset >= data.size) {
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

    private fun isCounterRequired(card: Card): Boolean {
        return if (isWritingByUserCodes) {
            false
        } else {
            card.settings.isIssuerDataProtectedAgainstReplay
        }
    }

    private fun verifySignatures(publicKey: ByteArray, cardId: String): Boolean {
        return if (isWritingByUserCodes) {
            true
        } else {
            ifNotNullOr(
                a = counter,
                b = startingSignature,
                c = finalizingSignature,
                block = { counter, startingSignature, finalizingSignature ->
                    val firstData = IssuerDataToVerify(cardId, null, counter, data.size)
                    val secondData = IssuerDataToVerify(cardId, data, counter)

                    val startingSignatureVerified = verify(publicKey, startingSignature, firstData)
                    val finalizingSignatureVerified = verify(publicKey, finalizingSignature, secondData)

                    startingSignatureVerified && finalizingSignatureVerified
                },
                orBloc = {
                    false
                },
            )
        }
    }

    companion object {
        const val SINGLE_WRITE_SIZE = 900
        const val MAX_SIZE = 48 * 1024

        /**
         * Convenience constructor
         * @property file: File to write
         */
        operator fun invoke(file: FileToWrite): WriteFileCommand = when (file) {
            is FileToWrite.ByFileOwner -> {
                WriteFileCommand(
                    data = file.payload,
                    startingSignature = file.startingSignature,
                    finalizingSignature = file.finalizingSignature,
                    counter = file.counter,
                    fileVisibility = file.fileVisibility,
                    walletPublicKey = file.walletPublicKey,
                )
            }
            is FileToWrite.ByUser -> {
                WriteFileCommand(
                    data = file.payload,
                    fileVisibility = file.fileVisibility,
                    walletPublicKey = file.walletPublicKey,
                )
            }
        }
    }
}

package com.tangem.operations.files

import com.tangem.common.CompletionResult
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError

/**
 * This task allows to read multiple files written to the card with [WriteFileCommand].
 * If the files are private, then Passcode (PIN2) is required to read the files.
 */
class ReadFilesTask(
    private val fileName: String? = null,
    private val walletPublicKey: ByteArray? = null
) : CardSessionRunnable<List<File>> {

    /**
     * If true, user code or security delay will be requested
     */
    var shouldReadPrivateFiles: Boolean = false

    private val files = mutableListOf<File>()

    override fun run(session: CardSession, callback: CompletionCallback<List<File>>) {
        readAllFiles(0, session, callback)
    }

    private fun readAllFiles(fileIndex: Int, session: CardSession, callback: CompletionCallback<List<File>>) {
        val command = ReadFileCommand(fileIndex, fileName, walletPublicKey)
        command.shouldReadPrivateFiles = shouldReadPrivateFiles

        command.run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    val file = result.data
                    if (file.fileData.isNotEmpty()) {
                        files.add(File(file))
                        readAllFiles(file.fileIndex + 1, session, callback)
                    } else {
                        callback(CompletionResult.Success(files))
                    }
                }
                is CompletionResult.Failure -> {
                    if (result.error is TangemSdkError.FileNotFound) {
                        callback(CompletionResult.Success(files))
                    } else {
                        callback(CompletionResult.Failure(result.error))
                    }
                }
            }
        }
    }
}


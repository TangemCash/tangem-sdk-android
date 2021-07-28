package com.tangem.operations.files

import com.squareup.moshi.JsonClass
import com.tangem.Log
import com.tangem.common.CompletionResult
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.common.files.File
import com.tangem.operations.CommandResponse

@JsonClass(generateAdapter = true)
class ReadFilesResponse(
    val files: List<File>
) : CommandResponse

/**
 * This task allows to read multiple files written to the card with [WriteFileCommand].
 * If the files are private, then Passcode (PIN2) is required to read the files.
 *
 * @property readPrivateFiles if set to true, then the task will read private files,
 * for which it requires PIN2. Otherwise only public files can be read.
 * @property indices indices of files to be read. If not provided, the task will read and return
 * all files from a card that satisfy the access level condition (either only public or private and public).
 */
class ReadFilesTask(
    private val readPrivateFiles: Boolean = false,
    private val indices: List<Int>? = listOf()
) : CardSessionRunnable<ReadFilesResponse> {

    private var index = 0
    private val files = mutableListOf<File>()

    override fun run(session: CardSession, callback: CompletionCallback<ReadFilesResponse>) {
        if (indices.isNullOrEmpty()) {
            readAllFiles(session, callback)
        } else {
            readSpecifiedFiles(indices, session, callback)
        }
    }

    private fun readAllFiles(session: CardSession, callback: CompletionCallback<ReadFilesResponse>) {
        val command = ReadFileCommand(index, readPrivateFiles)
        command.run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    if (result.data.fileData.isNotEmpty()) {
                        files.add(File(result.data))
                    }
                    index = result.data.fileIndex + 1
                    readAllFiles(session, callback)
                }
                is CompletionResult.Failure -> {
                    if (result.error is TangemSdkError.FileNotFound) {
                        Log.debug { "Receive files not found error" }
                        callback(CompletionResult.Success(ReadFilesResponse(files)))
                    } else {
                        callback(CompletionResult.Failure(result.error))
                    }
                }
            }
        }
    }

    private fun readSpecifiedFiles(
        indices: List<Int>,
        session: CardSession,
        callback: CompletionCallback<ReadFilesResponse>
    ) {
        val command = ReadFileCommand(indices[index], readPrivateFiles)
        command.run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    val data = result.data
                    val newFile = File(data.fileIndex, data.fileSettings, data.fileData)
                    files.add(newFile)

                    if (index == indices.lastIndex) {
                        callback(CompletionResult.Success(ReadFilesResponse(files)))
                        return@run
                    }

                    index += 1
                    readSpecifiedFiles(indices, session, callback)
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }
}
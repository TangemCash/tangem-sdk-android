package com.tangem.operations.files

import com.squareup.moshi.JsonClass
import com.tangem.common.CompletionResult
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.guard
import com.tangem.operations.CommandResponse

/**
 * Response for [WriteFilesTask].
 * @property cardId: CID, Unique Tangem card ID number
 * @property filesIndices: Indices of created files
 */
@JsonClass(generateAdapter = true)
class WriteFilesResponse(
    val cardId: String,
    val filesIndices: List<Int>,
) : CommandResponse

/**
 * This task allows to write multiple files to a card.
 * There are two secure ways to write files.
 * 1) Data can be signed by owner (the one specified on card during personalization).
 * 2) Data can be protected by user code. In this case, user code is required for the command.
 * @property files: Array of files to write
 */
class WriteFilesTask(
    private val files: List<FileToWrite>,
) : CardSessionRunnable<WriteFilesResponse> {

    private val savedFilesIndices = mutableListOf<Int>()

    override fun run(session: CardSession, callback: CompletionCallback<WriteFilesResponse>) {
        if (files.isEmpty()) {
            callback(CompletionResult.Failure(TangemSdkError.FilesIsEmpty()))
            return
        }

        writeFiles(0, session, callback)
    }

    private fun writeFiles(index: Int, session: CardSession, callback: CompletionCallback<WriteFilesResponse>) {
        val card = session.environment.card.guard {
            callback(CompletionResult.Failure(TangemSdkError.CardError()))
            return
        }

        if (index >= files.size) {
            callback(CompletionResult.Success(WriteFilesResponse(card.cardId, savedFilesIndices)))
            return
        }

        val fileToWrite = files[index]
        WriteFileCommand(fileToWrite).run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    result.data.fileIndex?.let { savedFilesIndices.add(it) }
                    writeFiles(index + 1, session, callback)
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }
}

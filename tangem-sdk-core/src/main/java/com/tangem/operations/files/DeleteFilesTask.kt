package com.tangem.operations.files

import com.tangem.common.CompletionResult
import com.tangem.common.SuccessResponse
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError

/**
 * This task allows to delete multiple or all files written to the card with [WriteFileCommand].
 * Passcode (PIN2) is required to delete the files.
 *
 * @property filesIndices indices of files to be deleted. If [filesIndices] are not provided,
 * then all files will be deleted.
 */
class DeleteFilesTask(
    private val filesIndices: List<Int>? = null
) : CardSessionRunnable<SuccessResponse> {

    override fun run(session: CardSession, callback: CompletionCallback<SuccessResponse>) {
        if (filesIndices == null) {
            deleteAllFiles(session, callback)
        } else {
            deleteFiles(filesIndices.sorted(), session, callback)
        }
    }

    private fun deleteAllFiles(session: CardSession, callback: CompletionCallback<SuccessResponse>) {
        DeleteFileCommand(0).run(session) { result ->
            when (result) {
                is CompletionResult.Success -> deleteAllFiles(session, callback)
                is CompletionResult.Failure ->
                    if (result.error is TangemSdkError.ErrorProcessingCommand) {
                        callback(CompletionResult.Success(SuccessResponse(session.environment.card?.cardId ?: "")))
                    } else {
                        callback(CompletionResult.Failure(result.error))
                    }
            }
        }
    }

    private fun deleteFiles(filesIndex: List<Int>, session: CardSession, callback: CompletionCallback<SuccessResponse>) {
        val index = filesIndex.last()
        DeleteFileCommand(index).run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    val remainingIndex = filesIndex.dropLast(1)
                    if (remainingIndex.isEmpty()) {
                        callback(result)
                    } else {
                        deleteFiles(remainingIndex, session, callback)
                    }
                }
                is CompletionResult.Failure -> callback(result)
            }
        }
    }
}
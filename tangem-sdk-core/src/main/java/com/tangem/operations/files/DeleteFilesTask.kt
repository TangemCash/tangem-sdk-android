package com.tangem.operations.files

import com.tangem.common.CompletionResult
import com.tangem.common.SuccessResponse
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.extensions.guard
import java.util.*

/**
 * This task allows to delete multiple or all files written to the card with [WriteFileCommand].
 * Passcode (PIN2) is required to delete the files.
 *
 * @property indices optional array of indices that should be deleted. If not specified all files
 * will be deleted from card
 */
class DeleteFilesTask(
    indices: List<Int>? = null
) : CardSessionRunnable<SuccessResponse> {

    private val indices: Deque<Int> = indices?.let { ArrayDeque(it.sorted()) } ?: ArrayDeque()

    override fun run(session: CardSession, callback: CompletionCallback<SuccessResponse>) {
        if (indices.isEmpty()) {
            DeleteAllFilesTask().run(session, callback)
        } else {
            deleteFiles(session, callback)
        }
    }

    private fun deleteFiles(session: CardSession, callback: CompletionCallback<SuccessResponse>) {
        val index = indices.pollLast().guard {
            callback(CompletionResult.Success(SuccessResponse(session.environment.card?.cardId ?: "")))
            return
        }

        DeleteFileCommand(index).run(session) { result ->
            when (result) {
                is CompletionResult.Success -> deleteFiles(session, callback)
                is CompletionResult.Failure -> callback(result)
            }
        }
    }
}
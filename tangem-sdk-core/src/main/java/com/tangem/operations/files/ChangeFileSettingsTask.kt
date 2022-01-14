package com.tangem.operations.files

import com.tangem.common.CompletionResult
import com.tangem.common.SuccessResponse
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.extensions.guard
import java.util.*

/**
 * This task allows to change settings of multiple files written to the card with [WriteFileCommand].
 * @property changes: Dictionary of file indices with new settings
 */
class ChangeFileSettingsTask(
    changes: Map<Int, FileVisibility>
) : CardSessionRunnable<SuccessResponse> {

    private val changes: Deque<Pair<Int, FileVisibility>> = ArrayDeque(changes.toList())

    override fun run(session: CardSession, callback: CompletionCallback<SuccessResponse>) {
        changeSettings(session, callback)
    }

    private fun changeSettings(session: CardSession, callback: CompletionCallback<SuccessResponse>) {
        val changes = changes.pollLast().guard {
            callback(CompletionResult.Success(SuccessResponse(session.environment.card?.cardId ?: "")))
            return
        }

        ChangeFileSettingsCommand(changes.first, changes.second).run(session) { result ->
            when (result) {
                is CompletionResult.Success -> changeSettings(session, callback)
                is CompletionResult.Failure -> callback(result)
            }
        }
    }
}
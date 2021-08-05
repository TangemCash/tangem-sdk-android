package com.tangem.operations.files

import com.tangem.common.CompletionResult
import com.tangem.common.SuccessResponse
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.files.FileSettingsChange

/**
 * This task allows to change settings of multiple files written to the card with [WriteFileCommand].
 * Passcode (PIN2) is required for this operation.
 * [FileSettings] change access level to a file - it can be [FileSettings.Private],
 * accessible only with PIN2, or [FileSettings.Public], accessible without PIN2
 *
 * @property changes contains list of [FileSettingsChange] -
 * indices of files that are to be changed and desired settings.
 */
class ChangeFilesSettingsTask(
    private val changes: List<FileSettingsChange>
) : CardSessionRunnable<SuccessResponse> {

    private var currentIndex = 0

    override fun run(session: CardSession, callback: CompletionCallback<SuccessResponse>) {
        changeSettings(session, callback)
    }

    private fun changeSettings(session: CardSession, callback: CompletionCallback<SuccessResponse>) {
        ChangeFileSettingsCommand(changes[currentIndex]).run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    if (currentIndex == changes.lastIndex) {
                        callback(result)
                    } else {
                        currentIndex += 1
                        changeSettings(session, callback)
                    }
                }
                is CompletionResult.Failure -> callback(result)
            }
        }
    }
}
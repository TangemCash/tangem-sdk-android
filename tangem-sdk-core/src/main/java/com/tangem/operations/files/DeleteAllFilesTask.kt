package com.tangem.operations.files

import com.tangem.common.CompletionResult
import com.tangem.common.SuccessResponse
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError

/**
 * Created by Anton Zhilenkov on 13/07/2021.
 */
internal class DeleteAllFilesTask : CardSessionRunnable<SuccessResponse> {

    override fun run(session: CardSession, callback: CompletionCallback<SuccessResponse>) {
        deleteFile(session, callback)
    }

    private fun deleteFile(session: CardSession, callback: CompletionCallback<SuccessResponse>) {
        DeleteFileCommand(0).run(session) { result ->
            when (result) {
                is CompletionResult.Success -> deleteFile(session, callback)
                is CompletionResult.Failure -> {
                    if (result.error is TangemSdkError.ErrorProcessingCommand) {
                        val response = SuccessResponse(session.environment.card?.cardId ?: "")
                        callback(CompletionResult.Success(response))
                    } else {
                        callback(result)
                    }
                }
            }
        }
    }
}
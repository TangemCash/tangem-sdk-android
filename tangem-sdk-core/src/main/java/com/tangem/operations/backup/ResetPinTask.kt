package com.tangem.operations.backup

import com.squareup.moshi.JsonClass
import com.tangem.common.CompletionResult
import com.tangem.common.SuccessResponse
import com.tangem.common.backup.BackupSession
import com.tangem.common.backup.BackupSlave
import com.tangem.common.backup.ResetPinSession
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.operations.CommandResponse
import com.tangem.operations.PreflightReadMode

class ResetPinTask(
    private val resetPinSession: ResetPinSession
) : CardSessionRunnable<SuccessResponse> {

    override fun preflightReadMode(): PreflightReadMode {
        return PreflightReadMode.ReadCardOnly
    }
    override fun run(session: CardSession, callback: CompletionCallback<SuccessResponse>) {
            AuthorizeResetPinTokenCommand(resetPinSession).run(session) {
            result ->
            when(result)
            {
                is CompletionResult.Success -> {
                    ResetPinCommand(resetPinSession).run(session)
                    {
                        result2 ->
                        when(result2)
                        {
                            is CompletionResult.Success -> {
                                callback(result2)
                            }
                            is CompletionResult.Failure -> {
                                callback(CompletionResult.Failure(result2.error))
                            }

                        }
                    }
                }
                is CompletionResult.Failure -> {
                    callback(CompletionResult.Failure(result.error))
                }

            }
        }
    }
}
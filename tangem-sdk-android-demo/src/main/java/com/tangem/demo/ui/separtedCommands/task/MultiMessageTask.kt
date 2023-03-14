package com.tangem.demo.ui.separtedCommands.task

import com.tangem.Message
import com.tangem.common.CompletionResult
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.operations.PreflightReadMode
import com.tangem.operations.PreflightReadTask
import com.tangem.operations.sign.SignHashResponse

class MultiMessageTask : CardSessionRunnable<SignHashResponse> {

    private val message1 = Message("Header - 1", "Body - 1")
    private val message2 = Message("Header - 2", "Body - 2")

    override fun run(session: CardSession, callback: CompletionCallback<SignHashResponse>) {
        session.setMessage(message1)
        Thread.sleep(THREAD_DELAY_MS)
        session.setMessage(message2)
        Thread.sleep(THREAD_DELAY_MS)
        PreflightReadTask(PreflightReadMode.None).run(session) {
            when (it) {
                is CompletionResult.Success -> {
                    session.setMessage(Message(header = "Success", body = "SignHashCommand"))
                    Thread.sleep(THREAD_DELAY_MS)
                    callback(
                        CompletionResult.Failure(
                            TangemSdkError.ExceptionError(
                                Throwable("Test error message")
                            )
                        )
                    )
                }
                is CompletionResult.Failure -> {
                    session.setMessage(Message(header = "Success", body = "SignHashCommand"))
                    Thread.sleep(THREAD_DELAY_MS)
                    callback(CompletionResult.Failure(it.error))
                }
            }
        }
    }

    private companion object {
        const val THREAD_DELAY_MS = 2_000L
    }
}

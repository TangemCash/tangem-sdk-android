package com.tangem.tangem_demo.ui.separtedCommands.task

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

    val message1 = Message("Header - 1", "Body - 1")
    val message2 = Message("Header - 2", "Body - 2")

    override fun run(session: CardSession, callback: CompletionCallback<SignHashResponse>) {
        session.setMessage(message1)
        Thread.sleep(2000)
        session.setMessage(message2)
        Thread.sleep(2000)
        PreflightReadTask(PreflightReadMode.None).run(session) {
            when (it) {
                is CompletionResult.Success -> {
                    session.setMessage(Message("Success", "SignHashCommand"))
                    Thread.sleep(2000)
                    callback(CompletionResult.Failure(TangemSdkError.ExceptionError(
                        Throwable("Test error message")
                    )))
                }
                is CompletionResult.Failure -> {
                    session.setMessage(Message("Success", "SignHashCommand"))
                    Thread.sleep(2000)
                    callback(CompletionResult.Failure(it.error))
                }
            }
        }
    }
}

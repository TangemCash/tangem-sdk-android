package com.tangem.operations.resetcode

import com.tangem.common.CompletionResult
import com.tangem.common.SuccessResponse
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.operations.PreflightReadMode

class ResetPinTask(
    private val confirmationCard: ConfirmationCard,
    private val accessCode: ByteArray,
    private val passcode: ByteArray,
) : CardSessionRunnable<SuccessResponse> {

    override fun preflightReadMode(): PreflightReadMode = PreflightReadMode.ReadCardOnly

    override fun run(session: CardSession, callback: CompletionCallback<SuccessResponse>) {
        AuthorizeResetPinTokenCommand(confirmationCard).run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    ResetPinCommand(accessCode, passcode).run(session, callback)
                }
                is CompletionResult.Failure -> callback(result)
            }
        }
    }
}

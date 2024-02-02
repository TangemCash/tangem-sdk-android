package com.tangem.operations.backup

import com.tangem.common.CompletionResult
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback

class StartPrimaryCardLinkingTask : CardSessionRunnable<PrimaryCard> {

    override val allowsRequestAccessCodeFromRepository: Boolean
        get() = false

    override fun run(session: CardSession, callback: CompletionCallback<PrimaryCard>) {
        StartPrimaryCardLinkingCommand()
            .run(session) { result ->
                when (result) {
                    is CompletionResult.Success -> {
                        callback(CompletionResult.Success(result.data))
                        // loadIssuerSignature(result.data, session, callback)
                    }

                    is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
                }
            }
    }
}

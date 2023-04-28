package com.tangem.operations.usersetttings

import com.tangem.common.CompletionResult
import com.tangem.common.SuccessResponse
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.guard
import com.tangem.operations.PreflightReadMode

class SetUserCodeRecoveryAllowedTask(
    private val isAllowed: Boolean,
) : CardSessionRunnable<SuccessResponse> {

    override fun preflightReadMode(): PreflightReadMode = PreflightReadMode.ReadCardOnly

    override fun run(session: CardSession, callback: CompletionCallback<SuccessResponse>) {
        val card = session.environment.card.guard {
            callback(CompletionResult.Failure(TangemSdkError.MissingPreflightRead()))
            return
        }
        val userSettings = card.userSettings.copy(isUserCodeRecoveryAllowed = isAllowed)
        SetUserSettingsCommand(userSettings).run(session) { result ->
            val finalResult = when (result) {
                is CompletionResult.Failure -> CompletionResult.Failure(result.error)
                is CompletionResult.Success -> CompletionResult.Success(SuccessResponse(result.data.cardId))
            }
            callback(finalResult)
        }
    }
}

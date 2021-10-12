package com.tangem.operations.pins

import com.tangem.common.CompletionResult
import com.tangem.common.UserCodeType
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.operations.CommandResponse

/**
 * Created by Anton Zhilenkov on 21/07/2021.
 */
data class CheckUserCodesResponse(
    val isAccessCodeSet: Boolean,
    val isPasscodeSet: Boolean,
) : CommandResponse

class CheckUserCodesCommand : CardSessionRunnable<CheckUserCodesResponse> {

    override fun run(session: CardSession, callback: CompletionCallback<CheckUserCodesResponse>) {
        val command = SetUserCodeCommand.change(
                session.environment.accessCode.value,
                session.environment.passcode.value,
        )
        command.shouldRestrictDefaultCodes = false
        command.isPasscodeRequire = false

        command.run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    val response = CheckUserCodesResponse(
                            session.environment.isUserCodeSet(UserCodeType.AccessCode),
                            session.environment.isUserCodeSet(UserCodeType.Passcode),
                    )
                    callback(CompletionResult.Success(response))
                }
                is CompletionResult.Failure -> {
                    if (result.error is TangemSdkError.InvalidParams) {
                        val response = CheckUserCodesResponse(
                                session.environment.isUserCodeSet(UserCodeType.AccessCode),
                                true,
                        )
                        callback(CompletionResult.Success(response))
                    } else {
                        callback(CompletionResult.Failure(result.error))
                    }
                }
            }
        }
    }
}
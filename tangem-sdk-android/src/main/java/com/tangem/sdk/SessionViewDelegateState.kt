package com.tangem.sdk

import com.tangem.Message
import com.tangem.WrongValueType
import com.tangem.common.UserCodeType
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemError
import com.tangem.operations.resetcode.ResetPinService

sealed class SessionViewDelegateState {
    data class Error(val error: TangemError) : SessionViewDelegateState()
    data class Success(val message: Message?) : SessionViewDelegateState()
    data class SecurityDelay(val ms: Int, val totalDurationSeconds: Int) :
        SessionViewDelegateState()

    data class Delay(val total: Int, val current: Int, val step: Int) : SessionViewDelegateState()
    data class Ready(val cardId: String?) : SessionViewDelegateState()
    data class PinRequested(
        val type: UserCodeType,
        val isFirstAttempt: Boolean,
        val showForgotButton: Boolean,
        val cardId: String?,
        val callback: CompletionCallback<String>
    ) : SessionViewDelegateState()

    data class PinChangeRequested(
        val type: UserCodeType,
        val cardId: String?,
        val callback: CompletionCallback<String>
    ) : SessionViewDelegateState()

    data class ResetCodes(
        val type: UserCodeType,
        val state: ResetPinService.State,
        val cardId: String?,
        val callback: CompletionCallback<Boolean>
    ) : SessionViewDelegateState()

    data class WrongCard(val wrongValueType: WrongValueType) : SessionViewDelegateState()
    object TagLost : SessionViewDelegateState()
    object TagConnected : SessionViewDelegateState()
    object HowToTap : SessionViewDelegateState()
    object None : SessionViewDelegateState()
}
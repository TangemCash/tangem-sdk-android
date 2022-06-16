package com.tangem.jvm

import com.tangem.Log
import com.tangem.Message
import com.tangem.RequestUserCodeResult
import com.tangem.SessionViewDelegate
import com.tangem.WrongValueType
import com.tangem.common.CompletionResult
import com.tangem.common.StringsLocator
import com.tangem.common.UserCodeType
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.Config
import com.tangem.common.core.TangemError
import com.tangem.common.extensions.VoidCallback
import com.tangem.operations.resetcode.ResetCodesViewDelegate
import com.tangem.operations.resetcode.ResetCodesViewState

class LoggingSessionDelegate : SessionViewDelegate {
    override val resetCodesViewDelegate: ResetCodesViewDelegate = EmptyResetCodesViewDelegate()

    override fun dismiss() {
    }

    override fun onDelay(total: Int, current: Int, step: Int) {
        Log.session { "Performing long task: $current of $total" }
    }

    override fun onError(error: TangemError) {
        Log.error { "Error: ${error.code}, ${error.javaClass.simpleName}" }
    }

    override fun onSessionStopped(message: Message?) {
        Log.session { "Session completed" }
    }

    override fun onTagConnected() {
        Log.session { "Tag has been connected!" }
    }

    override fun onSessionStarted(cardId: String?, message: Message?, enableHowTo: Boolean) {
        Log.session { "Session started" }
    }

    override fun requestUserCode(
        type: UserCodeType,
        isFirstAttempt: Boolean,
        showForgotButton: Boolean,
        showRememberCodeToggle: Boolean,
        rememberCodeToggled: Boolean,
        cardId: String?,
        callback: CompletionCallback<RequestUserCodeResult>
    ) {
        if (isFirstAttempt) {
            Log.view { "TAG: Enter PIN:" }
        } else {
            Log.view { "TAG: Wrong Pin. Enter correct PIN:" }
        }

        val pin = readLine()
        pin?.let { code ->
            callback.invoke(
                CompletionResult.Success(
                    RequestUserCodeResult(
                        code = code,
                        rememberCode = false,
                    )
                )
            )
        }
    }

    override fun requestUserCodeChange(type: UserCodeType, callback: CompletionCallback<String>) {
        Log.view { "TAG: Enter new PIN:" }
        val pin = readLine()
        pin?.let { callback.invoke(CompletionResult.Success(it)) }
    }

    override fun onSecurityDelay(ms: Int, totalDurationSeconds: Int) {
        Log.warning { "Security delay: $ms" }
    }

    override fun onTagLost() {
        Log.warning { "Tag lost!" }
    }

    override fun onWrongCard(wrongValueType: WrongValueType) {
        Log.warning { "You tapped a different card. Please match the in-app Card ID to the your physical Card ID to continue this process." }
    }

    override fun setConfig(config: Config) {
    }

    override fun setMessage(message: Message?) {
    }

    override fun attestationDidFail(isDevCard: Boolean, positive: VoidCallback, negative: VoidCallback) {
    }

    override fun attestationCompletedOffline(positive: VoidCallback, negative: VoidCallback, retry: VoidCallback) {
    }

    override fun attestationCompletedWithWarnings(positive: VoidCallback) {
    }

    companion object {
        const val TAG = "DELEGATE"
    }
}

class EmptyResetCodesViewDelegate : ResetCodesViewDelegate {
    override var stopSessionCallback: VoidCallback = {}
    override val stringsLocator: StringsLocator = MockStringLocator()

    override fun setState(state: ResetCodesViewState) {}

    override fun hide(callback: VoidCallback) {}

    override fun showError(error: TangemError) {}

    override fun showAlert(title: String, message: String, onContinue: VoidCallback) {}
}

class MockStringLocator: StringsLocator {
    override fun getString(stringId: StringsLocator.ID, vararg formatArgs: String) = ""
}

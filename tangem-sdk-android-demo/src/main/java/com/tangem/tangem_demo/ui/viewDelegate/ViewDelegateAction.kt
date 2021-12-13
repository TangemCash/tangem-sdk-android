package com.tangem.tangem_demo.ui.viewDelegate

import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.tangem.SessionViewDelegate
import com.tangem.WrongValueType
import com.tangem.common.core.TangemSdkError
import com.tangem.tangem_demo.R
import com.tangem.tangem_demo.inflate
import com.tangem.tangem_demo.ui.extension.withMainContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Created by Anton Zhilenkov on 13/12/2021.
 */
interface ViewDelegateAction {
    fun init(container: ViewGroup, delegate: SessionViewDelegate, scope: CoroutineScope)
}

abstract class BaseDelegateAction : ViewDelegateAction {

    protected lateinit var delegate: SessionViewDelegate
        private set

    lateinit var actionLayout: ViewGroup
        private set
    protected lateinit var tvActionInfo: TextView
        private set
    protected lateinit var btnActionRun: Button
        private set

    override fun init(
        container: ViewGroup,
        viewDelegate: SessionViewDelegate,
        scope: CoroutineScope,
    ) {
        actionLayout = container.inflate(R.layout.layout_action, false)
        container.addView(actionLayout)
        tvActionInfo = actionLayout.findViewById(R.id.tvActionInfo)
        btnActionRun = actionLayout.findViewById(R.id.btnActionRun)

        tvActionInfo.text = getInfo()
        btnActionRun.setOnClickListener { scope.launch { runInternal() } }

        delegate = viewDelegate
    }

    abstract fun getInfo(): String

    protected abstract suspend fun runInternal()
}

class TagConnectTagLostError : BaseDelegateAction() {

    override fun getInfo(): String = "StartSession, TagConnected, TagLost, TagConnected, TagLost, Dismiss"

    override suspend fun runInternal() {
        withMainContext { delegate.onSessionStarted(null, null, true) }
        delay(2000)
        withMainContext { delegate.onTagConnected() }
        delay(2000)
        withMainContext { delegate.onTagLost() }
        delay(2000)
        withMainContext { delegate.onTagConnected() }
        delay(2000)
        withMainContext { delegate.onTagLost() }
        delay(2000)
        withMainContext { delegate.onError(TangemSdkError.UnknownError()) }
        delay(2000)
        withMainContext { delegate.onTagLost() }
        delay(2000)
        withMainContext { delegate.dismiss() }
    }
}

class SecurityDelay(
    private val delaySeconds: Int,
) : BaseDelegateAction() {

    override fun getInfo(): String = "StartSession, TagConnected, onSecurityDelay(ms, total), SessionStopped"

    override suspend fun runInternal() {
        withMainContext { delegate.onSessionStarted(null, null, true) }
        delay(2000)
        withMainContext { delegate.onTagConnected() }
        delay(1000)
        val totalDurationMs = delaySeconds * 1000
        var currentMs = totalDurationMs / 10

        repeat(delaySeconds) {
            delay(1000)
            currentMs -= 100
            withMainContext { delegate.onSecurityDelay(currentMs, totalDurationMs) }
        }
        withMainContext { delegate.onSessionStopped(null) }
    }
}

class SecurityDelayPinFails(
    private val delayMs: Int,
) : BaseDelegateAction() {

    override fun getInfo(): String = "StartSession, TagConnected, onSecurityDelay(ms, 0), TagLost, Dismiss"

    override suspend fun runInternal() {
        withMainContext { delegate.onSessionStarted(null, null, true) }
        delay(2000)
        withMainContext { delegate.onTagConnected() }
        delay(1000)
        var currentMs = delayMs

        repeat(delayMs / 100) {
            delay(1000)
            currentMs -= 100
            withMainContext { delegate.onSecurityDelay(currentMs, 0) }
        }
        delay(2000)
        withMainContext { delegate.onTagLost() }
        delay(1000)
        withMainContext { delegate.dismiss() }
    }
}

class WrongCard : BaseDelegateAction() {

    override fun getInfo(): String = "StartSession, TagConnected, WrongCard(id), WrongCard(type), Dismiss"

    override suspend fun runInternal() {
        withMainContext { delegate.onSessionStarted(null, null, true) }
        delay(2000)
        withMainContext { delegate.onTagConnected() }
        delay(2000)
        withMainContext { delegate.onWrongCard(WrongValueType.CardId) }
        delay(6000)
        withMainContext { delegate.onWrongCard(WrongValueType.CardType) }
        delay(6000)
        withMainContext { delegate.dismiss() }
    }
}

//        delegate.onDelay(total: Int, current: Int, step: Int)
//        delegate.onError(error: TangemError)
//        delegate.requestUserCode(type: UserCodeType, isFirstAttempt: Boolean, callback: (code: String) -> Unit)
//        delegate.requestUserCodeChange(type: UserCodeType, callback: (newCode: String?) -> Unit)
//        delegate.attestationDidFail(isDevCard: Boolean, positive: VoidCallback, negative: VoidCallback)
//        delegate.attestationCompletedOffline(positive: VoidCallback, negative: VoidCallback, retry: VoidCallback)
//        delegate.attestationCompletedWithWarnings(positive: VoidCallback)

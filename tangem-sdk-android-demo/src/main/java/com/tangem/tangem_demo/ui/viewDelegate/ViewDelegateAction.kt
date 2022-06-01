package com.tangem.tangem_demo.ui.viewDelegate

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.LinearLayoutCompat
import com.google.android.material.slider.Slider
import com.tangem.SessionViewDelegate
import com.tangem.WrongValueType
import com.tangem.common.UserCodeType
import com.tangem.common.core.TangemSdkError
import com.tangem.tangem_demo.R
import com.tangem.tangem_demo.inflate
import com.tangem.tangem_demo.ui.extension.withMainContext
import com.tangem.tangem_sdk_new.extensions.dpToPx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Created by Anton Zhilenkov on 13/12/2021.
 */
interface ViewDelegateAction {
    fun inflateViews(container: ViewGroup)
    fun init(viewDelegate: SessionViewDelegate, scope: CoroutineScope)
    fun info(): String
    fun usedCommandsInfo(): String
    fun getCommandsPool(): suspend () -> Unit
}

abstract class BaseDelegateAction : ViewDelegateAction {

    protected lateinit var delegate: SessionViewDelegate
        private set

    protected var actionLayout: ViewGroup? = null
    protected var additionContainer: ViewGroup? = null

    protected var tvActionInfo: TextView? = null
    protected var btnActionRun: Button? = null

    override fun inflateViews(container: ViewGroup) {
        val actionLayout = container.inflate<ViewGroup>(R.layout.layout_action, false)
        container.addView(actionLayout)
        additionContainer = actionLayout.findViewById(R.id.action_additional_container)
        btnActionRun = actionLayout.findViewById(R.id.btnActionRun)

        val details = StringBuilder().apply {
            val checkFor = info()
            if (checkFor.isNotEmpty()) append("Info: ").append(info()).append("\n\n")
            append("Commands: ").append(usedCommandsInfo())
        }
        tvActionInfo = actionLayout.findViewById(R.id.tvActionInfo)
        tvActionInfo?.text = details.toString()
        this.actionLayout = actionLayout
    }

    override fun init(viewDelegate: SessionViewDelegate, scope: CoroutineScope) {
        delegate = viewDelegate
        btnActionRun?.setOnClickListener {
            scope.launch {
                getCommandsPool().invoke()
                withMainContext {
                    actionLayout?.context?.let { context ->
                        Toast.makeText(context, "Complete", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun info(): String = ""
}

class TagConnectTagLostError : BaseDelegateAction() {

    override fun info(): String {
        return "Tag connect/lost, error"
    }

    override fun usedCommandsInfo(): String {
        return "StartSession, TagConnected, TagLost, TagConnected, TagLost, Error, TagLost, Dismiss"
    }

    override fun getCommandsPool(): suspend () -> Unit = suspend {
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

class SecurityDelay : BaseDelegateAction() {

    private val step = 1000f
    private val maxLength = 60000f
    private val startPosition = 15000f

    private lateinit var slider: Slider

    override fun inflateViews(container: ViewGroup) {
        super.inflateViews(container)

        val sliderId = createSecurityDelaySliderContainer(additionContainer, step, maxLength, startPosition)
        additionContainer?.findViewById<Slider>(sliderId)?.let { slider = it }
    }

    override fun info(): String {
        return "Standard security delay"
    }

    override fun usedCommandsInfo(): String {
        return "StartSession, TagConnected, SecurityDelay(x, ms), SessionStopped"
    }

    override fun getCommandsPool(): suspend () -> Unit = suspend {
        withMainContext { delegate.onSessionStarted(null, null, true) }
        delay(2000)
        withMainContext { delegate.onTagConnected() }

        val totalDurationMs = slider.value.toInt() + 1000
        val securityDelayCounts = totalDurationMs / 1000
        var currentMs = totalDurationMs / 10
        repeat(securityDelayCounts) {
            delay(1000)
            currentMs -= 100
            withMainContext { delegate.onSecurityDelay(currentMs, totalDurationMs) }
        }

        withMainContext { delegate.onSessionStopped(null) }
    }
}

class SecurityDelayPinFails : BaseDelegateAction() {

    private val step = 1f
    private val maxLength = 4500f
    private val startPosition = 645f

    private lateinit var slider: Slider

    override fun inflateViews(container: ViewGroup) {
        super.inflateViews(container)

        val sliderId = createSecurityDelaySliderContainer(additionContainer, step, maxLength, startPosition)
        additionContainer?.findViewById<Slider>(sliderId)?.let { slider = it }
    }

    override fun info(): String {
        return "Security delay if user codes fails"
    }

    override fun usedCommandsInfo(): String {
        return "StartSession, TagConnected, onSecurityDelay(x, 0), TagLost, Dismiss"
    }

    override fun getCommandsPool(): suspend () -> Unit = suspend {
        withMainContext { delegate.onSessionStarted(null, null, true) }
        delay(2000)
        withMainContext { delegate.onTagConnected() }
        delay(1000)

        val repeatCounts = slider.value.toInt() / 100
        var currentMs = slider.value.toInt()
        repeat(repeatCounts) {
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

    override fun info(): String {
        return "Wrong card error with types: cardId, cardType"
    }

    override fun usedCommandsInfo(): String {
        return "StartSession, TagConnected, WrongCard(id), WrongCard(type), Dismiss"
    }

    override fun getCommandsPool(): suspend () -> Unit = suspend {
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

class OnError : BaseDelegateAction() {

    override fun info(): String {
        return "Show 'WalletCannotBeCreated' error"
    }

    override fun usedCommandsInfo(): String {
        return "StartSession, TagConnected, Error, Dismiss"
    }

    override fun getCommandsPool(): suspend () -> Unit = suspend {
        withMainContext { delegate.onSessionStarted(null, null, true) }
        delay(2000)
        withMainContext { delegate.onTagConnected() }
        delay(2000)
        withMainContext { delegate.onError(TangemSdkError.WalletCannotBeCreated()) }
        delay(6000)
        withMainContext { delegate.dismiss() }
    }
}

class RequestAccessCode : BaseDelegateAction() {

    override fun info(): String {
        return "Request Access codes with first/second/first attempts"
    }

    override fun usedCommandsInfo(): String {
        return "SessionStarted, RequestUserCode(access,true), Dismiss, " +
                "SessionStarted, RequestUserCode(access,false), Dismiss, " +
                "SessionStarted, RequestUserCode(access,true), Dismiss"
    }

    override fun getCommandsPool(): suspend () -> Unit = suspend {
        withMainContext { delegate.onSessionStarted(null, null, true) }
        delay(500)
        withMainContext {
            delegate.requestUserCode(
                type = UserCodeType.AccessCode,
                isFirstAttempt = true,
                showForgotButton = false,
                showRememberCodeToggle = false,
                rememberCodeToggled = false,
                cardId = null,
                {}
            )
        }
        delay(2000)
        withMainContext { delegate.dismiss() }
        delay(500)
        withMainContext { delegate.onSessionStarted(null, null, true) }
        delay(500)
        withMainContext {
            delegate.requestUserCode(
                type = UserCodeType.AccessCode,
                isFirstAttempt = false,
                showForgotButton = false,
                showRememberCodeToggle = false,
                rememberCodeToggled = false,
                cardId = null,
                {},
            )
        }
        delay(2000)
        withMainContext { delegate.dismiss() }
        delay(500)
        withMainContext { delegate.onSessionStarted(null, null, true) }
        delay(500)
        withMainContext {
            delegate.requestUserCode(
                type = UserCodeType.AccessCode,
                isFirstAttempt = true,
                showForgotButton = false,
                showRememberCodeToggle = false,
                rememberCodeToggled = false,
                cardId = null,
                {},
            )
        }
        delay(2000)
        withMainContext { delegate.dismiss() }
    }
}

class SingleRequestAccessCode(
    private val showForgotButton: Boolean
) : BaseDelegateAction() {

    override fun info(): String = "Request Access code. Show the Forgot button = $showForgotButton"

    override fun usedCommandsInfo(): String = "RequestUserCode(access,true), Dismiss"

    override fun getCommandsPool(): suspend () -> Unit = suspend {
        withMainContext { delegate.onSessionStarted(null, null, true) }
        delay(500)
        withMainContext {
            delegate.requestUserCode(
                type = UserCodeType.AccessCode,
                isFirstAttempt = true,
                showForgotButton = showForgotButton,
                showRememberCodeToggle = false,
                rememberCodeToggled = false,
                cardId = null,
                {},
            )
        }
    }
}

class RequestPasscode : BaseDelegateAction() {

    override fun info(): String {
        return "Request Passcodes with first/second/first attempts"
    }

    override fun usedCommandsInfo(): String {
        return "SessionStarted, RequestUserCode(passcode,true), Dismiss, " +
                "SessionStarted, RequestUserCode(passcode,false), Dismiss, " +
                "SessionStarted, RequestUserCode(passcode,true), Dismiss"
    }

    override fun getCommandsPool(): suspend () -> Unit = suspend {
        withMainContext { delegate.onSessionStarted(null, null, true) }
        delay(500)
        withMainContext {
            delegate.requestUserCode(
                type = UserCodeType.Passcode,
                isFirstAttempt = true,
                showForgotButton = false,
                showRememberCodeToggle = false,
                rememberCodeToggled = false,
                cardId = null,
                {},
            )
        }
        delay(2000)
        withMainContext { delegate.dismiss() }
        delay(500)
        withMainContext { delegate.onSessionStarted(null, null, true) }
        delay(500)
        withMainContext {
            delegate.requestUserCode(
                type = UserCodeType.Passcode,
                isFirstAttempt = false,
                showForgotButton = false,
                showRememberCodeToggle = false,
                rememberCodeToggled = false,
                cardId = null,
                {},
            )
        }
        delay(2000)
        withMainContext { delegate.dismiss() }
        delay(500)
        withMainContext { delegate.onSessionStarted(null, null, true) }
        delay(500)
        withMainContext {
            delegate.requestUserCode(
                type = UserCodeType.Passcode,
                isFirstAttempt = true,
                showForgotButton = false,
                showRememberCodeToggle = false,
                rememberCodeToggled = false,
                cardId = null,
                {},
            )
        }
        delay(2000)
        withMainContext { delegate.dismiss() }
    }
}

class RequestUserCode : BaseDelegateAction() {

    private lateinit var accessCode: RequestAccessCode
    private lateinit var passcode: RequestPasscode

    override fun init(viewDelegate: SessionViewDelegate, scope: CoroutineScope) {
        super.init(viewDelegate, scope)

        accessCode = RequestAccessCode().apply { init(viewDelegate, scope) }
        passcode = RequestPasscode().apply { init(viewDelegate, scope) }
    }

    override fun info(): String {
        return "Request access/passcode successively, with first/second/first attempts"
    }

    override fun usedCommandsInfo(): String {
        return "from 'Request access/passcode' actions"
    }

    override fun getCommandsPool(): suspend () -> Unit = suspend {
        accessCode.getCommandsPool().invoke()
        delay(1000)
        passcode.getCommandsPool().invoke()
    }
}

class RequestPinSetup : BaseDelegateAction() {

    override fun info(): String = "Request pin setup"

    override fun usedCommandsInfo(): String = "SessionStarted, RequestPinSetup"

    override fun getCommandsPool(): suspend () -> Unit = suspend {
        withMainContext { delegate.onSessionStarted(null, null, true) }
        delay(500)
        withMainContext { delegate.requestUserCodeChange(UserCodeType.Passcode) {} }
    }
}

private fun createSecurityDelaySliderContainer(
    additionContainer: ViewGroup?,
    step: Float,
    valueTo: Float,
    value: Float
): Int {
    additionContainer ?: return View.NO_ID

    val context = additionContainer.context
    val llSliderContainer = LinearLayoutCompat(context).apply {
        gravity = Gravity.CENTER_VERTICAL
        setPadding(paddingLeft, paddingTop, dpToPx(8f).toInt(), paddingBottom)
    }
    llSliderContainer.orientation = LinearLayoutCompat.HORIZONTAL
    additionContainer.addView(llSliderContainer)

    val tvSliderValue = TextView(context)
    val slider = Slider(context).apply {
        val params = LinearLayoutCompat.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT)
        params.weight = 1f
        layoutParams = params
    }
    slider.stepSize = step
    slider.valueFrom = 0f
    slider.valueTo = valueTo
    slider.addOnChangeListener { slider, value, fromUser -> tvSliderValue.text = "${value.toInt()} ms" }
    slider.value = value
    slider.id = View.generateViewId()

    llSliderContainer.addView(slider)
    llSliderContainer.addView(tvSliderValue)
    return slider.id
}

//        delegate.onDelay(total: Int, current: Int, step: Int)
//        delegate.requestUserCode(type: UserCodeType, isFirstAttempt: Boolean, callback: (code: String) -> Unit)
//        delegate.requestUserCodeChange(type: UserCodeType, callback: (newCode: String?) -> Unit)
//        delegate.attestationDidFail(isDevCard: Boolean, positive: VoidCallback, negative: VoidCallback)
//        delegate.attestationCompletedOffline(positive: VoidCallback, negative: VoidCallback, retry: VoidCallback)
//        delegate.attestationCompletedWithWarnings(positive: VoidCallback)

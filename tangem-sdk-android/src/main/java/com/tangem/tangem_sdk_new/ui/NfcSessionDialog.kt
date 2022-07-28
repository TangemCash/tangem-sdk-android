package com.tangem.tangem_sdk_new.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.tangem.Log
import com.tangem.Message
import com.tangem.common.CompletionResult
import com.tangem.common.Timer
import com.tangem.common.core.SessionEnvironment
import com.tangem.common.core.TangemSdkError
import com.tangem.tangem_sdk_new.R
import com.tangem.tangem_sdk_new.SessionViewDelegateState
import com.tangem.tangem_sdk_new.extensions.hide
import com.tangem.tangem_sdk_new.extensions.show
import com.tangem.tangem_sdk_new.nfc.NfcLocationProvider
import com.tangem.tangem_sdk_new.nfc.NfcManager
import com.tangem.tangem_sdk_new.postUI
import com.tangem.tangem_sdk_new.ui.widget.*
import com.tangem.tangem_sdk_new.ui.widget.howTo.HowToTapWidget
import com.tangem.tangem_sdk_new.ui.widget.progressBar.ProgressbarStateWidget
import kotlinx.coroutines.Dispatchers

class NfcSessionDialog(
    context: Context,
    private val nfcManager: NfcManager,
    private val nfcLocationProvider: NfcLocationProvider,
) : BaseSdkDialog(context) {

    private lateinit var taskContainer: ViewGroup
    private lateinit var howToContainer: ViewGroup

    private lateinit var headerWidget: HeaderWidget
    private lateinit var touchCardWidget: TouchCardWidget
    private lateinit var progressStateWidget: ProgressbarStateWidget
    private lateinit var pinCodeRequestWidget: PinCodeRequestWidget
    private lateinit var pinCodeSetChangeWidget: PinCodeModificationWidget
    private lateinit var messageWidget: MessageWidget
    private lateinit var howToTapWidget: HowToTapWidget

    private var currentState: SessionViewDelegateState? = null

    @Deprecated("Used to fix lack of security delay on cards with firmware version below 1.21")
    private var emulateSecurityDelayTimer: Timer? = null

    init {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_layout, null)
        setContentView(dialogView)
    }

    override fun setContentView(view: View) {
        super.setContentView(view)

        val nfcLocation = nfcLocationProvider.getLocation() ?: NfcLocation.model13

        taskContainer = view.findViewById(R.id.taskContainer)
        howToContainer = view.findViewById(R.id.howToContainer)

        headerWidget = HeaderWidget(view.findViewById(R.id.llHeader))
        touchCardWidget = TouchCardWidget(view.findViewById(R.id.rlTouchCard), nfcLocation)
        progressStateWidget = ProgressbarStateWidget(view.findViewById(R.id.clProgress))
        pinCodeRequestWidget = PinCodeRequestWidget(view.findViewById(R.id.csPinCode))
        pinCodeSetChangeWidget = PinCodeModificationWidget(view.findViewById(R.id.llChangePin), PinCodeModificationWidget.Mode.SET)
        messageWidget = MessageWidget(view.findViewById(R.id.llMessage))
        howToTapWidget = HowToTapWidget(howToContainer, nfcManager, nfcLocationProvider)

        stateWidgets.add(headerWidget)
        stateWidgets.add(touchCardWidget)
        stateWidgets.add(progressStateWidget)
        stateWidgets.add(pinCodeRequestWidget)
        stateWidgets.add(pinCodeSetChangeWidget)
        stateWidgets.add(messageWidget)
        stateWidgets.add(howToTapWidget)

        headerWidget.onHowTo = { show(SessionViewDelegateState.HowToTap) }

        behavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    @UiThread
    fun showHowTo(enable: Boolean) {
        headerWidget.howToIsEnabled = enable
    }

    @UiThread
    fun setInitialMessage(message: Message?) {
        messageWidget.setInitialMessage(message)
    }

    @UiThread
    fun setMessage(message: Message?) {
        messageWidget.setMessage(message)
    }

    fun show(state: SessionViewDelegateState) {
        if (ownerActivity == null || ownerActivity?.isFinishing == true) return

        postUI {
            if (!this.isShowing) this.show()
            when (state) {
                is SessionViewDelegateState.Ready -> onReady(state)
                is SessionViewDelegateState.Success -> onSuccess(state)
                is SessionViewDelegateState.Error -> onError(state)
                is SessionViewDelegateState.SecurityDelay -> onSecurityDelay(state)
                is SessionViewDelegateState.Delay -> onDelay(state)
                is SessionViewDelegateState.PinRequested -> onPinRequested(state)
                is SessionViewDelegateState.PinChangeRequested -> onPinChangeRequested(state)
                is SessionViewDelegateState.WrongCard -> onWrongCard(state)
                SessionViewDelegateState.TagConnected -> onTagConnected(state)
                SessionViewDelegateState.TagLost -> onTagLost(state)
                SessionViewDelegateState.HowToTap -> howToTap(state)
            }
            currentState = state
        }
    }

    private fun onReady(state: SessionViewDelegateState.Ready) {
        setStateAndShow(state, headerWidget, touchCardWidget, messageWidget)
    }

    private fun onSuccess(state: SessionViewDelegateState.Success) {
        setStateAndShow(state, headerWidget, progressStateWidget, messageWidget)
        performHapticFeedback()
        postUI(1000) { cancel() }
    }

    private fun onError(state: SessionViewDelegateState.Error) {
        setStateAndShow(state, headerWidget, progressStateWidget, messageWidget)
        performHapticFeedback()
    }

    private fun onSecurityDelay(state: SessionViewDelegateState.SecurityDelay) {
        if (state.ms == SessionEnvironment.missingSecurityDelayCode) {
            activateTrickySecurityDelay(state.totalDurationSeconds.toLong())
            return
        }
        setStateAndShow(state, headerWidget, progressStateWidget, messageWidget)
        performHapticFeedback()
    }

    private fun onDelay(state: SessionViewDelegateState.Delay) {
        setStateAndShow(state, headerWidget, progressStateWidget, messageWidget)
        performHapticFeedback()
    }

    private fun onPinRequested(state: SessionViewDelegateState.PinRequested) {
        val delayMs = if (currentState is SessionViewDelegateState.SecurityDelay) 950L else 0L

        postUI(delayMs) {
            enableBottomSheetAnimation()
            headerWidget.onClose = {
                dismissWithAnimation = false
                cancel()
            }
            behavior.state = BottomSheetBehavior.STATE_EXPANDED

            pinCodeRequestWidget.onContinue = {
                enableBottomSheetAnimation()
                pinCodeRequestWidget.onContinue = null
                setStateAndShow(getEmptyOnReadyEvent(), headerWidget, touchCardWidget, messageWidget)
                postUI(200) { state.callback(it) }
            }

            setStateAndShow(state, headerWidget, pinCodeRequestWidget)
            performHapticFeedback()
        }
    }

    private fun onPinChangeRequested(state: SessionViewDelegateState.PinChangeRequested) {
        enableBottomSheetAnimation()
        behavior.state = BottomSheetBehavior.STATE_EXPANDED

        headerWidget.howToIsEnabled = false
        headerWidget.onClose = {
            dismissWithAnimation = false
            cancel()
        }
        // userCode changes failed if an user dismiss the dialog from any moment
        pinCodeSetChangeWidget.onBottomSheetDismiss = {
            state.callback(CompletionResult.Failure(TangemSdkError.UserCancelled()))
        }
        pinCodeSetChangeWidget.onSave = {
            enableBottomSheetAnimation()
            // disable sending the error if dialog closed after accepting an userCode
            pinCodeSetChangeWidget.onBottomSheetDismiss = null
            pinCodeSetChangeWidget.onSave = null

            setStateAndShow(getEmptyOnReadyEvent(), headerWidget, touchCardWidget, messageWidget)
            postUI(200) { state.callback(CompletionResult.Success(it.trim())) }
        }

        setStateAndShow(state, headerWidget, pinCodeSetChangeWidget)
        performHapticFeedback()
    }

    private fun onTagLost(state: SessionViewDelegateState) {
        if (currentState is SessionViewDelegateState.Success ||
                currentState is SessionViewDelegateState.PinRequested ||
                currentState is SessionViewDelegateState.PinChangeRequested) {
            return
        }
        setStateAndShow(state, headerWidget, touchCardWidget, messageWidget)
    }

    private fun onTagConnected(state: SessionViewDelegateState) {
        setStateAndShow(state, headerWidget, progressStateWidget, messageWidget)
    }

    private fun onWrongCard(state: SessionViewDelegateState.WrongCard) {
        Log.view { "showing wrong card. Type: ${state.wrongValueType}" }
        if (currentState !is SessionViewDelegateState.WrongCard) {
            performHapticFeedback()
            setStateAndShow(state, headerWidget, progressStateWidget, messageWidget)
            postUI(4000) {
                currentState = getEmptyOnReadyEvent()
                setStateAndShow(currentState!!, headerWidget, touchCardWidget, messageWidget)
            }
        }
    }

    private fun howToTap(state: SessionViewDelegateState) {
        Log.view { "showing how to tap" }
        enableBottomSheetAnimation()
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        taskContainer.hide()
        findDesignBottomSheetView()?.let { it.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT }
        howToContainer.show()

        howToTapWidget.previousState = currentState
        howToTapWidget.onCloseListener = {
            howToTapWidget.onCloseListener = null
            enableBottomSheetAnimation()
            howToContainer.hide()
            findDesignBottomSheetView()?.let { it.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT }
            taskContainer.show()

            howToTapWidget.previousState?.let {
                howToTapWidget.setState(it)
                show(it)
            }
            howToTapWidget.previousState = null
        }
        setStateAndShow(state, howToTapWidget)
    }

    override fun setStateAndShow(
        state: SessionViewDelegateState,
        vararg views: StateWidget<SessionViewDelegateState>
    ) {
        handleStateForTrickySecurityDelay(state)
        super.setStateAndShow(state, *views)
    }

    @Deprecated("Used to fix lack of security delay on cards with firmware version below 1.21")
    private fun activateTrickySecurityDelay(totalDuration: Long) {
        val timer = Timer(totalDuration, 100L, 1000L, dispatcher = Dispatchers.IO)
        timer.onComplete = {
            postUI { onSecurityDelay(SessionViewDelegateState.SecurityDelay(0, 0)) }
        }
        timer.onTick = {
            postUI { onSecurityDelay(SessionViewDelegateState.SecurityDelay((totalDuration - it).toInt(), 0)) }
        }
        timer.start()
        emulateSecurityDelayTimer = timer
    }

    @Deprecated("Used to fix lack of security delay on cards with firmware version below 1.21")
    private fun handleStateForTrickySecurityDelay(state: SessionViewDelegateState) {
        if (emulateSecurityDelayTimer == null) return

        when (state) {
            SessionViewDelegateState.TagConnected -> {
                emulateSecurityDelayTimer?.period?.let { activateTrickySecurityDelay(it) }
            }
            SessionViewDelegateState.TagLost -> {
                emulateSecurityDelayTimer?.cancel()
            }
            is SessionViewDelegateState.SecurityDelay -> {
                // do nothing for saving the securityDelay timer logic
            }
            else -> {
                emulateSecurityDelayTimer?.cancel()
                emulateSecurityDelayTimer = null
            }
        }
    }

    private fun getEmptyOnReadyEvent(): SessionViewDelegateState {
        return SessionViewDelegateState.Ready(headerWidget.cardId)
    }
}
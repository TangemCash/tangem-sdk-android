package com.tangem.sdk.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.tangem.Log
import com.tangem.ViewDelegateMessage
import com.tangem.common.CompletionResult
import com.tangem.common.Timer
import com.tangem.common.core.ScanTagImage
import com.tangem.common.core.SessionEnvironment
import com.tangem.common.core.TangemSdkError
import com.tangem.sdk.R
import com.tangem.sdk.SessionViewDelegateState
import com.tangem.sdk.extensions.hide
import com.tangem.sdk.extensions.show
import com.tangem.sdk.nfc.NfcLocationProvider
import com.tangem.sdk.nfc.NfcManager
import com.tangem.sdk.postUI
import com.tangem.sdk.ui.widget.HeaderWidget
import com.tangem.sdk.ui.widget.MessageWidget
import com.tangem.sdk.ui.widget.PinCodeModificationWidget
import com.tangem.sdk.ui.widget.PinCodeRequestWidget
import com.tangem.sdk.ui.widget.StateWidget
import com.tangem.sdk.ui.widget.TouchCardWidget
import com.tangem.sdk.ui.widget.howTo.HowToTapWidget
import com.tangem.sdk.ui.widget.progressBar.ProgressbarStateWidget
import kotlinx.coroutines.Dispatchers

class NfcSessionDialog(
    context: Context,
    private val nfcManager: NfcManager,
    private val nfcLocationProvider: NfcLocationProvider,
    private val iconScanRes: Int? = null,
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

        val nfcLocation = nfcLocationProvider.getLocation() ?: NfcLocation.Model13

        taskContainer = view.findViewById(R.id.taskContainer)
        howToContainer = view.findViewById(R.id.howToContainer)

        headerWidget = HeaderWidget(view.findViewById(R.id.llHeader))
        touchCardWidget = TouchCardWidget(view.findViewById(R.id.flImageContainer), nfcLocation)
        touchCardWidget.setIconScanRes(iconScanRes)
        progressStateWidget = ProgressbarStateWidget(view.findViewById(R.id.clProgress))
        pinCodeRequestWidget = PinCodeRequestWidget(view.findViewById(R.id.csPinCode))
        pinCodeSetChangeWidget = PinCodeModificationWidget(
            view.findViewById(R.id.llChangePin),
            PinCodeModificationWidget.Mode.SET,
        )
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
    fun setInitialMessage(message: ViewDelegateMessage?) {
        messageWidget.setInitialMessage(message)
    }

    @UiThread
    fun setMessage(message: ViewDelegateMessage?) {
        messageWidget.setMessage(message)
    }

    fun setScanImage(scanImage: ScanTagImage) {
        touchCardWidget.setScanImage(scanImage)
    }

    @Suppress("LongMethod", "ComplexMethod")
    fun show(state: SessionViewDelegateState) {
        postUI {
            if (ownerActivity?.isFinishing == true) return@postUI
            if (!this.isShowing) {
                show()
            }

            when (state) {
                is SessionViewDelegateState.Ready -> onReady(state)
                is SessionViewDelegateState.Success -> onSuccess(state)
                is SessionViewDelegateState.Error -> onError(state)
                is SessionViewDelegateState.SecurityDelay -> onSecurityDelay(state)
                is SessionViewDelegateState.Delay -> onDelay(state)
                is SessionViewDelegateState.PinRequested -> onPinRequested(state)
                is SessionViewDelegateState.PinChangeRequested -> onPinChangeRequested(state)
                is SessionViewDelegateState.WrongCard -> onWrongCard(state)
                is SessionViewDelegateState.TagConnected -> onTagConnected(state)
                is SessionViewDelegateState.TagLost -> onTagLost(state)
                is SessionViewDelegateState.HowToTap -> howToTap(state)
                is SessionViewDelegateState.None,
                is SessionViewDelegateState.ResetCodes,
                -> Unit
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
        postUI(msTime = 1000) { cancel() }
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
        val delayMs = if (currentState is SessionViewDelegateState.SecurityDelay) SECURITY_DELAY_MS else 0L

        postUI(delayMs) {
            enableBottomSheetAnimation()
            headerWidget.onClose = {
                dismissWithAnimation = false
                cancel()
            }
            behavior.state = BottomSheetBehavior.STATE_EXPANDED

            // pincode entering failed if an user dismiss the dialog from any moment
            pinCodeRequestWidget.onBottomSheetDismiss = {
                state.callback(CompletionResult.Failure(TangemSdkError.UserCancelled()))
            }

            pinCodeRequestWidget.onContinue = {
                enableBottomSheetAnimation()
                pinCodeRequestWidget.onContinue = null
                setStateAndShow(getEmptyOnReadyEvent(), headerWidget, touchCardWidget, messageWidget)
                postUI(msTime = 200) { state.callback(it) }
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
            postUI(msTime = 200) { state.callback(CompletionResult.Success(it.trim())) }
        }

        setStateAndShow(state, headerWidget, pinCodeSetChangeWidget)
        performHapticFeedback()
    }

    private fun onTagLost(state: SessionViewDelegateState) {
        when (currentState) {
            is SessionViewDelegateState.Success,
            is SessionViewDelegateState.PinRequested,
            is SessionViewDelegateState.PinChangeRequested,
            is SessionViewDelegateState.Error,
            -> {
                return
            }

            else -> {
                setStateAndShow(state, headerWidget, touchCardWidget, messageWidget)
            }
        }
    }

    private fun onTagConnected(state: SessionViewDelegateState) {
        setStateAndShow(state, headerWidget, progressStateWidget, messageWidget)
    }

    private fun onWrongCard(state: SessionViewDelegateState.WrongCard) {
        Log.view { "showing wrong card. Type: ${state.wrongValueType}" }
        if (currentState !is SessionViewDelegateState.WrongCard) {
            performHapticFeedback()
            setStateAndShow(state, headerWidget, progressStateWidget, messageWidget)
            postUI(msTime = 4000) {
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
        vararg views: StateWidget<SessionViewDelegateState>,
    ) {
        handleStateForTrickySecurityDelay(state)
        super.setStateAndShow(state, *views)
    }

    @Deprecated("Used to fix lack of security delay on cards with firmware version below 1.21")
    private fun activateTrickySecurityDelay(totalDuration: Long) {
        val timer = Timer(period = totalDuration, step = 100L, delayMs = 1000L, dispatcher = Dispatchers.IO)
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

    private companion object {
        const val SECURITY_DELAY_MS = 950L
    }
}

package com.tangem.sdk.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.operations.resetcode.ResetCodesViewState
import com.tangem.sdk.R
import com.tangem.sdk.SessionViewDelegateState
import com.tangem.sdk.ui.widget.HeaderWidget
import com.tangem.sdk.ui.widget.PinCodeModificationWidget
import com.tangem.sdk.ui.widget.ResetCodesWidget

class ResetCodesDialog(context: Context) : BaseSdkDialog(context) {

    private lateinit var pinCodeSetChangeWidget: PinCodeModificationWidget
    private lateinit var resetCodesWidget: ResetCodesWidget
    private lateinit var headerWidget: HeaderWidget

    init {
        val dialogView =
            LayoutInflater.from(context).inflate(R.layout.reset_codes_widget_layout, null)
        setContentView(dialogView)
    }

    override fun setContentView(view: View) {
        super.setContentView(view)

        headerWidget = HeaderWidget(view.findViewById(R.id.llHeader))
        pinCodeSetChangeWidget = PinCodeModificationWidget(
            mainView = view.findViewById(R.id.llChangePin),
            mode = PinCodeModificationWidget.Mode.RESET
        )
        resetCodesWidget = ResetCodesWidget(view.findViewById(R.id.reset_codes))

        stateWidgets.add(headerWidget)
        stateWidgets.add(pinCodeSetChangeWidget)
        stateWidgets.add(resetCodesWidget)

        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        show()
    }

    fun showState(state: ResetCodesViewState) {
        if (ownerActivity == null || ownerActivity?.isFinishing == true) return
        if (!this.isShowing) this.show()

        when (state) {
            ResetCodesViewState.Empty -> {}
            is ResetCodesViewState.RequestCode -> showRequestCode(state)
            is ResetCodesViewState.ResetCodes -> showResetCodes(state)
        }
    }

    private fun showRequestCode(state: ResetCodesViewState.RequestCode) {
        enableBottomSheetAnimation()
        behavior.state = BottomSheetBehavior.STATE_EXPANDED

        headerWidget.onClose = {
            dismissWithAnimation = false
            cancel()
        }
        // userCode changes failed if an user dismiss the dialog from any moment
        pinCodeSetChangeWidget.onBottomSheetDismiss = {
            state.callback(CompletionResult.Failure(TangemSdkError.UserCancelled()))
        }
        pinCodeSetChangeWidget.onSave = {
            // disable sending the error if dialog closed after accepting an userCode
            pinCodeSetChangeWidget.onBottomSheetDismiss = null
            pinCodeSetChangeWidget.onSave = null

            state.callback(CompletionResult.Success(it.trim()))
        }

        val viewState =
            SessionViewDelegateState.PinChangeRequested(state.type, state.cardId, state.callback)
        setStateAndShow(viewState, pinCodeSetChangeWidget, headerWidget)
        performHapticFeedback()
    }

    private fun showResetCodes(state: ResetCodesViewState.ResetCodes) {
        enableBottomSheetAnimation()
        behavior.state = BottomSheetBehavior.STATE_EXPANDED

        val viewState = SessionViewDelegateState.ResetCodes(
            type = state.type,
            state = state.state,
            cardId = state.cardId,
            callback = state.callback
        )
        setStateAndShow(viewState, resetCodesWidget, headerWidget)
    }
}
package com.tangem.tangem_sdk_new.ui.widget

import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.tangem.common.CompletionResult
import com.tangem.common.UserCodeType
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.tangem_sdk_new.R
import com.tangem.tangem_sdk_new.SessionViewDelegateState
import com.tangem.tangem_sdk_new.extensions.hideSoftKeyboard
import com.tangem.tangem_sdk_new.extensions.localizedDescription
import com.tangem.tangem_sdk_new.extensions.show
import com.tangem.tangem_sdk_new.extensions.showSoftKeyboard
import com.tangem.tangem_sdk_new.postUI

/**
 * Created by Anton Zhilenkov on 09/08/2020.
 */
class PinCodeRequestWidget(mainView: View) : BaseSessionDelegateStateWidget(mainView) {

    var onContinue: CompletionCallback<String>? = null

    private val tilPinCode = mainView.findViewById<TextInputLayout>(R.id.tilPinCode)
    private val etPinCode = mainView.findViewById<TextInputEditText>(R.id.etPinCode)
    private val btnContinue = mainView.findViewById<Button>(R.id.btnContinue)
    private val btnForgotCode = mainView.findViewById<Button>(R.id.btnForgotCode)
    private val expandingView = mainView.findViewById<View>(R.id.expandingView)

    init {
        etPinCode.isSingleLine = true
        etPinCode.imeOptions = EditorInfo.IME_ACTION_DONE
        etPinCode.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                btnContinue.performClick()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
    }

    fun canExpand(): Boolean = expandingView != null

    override fun setState(params: SessionViewDelegateState) {
        when (params) {
            is SessionViewDelegateState.PinRequested -> {
                val code = when (params.type) {
                    UserCodeType.AccessCode -> getString(R.string.pin1)
                    UserCodeType.Passcode -> getString(R.string.pin2)
                }
                tilPinCode.hint = code
                tilPinCode.error = if (params.isFirstAttempt) {
                    null
                } else {
                    when (params.type) {
                        UserCodeType.AccessCode -> TangemSdkError.WrongAccessCode()
                        UserCodeType.Passcode -> TangemSdkError.WrongPasscode()
                    }.localizedDescription(mainView.context)
                }

                etPinCode.setText("")
                postUI(1000) { etPinCode.showSoftKeyboard() }

                btnContinue.setOnClickListener {
                    val pin = etPinCode.text.toString()
                    if (pin.isEmpty()) {
                        tilPinCode.error = mainView.context.getString(R.string.pin_enter, code)
                    } else {
                        tilPinCode.error = null
                        tilPinCode.isErrorEnabled = false
                        mainView.requestFocus()
                        etPinCode.hideSoftKeyboard()
                        postUI(250) {
                            onContinue?.invoke(CompletionResult.Success(pin.trim()))
                        }
                    }
                }
                btnForgotCode.show(params.showForgotButton)
                btnForgotCode.setOnClickListener {
                    tilPinCode.error = null
                    tilPinCode.isErrorEnabled = false
                    mainView.requestFocus()
                    etPinCode.hideSoftKeyboard()
                    postUI(250) {
                        onContinue?.invoke(CompletionResult.Failure(TangemSdkError.UserForgotTheCode()))
                    }
                }
            }
        }
    }
}
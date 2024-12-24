package com.tangem.sdk.ui.widget

import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.tangem.common.CompletionResult
import com.tangem.common.UserCodeType
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.sdk.R
import com.tangem.sdk.SessionViewDelegateState
import com.tangem.sdk.extensions.hideSoftKeyboard
import com.tangem.sdk.extensions.localizedDescription
import com.tangem.sdk.extensions.show
import com.tangem.sdk.extensions.showSoftKeyboard
import com.tangem.sdk.postUI
import com.tangem.sdk.ui.common.disableAutofill
import com.tangem.sdk.ui.common.disableContextMenu

/**
 * Created by Anton Zhilenkov on 09/08/2020.
 */
class PinCodeRequestWidget(mainView: View) : BaseSessionDelegateStateWidget(mainView) {

    var onContinue: CompletionCallback<String>? = null

    private val tilPinCode = mainView.findViewById<TextInputLayout>(R.id.tilPinCode)
    private val etPinCode = mainView.findViewById<TextInputEditText>(R.id.etPinCode)
    private val btnContinue = mainView.findViewById<Button>(R.id.btnContinue)
    private val btnForgotCode = mainView.findViewById<Button>(R.id.btnForgotCode)

    init {
        etPinCode.isSingleLine = true
        etPinCode.imeOptions = EditorInfo.IME_ACTION_DONE
        etPinCode.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                btnContinue.performClick()
                true
            } else {
                false
            }
        }
        val hiddenPassword = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        etPinCode.inputType = hiddenPassword

        etPinCode.disableContextMenu()
        etPinCode.disableAutofill()
    }

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
                        UserCodeType.AccessCode -> String.format(
                            TangemSdkError.WrongAccessCode().localizedDescription(mainView.context),
                            mainView.context.getString(R.string.pin1),
                        )
                        UserCodeType.Passcode -> String.format(
                            TangemSdkError.WrongPasscode().localizedDescription(mainView.context),
                            mainView.context.getString(R.string.pin2),
                        )
                    }
                }

                etPinCode.setText("")
                postUI(msTime = 1000) { etPinCode.showSoftKeyboard() }

                btnContinue.setOnClickListener {
                    val pin = etPinCode.text.toString()
                    if (pin.isEmpty()) {
                        tilPinCode.error = mainView.context.getString(R.string.pin_enter, code)
                    } else {
                        tilPinCode.error = null
                        tilPinCode.isErrorEnabled = false
                        mainView.requestFocus()
                        etPinCode.hideSoftKeyboard()
                        postUI(msTime = 250) {
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
                    postUI(msTime = 250) {
                        onContinue?.invoke(CompletionResult.Failure(TangemSdkError.UserForgotTheCode()))
                    }
                }
            }
            else -> Unit
        }
    }
}

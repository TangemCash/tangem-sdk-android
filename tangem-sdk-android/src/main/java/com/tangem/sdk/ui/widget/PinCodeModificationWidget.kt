package com.tangem.sdk.ui.widget

import android.text.Editable
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.os.postDelayed
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.tangem.common.UserCodeType
import com.tangem.sdk.R
import com.tangem.sdk.SessionViewDelegateState
import com.tangem.sdk.extensions.hideSoftKeyboard
import com.tangem.sdk.extensions.showSoftKeyboard
import com.tangem.sdk.postUI
import com.tangem.sdk.ui.common.disableContextMenu
import com.tangem.sdk.ui.common.setupImeActionDone

/**
 * Created by Anton Zhilenkov on 05/08/2020.
 */
class PinCodeModificationWidget(
    mainView: View,
    private var mode: Mode,
) : BaseSessionDelegateStateWidget(mainView) {

    var onSave: ((String) -> Unit)? = null

    private val tvScreenTitle = mainView.findViewById<TextView>(R.id.tvScreenTitle)

    private val tilPinCode = mainView.findViewById<TextInputLayout>(R.id.tilPinCode)
    private val tilPinCodeConfirm = mainView.findViewById<TextInputLayout>(R.id.tilPinCodeConfirm)

    private val etPinCode = tilPinCode.findViewById<TextInputEditText>(R.id.etPinCode)
    private val etPinCodeConfirm = tilPinCodeConfirm.findViewById<TextInputEditText>(R.id.etPinCode)

    private val btnSave = mainView.findViewById<Button>(R.id.btnSaveCode)

    private var userCodeType: UserCodeType = UserCodeType.AccessCode
    private val pinName: String
        get() = when (userCodeType) {
            UserCodeType.AccessCode -> getString(R.string.pin1)
            UserCodeType.Passcode -> getString(R.string.pin2)
        }

    init {
        setStateByMode()
        setupInnerLogic()

        etPinCodeConfirm.setupImeActionDone(btnSave::performClick)

        etPinCode.disableContextMenu()
        etPinCodeConfirm.disableContextMenu()
    }

    override fun setState(params: SessionViewDelegateState) {
        if (params is SessionViewDelegateState.PinChangeRequested) {
            userCodeType = params.type
            clearFields()
            setStateByMode()
            postUI(msTime = 1000) { etPinCode.showSoftKeyboard() }
        }
    }

    private fun clearFields() {
        etPinCode.setText("")
        etPinCodeConfirm.setText("")
    }

    private fun setStateByMode() {
        val nameOfPin = pinName

        tilPinCode.hint = nameOfPin
        tilPinCodeConfirm.hint = getFormattedString(R.string.pin_set_code_confirm_format, nameOfPin)
        btnSave.text = getString(R.string.common_continue)

        tvScreenTitle.text = getFormattedString(
            id = when (mode) {
                Mode.SET -> R.string.pin_set_code_format
                Mode.RESET -> R.string.pin_change_new_code_format
            },
            name = nameOfPin,
        )
    }

    private fun setupInnerLogic() {
        etPinCode.setErrorStateVisibilityWithDebounce()
        etPinCodeConfirm.setErrorStateVisibilityWithDebounce()

        btnSave.isEnabled = false
        btnSave.setOnClickListener {
            mainView.requestFocus()
            mainView.hideSoftKeyboard()
            onSave?.let { onSaveClicked ->
                postUI(msTime = 250) {
                    onSaveClicked(requireNotNull(etPinCodeConfirm.text?.toString()))
                }
            }
        }
    }

    private fun TextInputEditText.setErrorStateVisibilityWithDebounce() {
        debounce(INPUT_FIELD_DEBOUNCE_DELAY) {
            setErrorStateVisibility(getInputFieldsState())
        }
    }

    private fun TextInputEditText.debounce(delay: Long, action: (Editable?) -> Unit) {
        doAfterTextChanged { text ->
            handler?.let {
                var counter = getTag(id) as? Int ?: 0
                it.removeCallbacksAndMessages(counter)
                it.postDelayed(delay, ++counter) { action(text) }
                setTag(id, counter)
            }
        }
    }

    private fun setErrorStateVisibility(state: CheckCodesState) {
        val errorMessage = when (state) {
            CheckCodesState.TOO_SHORT -> getFormattedString(
                R.string.error_pin_too_short_format,
                pinName,
                PIN_CODE_MIN_LENGTH,
            )
            CheckCodesState.NOT_MATCH -> getString(R.string.pin_confirm_error_format)
            else -> ""
        }

        TransitionManager.beginDelayedTransition(mainView as ViewGroup, AutoTransition())
        when (state) {
            CheckCodesState.TOO_SHORT, CheckCodesState.NOT_MATCH -> {
                tilPinCodeConfirm.error = errorMessage
                btnSave.isEnabled = false
            }
            CheckCodesState.MATCH -> {
                tilPinCodeConfirm.error = null
                tilPinCodeConfirm.isErrorEnabled = false
                btnSave.isEnabled = true
            }
            else -> {
                tilPinCodeConfirm.error = null
                tilPinCodeConfirm.isErrorEnabled = false
            }
        }
    }

    private fun getInputFieldsState(): CheckCodesState {
        val code1 = etPinCode.text?.toString() ?: ""
        val code2 = etPinCodeConfirm.text?.toString() ?: ""

        if (code1.isEmpty() || code2.isEmpty()) return CheckCodesState.UNDEFINED
        if (code1.length < PIN_CODE_MIN_LENGTH || code2.length < PIN_CODE_MIN_LENGTH) {
            return CheckCodesState.TOO_SHORT
        }

        return if (code1 == code2) CheckCodesState.MATCH else CheckCodesState.NOT_MATCH
    }

    enum class Mode {
        SET, RESET
    }

    private enum class CheckCodesState {
        NOT_MATCH,
        UNDEFINED,
        TOO_SHORT,
        MATCH,
    }

    private companion object {
        private const val INPUT_FIELD_DEBOUNCE_DELAY = 400L
        private const val PIN_CODE_MIN_LENGTH = 4
    }
}

package com.tangem.tangem_sdk_new.ui.widget

import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.os.postDelayed
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.tangem.common.UserCodeType
import com.tangem.tangem_sdk_new.R
import com.tangem.tangem_sdk_new.SessionViewDelegateState
import com.tangem.tangem_sdk_new.extensions.hideSoftKeyboard
import com.tangem.tangem_sdk_new.extensions.showSoftKeyboard
import com.tangem.tangem_sdk_new.postUI

/**
 * Created by Anton Zhilenkov on 05/08/2020.
 */
class PinCodeModificationWidget(
    mainView: View,
    private var mode: Mode
) : BaseSessionDelegateStateWidget(mainView) {

    var onSave: ((String) -> Unit)? = null

    private val tvScreenTitle: TextView = mainView.findViewById(R.id.tvScreenTitle)

    private val tilPinCode: TextInputLayout = mainView.findViewById(R.id.tilPinCode)
    private val tilNewPinCode: TextInputLayout = mainView.findViewById(R.id.tilNewPinCode)
    private val tilPinCodeConfirm: TextInputLayout = mainView.findViewById(R.id.tilPinCodeConfirm)

    private val etPinCode: TextInputEditText = mainView.findViewById(R.id.etPinCode)
    private val etNewPinCode: TextInputEditText = mainView.findViewById(R.id.etNewPinCode)
    private val etPinCodeConfirm: TextInputEditText = mainView.findViewById(R.id.etPinCodeConfirm)

    private val btnSave: Button = mainView.findViewById(R.id.btnSaveCode)

    private var isPasswordEnabled = true
    private var userCodeType: UserCodeType = UserCodeType.AccessCode
    private val pinName: String
        get() = when (userCodeType) {
            UserCodeType.AccessCode -> getString(R.string.pin1)
            UserCodeType.Passcode -> getString(R.string.pin2)
        }

    init {
        setStateByMode()
        setupInnerLogic()
    }

    override fun setState(params: SessionViewDelegateState) {
        when (params) {
            is SessionViewDelegateState.PinChangeRequested -> {
                userCodeType = params.type
                setStateByMode()
                postUI(1000) { etPinCode.showSoftKeyboard() }
            }
        }
    }

    fun switchModeTo(mode: Mode) {
        this.mode = mode
        setStateByMode()
    }

    private fun setStateByMode() {
        val nameOfPin = pinName
        when (mode) {
            Mode.SET -> {
                tvScreenTitle.text = getFormattedString(R.string.pin_set_code_format, nameOfPin)
                tilPinCode.hint = nameOfPin
                tilNewPinCode.visibility = View.GONE
                tilPinCodeConfirm.hint = getFormattedString(R.string.pin_set_code_confirm_format, nameOfPin)
                btnSave.text = getString(R.string.common_save)
            }
            Mode.CHANGE -> {
                tvScreenTitle.text = getFormattedString(R.string.pin_change_code_format, nameOfPin)
                tilPinCode.hint = getFormattedString(R.string.pin_change_current_code_format, nameOfPin)
                tilNewPinCode.hint = getFormattedString(R.string.pin_change_new_code_format, nameOfPin)
                tilPinCodeConfirm.hint = getFormattedString(R.string.pin_change_new_code_confirm_format, nameOfPin)
                btnSave.text = getString(R.string.common_save)
            }
            Mode.RESET -> {
                tvScreenTitle.text = getFormattedString(R.string.pin_change_new_code_format, nameOfPin)
                tilPinCode.hint = nameOfPin
                etPinCode.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                tilNewPinCode.visibility = View.GONE
                tilPinCodeConfirm.hint = getFormattedString(R.string.pin_set_code_confirm_format, nameOfPin)
                etPinCodeConfirm.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                btnSave.text = getString(R.string.common_continue)
            }
        }
    }

    private fun setupInnerLogic() {
        getEtForCodeChecks().debounce(INPUT_FIELD_DEBOUNCE_DELAY) {
            setErrorStateVisibility(getInputFieldsState())
        }
        etPinCodeConfirm.debounce(INPUT_FIELD_DEBOUNCE_DELAY) {
            setErrorStateVisibility(getInputFieldsState())
        }

        tilPinCode.setEndIconOnClickListener { onPasswordToggleClicked() }
        tilPinCodeConfirm.setEndIconOnClickListener { onPasswordToggleClicked() }

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

    private fun TextInputEditText.debounce(delay: Long, action: (Editable?) -> Unit) {
        doAfterTextChanged { text ->
            var counter = getTag(id) as? Int ?: 0
            handler.removeCallbacksAndMessages(counter)
            handler.postDelayed(delay, ++counter) { action(text) }
            setTag(id, counter)
        }
    }

    private fun getEtForCodeChecks(): TextInputEditText =
        if (mode == Mode.CHANGE) etNewPinCode else etPinCode

    private fun setErrorStateVisibility(state: CheckCodesState) {
        val errorMessage = when (state) {
            CheckCodesState.TOO_SHORT -> getFormattedString(
                R.string.error_pin_too_short_format,
                pinName,
                PIN_CODE_MIN_LENGTH
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
        val code1 = getEtForCodeChecks().text?.toString() ?: ""
        val code2 = etPinCodeConfirm.text?.toString() ?: ""

        if (code1.isEmpty() || code2.isEmpty()) return CheckCodesState.UNDEFINED
        if (code1.length < PIN_CODE_MIN_LENGTH || code2.length < PIN_CODE_MIN_LENGTH)
            return CheckCodesState.TOO_SHORT

        return if (code1 == code2) CheckCodesState.MATCH else CheckCodesState.NOT_MATCH
    }

    private fun onPasswordToggleClicked() {
        isPasswordEnabled = !isPasswordEnabled
        getEtForCodeChecks().togglePasswordInputType()
        etPinCodeConfirm.togglePasswordInputType()
    }

    private fun TextInputEditText.togglePasswordInputType() {
        val hiddenPassword = (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        inputType = if (isPasswordEnabled) {
            hiddenPassword
        } else {
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }
        setSelection(text?.length ?: 0)
    }

    enum class CheckCodesState(val isSaveButtonEnabled: Boolean = false) {
        NOT_MATCH(isSaveButtonEnabled = true),
        UNDEFINED,
        TOO_SHORT,
        MATCH(isSaveButtonEnabled = true)
    }

    enum class Mode {
        SET, CHANGE, RESET
    }

    companion object {
        private const val INPUT_FIELD_DEBOUNCE_DELAY = 400L
        private const val PIN_CODE_MIN_LENGTH = 4
    }
}

class DebounceAfterTextChanged(
    val view: EditText,
    var debounce: Long = 300,
    var afterTextChanged: ((Editable?) -> Unit)? = null
) {

    var textWatcher: TextWatcher? = null
        private set

    private val handler = Handler(Looper.getMainLooper())
    private val runnable = Runnable {
        afterTextChanged?.invoke(editable)
    }

    private var editable: Editable? = null

    init {
        textWatcher = view.addTextChangedListener(
            afterTextChanged = {
                editable = it
                handler.removeCallbacks(runnable)
                handler.postDelayed(runnable, debounce)
            }
        )
    }

    fun removeWatcher() {
        view.removeTextChangedListener(textWatcher)
    }
}

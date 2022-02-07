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
import androidx.core.widget.addTextChangedListener
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

    init {
        modifyUiByMode()
        attachCodesCheck(getEtForCodeChecks())
        attachCodesCheck(etPinCodeConfirm)
//        btnSave.isEnabled = false

        tilPinCode.setEndIconOnClickListener {
            isPasswordEnabled = !isPasswordEnabled
            togglePasswordInputType(etPinCode)
            togglePasswordInputType(etNewPinCode)
            togglePasswordInputType(etPinCodeConfirm)
        }

        btnSave.setOnClickListener {
            val state = checkCodes()
//            btnSave.isEnabled = state == MATCH
            updateErrorsVisibility(state == NOT_MATCH || state == UNDEFINED)
            if (state == MATCH) {
                mainView.requestFocus()
                mainView.hideSoftKeyboard()
                postUI(250) { onSave?.invoke(etPinCodeConfirm.text?.toString() ?: "") }
            }
        }
    }

    override fun setState(params: SessionViewDelegateState) {
        when (params) {
            is SessionViewDelegateState.PinChangeRequested -> {
                userCodeType = params.type
                resetPinCodes()
                modifyUiByMode()
                postUI(1000) { etPinCode.showSoftKeyboard() }
            }
        }
    }

    fun switchModeTo(mode: Mode) {
        this.mode = mode
        modifyUiByMode()
    }

    private fun modifyUiByMode() {
        val nameOfPin = when (userCodeType) {
            UserCodeType.AccessCode -> getString(R.string.pin1)
            UserCodeType.Passcode -> getString(R.string.pin2)
        }
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

    private fun resetPinCodes() {
        etPinCode.setText("")
        etNewPinCode.setText("")
        etPinCodeConfirm.setText("")
    }

    private fun attachCodesCheck(et: TextInputEditText) {
        DebounceAfterTextChanged(et) {
//            val state = checkCodes()
//            btnSave.isEnabled = state == MATCH
//            updateErrorsVisibility(state == NOT_MATCH)
        }
    }

    private fun getTilForCodeChecks(): TextInputLayout =
        if (mode == Mode.CHANGE) tilNewPinCode else tilPinCode

    private fun getEtForCodeChecks(): TextInputEditText =
        if (mode == Mode.CHANGE) etNewPinCode else etPinCode

    private fun checkCodes(): Int {
        val code1 = getEtForCodeChecks().text?.toString() ?: ""
        val code2 = etPinCodeConfirm.text?.toString() ?: ""

        if (code1.isEmpty() || code2.isEmpty()) return UNDEFINED

        return if (code1 == code2) MATCH else NOT_MATCH
    }

    private fun updateErrorsVisibility(errorIsVisible: Boolean) {
        val til1 = getTilForCodeChecks()
        val til2 = tilPinCodeConfirm

        TransitionManager.beginDelayedTransition(mainView as ViewGroup, AutoTransition())
        if (errorIsVisible) {
            val errorMessage = getString(R.string.pin_confirm_error_format)
            til1.error = errorMessage
            til2.error = errorMessage
        } else {
            til1.error = null
            til2.error = null
            til1.isErrorEnabled = false
            til2.isErrorEnabled = false
        }
    }

    private fun togglePasswordInputType(et: TextInputEditText) {
        et.inputType = if (isPasswordEnabled) 129 else InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        if (et.isFocused) et.setSelection(et.text?.length ?: 0)
    }

    companion object {
        private val MATCH = 1
        private val NOT_MATCH = 0
        private val UNDEFINED = -1
    }

    enum class Mode {
        SET, CHANGE, RESET
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
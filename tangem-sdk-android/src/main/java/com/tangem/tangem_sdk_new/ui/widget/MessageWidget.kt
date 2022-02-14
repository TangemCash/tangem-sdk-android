package com.tangem.tangem_sdk_new.ui.widget

import android.view.View
import android.widget.TextView
import com.tangem.Message
import com.tangem.WrongValueType
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.tangem_sdk_new.R
import com.tangem.tangem_sdk_new.SessionViewDelegateState
import com.tangem.tangem_sdk_new.extensions.localizedDescription

/**
 * Created by Anton Zhilenkov on 05/08/2020.
 */
class MessageWidget(mainView: View) : BaseSessionDelegateStateWidget(mainView) {

    private val tvTaskTitle: TextView = mainView.findViewById(R.id.tvTaskTitle)
    private val tvTaskMessage: TextView = mainView.findViewById(R.id.tvTaskMessage)

    private var initialMessage: Message? = null
    private var externalMessage: Message? = null

    override fun setState(params: SessionViewDelegateState) {
        val message = getMessage(params)
        when (params) {
            is SessionViewDelegateState.Ready -> {
                setText(tvTaskTitle, message?.header, R.string.view_delegate_scan)
                setText(tvTaskMessage, message?.body, R.string.view_delegate_scan_description)
            }
            is SessionViewDelegateState.Success -> {
                setText(tvTaskTitle, message?.header, R.string.common_success)
                setText(tvTaskMessage, message?.body ?: "")
            }
            is SessionViewDelegateState.Error -> {
                setText(tvTaskTitle, null, R.string.common_error)

                val errorMessage = getErrorString(params.error)
                val formattedErrorMessage = mainView.context.getString(
                    R.string.error_message,
                    params.error.code.toString(),
                    errorMessage
                )
                setText(tvTaskMessage, formattedErrorMessage)
            }
            is SessionViewDelegateState.SecurityDelay -> {
                setText(tvTaskTitle, message?.header, R.string.view_delegate_security_delay)
                setText(tvTaskMessage, message?.body, R.string.view_delegate_security_delay_description)
            }
            is SessionViewDelegateState.Delay -> {
                setText(tvTaskTitle, message?.header, R.string.view_delegate_delay)
                setText(tvTaskMessage, message?.body, R.string.view_delegate_security_delay_description)
            }
            is SessionViewDelegateState.PinRequested -> {
            }
            is SessionViewDelegateState.PinChangeRequested -> {
            }
            is SessionViewDelegateState.TagLost -> {
                setText(tvTaskTitle, message?.header, R.string.view_delegate_scan)
                setText(tvTaskMessage, message?.body, R.string.view_delegate_scan_description)
            }
            is SessionViewDelegateState.TagConnected -> {
            }
            is SessionViewDelegateState.WrongCard -> {
                val description = when (params.wrongValueType) {
                    WrongValueType.CardId -> getString(R.string.error_wrong_card_number)
                    WrongValueType.CardType -> getString(R.string.error_wrong_card_type)
                }

                setText(tvTaskTitle, null, R.string.common_error)
                val bodyMessage = mainView.context.getString(
                    R.string.error_message,
                    getString(R.string.error_wrong_card),
                    description
                )

                setText(tvTaskMessage, bodyMessage)
            }
        }
    }

    fun setInitialMessage(message: Message?) {
        this.initialMessage = message
    }

    fun setMessage(message: Message?) {
        this.externalMessage = message
        setText(tvTaskTitle, message?.header)
        setText(tvTaskMessage, message?.body)
    }

    private fun getMessage(params: SessionViewDelegateState): Message? {
        return when (params) {
            is SessionViewDelegateState.Ready -> initialMessage
            is SessionViewDelegateState.TagLost -> initialMessage
            is SessionViewDelegateState.Success -> params.message
            else -> externalMessage
        }
    }

    private fun getErrorString(error: TangemError): String {
        return if (error is TangemSdkError) {
            error.localizedDescription(mainView.context)
        } else {
            val localizedMessage = error.messageResId?.let { mainView.context.getString(it) }
            localizedMessage ?: error.customMessage
        }
    }

    private fun setText(tv: TextView, text: String?, id: Int? = null) {
        text?.let {
            tv.text = it
            return
        }
        id?.let { tv.text = getString(it) }
    }
}

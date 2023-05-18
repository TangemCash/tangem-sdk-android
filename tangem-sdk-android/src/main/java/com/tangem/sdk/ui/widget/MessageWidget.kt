package com.tangem.sdk.ui.widget

import android.view.View
import android.widget.TextView
import com.tangem.LocatorMessage
import com.tangem.ViewDelegateMessage
import com.tangem.WrongValueType
import com.tangem.common.StringsLocator
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.sdk.AndroidStringLocator
import com.tangem.sdk.R
import com.tangem.sdk.SessionViewDelegateState
import com.tangem.sdk.extensions.localizedDescription

/**
 * Created by Anton Zhilenkov on 05/08/2020.
 */
class MessageWidget(mainView: View) : BaseSessionDelegateStateWidget(mainView) {

    private val stringLocator: StringsLocator = AndroidStringLocator(mainView.context)

    private val tvTaskTitle: TextView = mainView.findViewById(R.id.tvTaskTitle)
    private val tvTaskMessage: TextView = mainView.findViewById(R.id.tvTaskMessage)

    private var initialMessage: ViewDelegateMessage? = null
    private var externalMessage: ViewDelegateMessage? = null

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
                setText(tvTaskMessage, errorMessage)
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
                setText(tv = tvTaskTitle, text = null, id = R.string.common_error)

                val description = when (params.wrongValueType) {
                    WrongValueType.CardId -> getString(R.string.error_wrong_card_number)
                    WrongValueType.CardType -> getString(R.string.error_wrong_card_type)
                }
                setText(
                    tv = tvTaskMessage,
                    text = "${getString(R.string.error_wrong_card)}: $description",
                )
            }
        }
    }

    fun setInitialMessage(message: ViewDelegateMessage?) {
        this.initialMessage = message
    }

    fun setMessage(message: ViewDelegateMessage?) {
        this.externalMessage = message
        setText(tvTaskTitle, message?.header)
        setText(tvTaskMessage, message?.body)
    }

    private fun getMessage(params: SessionViewDelegateState): ViewDelegateMessage? {
        return when (params) {
            is SessionViewDelegateState.Ready -> initialMessage
            is SessionViewDelegateState.TagLost -> initialMessage
            is SessionViewDelegateState.Success -> params.message
            else -> externalMessage
        }.apply {
            if (this is LocatorMessage) this.fetchMessages(stringLocator)
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

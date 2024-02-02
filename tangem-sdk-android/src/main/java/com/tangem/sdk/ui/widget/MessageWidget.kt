package com.tangem.sdk.ui.widget

import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
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
                setTaskBlock(
                    message = message,
                    titleId = R.string.view_delegate_scan,
                    messageId = R.string.view_delegate_scan_description,
                )
            }

            is SessionViewDelegateState.Success -> {
                setTaskBlock(message = message, messageText = "", titleId = R.string.common_success)
            }

            is SessionViewDelegateState.Error -> {
                setTaskBlock(
                    message = message,
                    titleText = null,
                    messageText = getErrorString(params.error),
                    titleId = R.string.common_error,
                )
            }

            is SessionViewDelegateState.SecurityDelay -> {
                setTaskBlock(
                    message = message,
                    titleId = R.string.view_delegate_security_delay,
                    messageId = R.string.view_delegate_security_delay_description,
                )
            }

            is SessionViewDelegateState.Delay -> {
                setTaskBlock(
                    message = message,
                    titleId = R.string.view_delegate_delay,
                    messageId = R.string.view_delegate_security_delay_description,
                )
            }

            is SessionViewDelegateState.TagLost -> {
                setTaskBlock(
                    message = message,
                    titleId = R.string.view_delegate_scan,
                    messageId = R.string.view_delegate_scan_description,
                )
            }

            is SessionViewDelegateState.WrongCard -> {
                val description = when (params.wrongValueType) {
                    is WrongValueType.CardId -> {
                        val value = params.wrongValueType.value
                        if (value != null) {
                            getFormattedString(R.string.error_wrong_card_number_with_card_id, value)
                        } else {
                            getString(R.string.error_wrong_card_number_without_card_id)
                        }
                    }

                    is WrongValueType.CardType -> {
                        getString(R.string.error_wrong_card_number_without_card_id)
                    }
                }

                setTaskBlock(
                    message = message,
                    titleText = null,
                    messageText = description,
                    titleId = R.string.common_error,
                )
            }

            else -> Unit
        }
    }

    fun setInitialMessage(message: ViewDelegateMessage?) {
        initialMessage = message
    }

    fun setMessage(message: ViewDelegateMessage?) {
        externalMessage = message
        if (message is LocatorMessage) message.fetchMessages(locator = stringLocator)
        setTaskBlock(message)
    }

    private fun getMessage(params: SessionViewDelegateState): ViewDelegateMessage? {
        return when (params) {
            is SessionViewDelegateState.Ready -> initialMessage
            is SessionViewDelegateState.TagLost -> initialMessage
            is SessionViewDelegateState.Success -> params.message
            else -> externalMessage
        }.apply {
            if (this is LocatorMessage) fetchMessages(locator = stringLocator)
        }
    }

    private fun setTaskBlock(
        message: ViewDelegateMessage?,
        titleText: String? = message?.header,
        messageText: String? = message?.body,
        @StringRes titleId: Int? = null,
        @StringRes messageId: Int? = null,
    ) {
        setText(tv = tvTaskTitle, text = titleText, id = titleId)
        setText(tv = tvTaskMessage, text = messageText, id = messageId)
    }

    private fun getErrorString(error: TangemError): String {
        return if (error is TangemSdkError) {
            error.localizedDescription(mainView.context)
        } else if (error.messageResId != null) {
            mainView.context.getString(requireNotNull(error.messageResId))
        } else {
            error.customMessage
        }
    }

    private fun setText(tv: TextView, text: String?, id: Int? = null) {
        if (text != null) {
            tv.text = text
        } else if (id != null) {
            tv.text = getString(id)
        }
    }
}

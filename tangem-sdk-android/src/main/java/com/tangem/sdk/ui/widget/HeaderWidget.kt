package com.tangem.sdk.ui.widget

import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.tangem.common.extensions.VoidCallback
import com.tangem.sdk.R
import com.tangem.sdk.SessionViewDelegateState
import com.tangem.sdk.extensions.hide
import com.tangem.sdk.extensions.hideSoftKeyboard
import com.tangem.sdk.extensions.show

/**
 * Created by Anton Zhilenkov on 09/08/2020.
 */
class HeaderWidget(mainView: View) : BaseSessionDelegateStateWidget(mainView) {

    private val tvCard = mainView.findViewById<TextView>(R.id.tvCard)
    private val tvCardId = mainView.findViewById<TextView>(R.id.tvCardId)
    private val imvClose = mainView.findViewById<ImageView>(R.id.imvClose)
    private val btnHowTo = mainView.findViewById<Button>(R.id.btnHowTo)

    var onClose: VoidCallback? = null
    var onHowTo: VoidCallback? = null

    var cardId: String? = null
        private set

    var howToIsEnabled: Boolean = false
        set(value) {
            field = value
            btnHowTo.post { btnHowTo.show(value) }
        }

    init {
        imvClose.setOnClickListener {
            mainView.hideSoftKeyboard()
            mainView.requestFocus()
            onClose?.invoke()
        }
        btnHowTo.setOnClickListener { onHowTo?.invoke() }
    }

    override fun showWidget(show: Boolean, withAnimation: Boolean) = Unit

    override fun setState(params: SessionViewDelegateState) {
        when (params) {
            is SessionViewDelegateState.Ready -> {
                cardId = params.cardId
                imvClose.hide()
                btnHowTo.isEnabled = true
                btnHowTo.show()
            }
            is SessionViewDelegateState.PinChangeRequested -> {
                cardId = params.cardId
                btnHowTo.hide()
                imvClose.show()
            }
            is SessionViewDelegateState.PinRequested -> {
                cardId = params.cardId
                btnHowTo.hide()
                imvClose.show(true)
            }
            is SessionViewDelegateState.TagLost -> btnHowTo.isEnabled = true
            is SessionViewDelegateState.TagConnected, is SessionViewDelegateState.Error,
            is SessionViewDelegateState.WrongCard,
            -> btnHowTo.isEnabled = false
            is SessionViewDelegateState.ResetCodes -> {
                cardId = params.cardId
                btnHowTo.hide()
                imvClose.show()
            }
            else -> {
                imvClose.hide()
                btnHowTo.show()
            }
        }
        setCardId()
    }

    private fun setCardId() {
        val scannedCardId = cardId
        if (scannedCardId == null) {
            tvCard.text = ""
        } else {
            tvCard.show()
            tvCard.text = getString(R.string.view_delegate_header_card)
            tvCardId.show()
            tvCardId.text = scannedCardId
        }
    }
}

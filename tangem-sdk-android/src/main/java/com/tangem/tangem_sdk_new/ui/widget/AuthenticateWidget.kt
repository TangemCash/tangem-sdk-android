package com.tangem.tangem_sdk_new.ui.widget

import android.view.View
import com.tangem.tangem_sdk_new.SessionViewDelegateState

class AuthenticateWidget(
    mainView: View,
) : BaseSessionDelegateStateWidget(mainView) {
    var onAuthenticated: (() -> Unit)? = null
    var onCancelled: (() -> Unit)? = null

    override fun setState(params: SessionViewDelegateState) {
        /* no-op */
    }
}

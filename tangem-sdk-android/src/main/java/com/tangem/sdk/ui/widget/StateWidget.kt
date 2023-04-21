package com.tangem.sdk.ui.widget

import android.view.View
import com.tangem.sdk.SessionViewDelegateState
import com.tangem.sdk.extensions.show

interface StateConsumer<T> {
    fun setState(params: T)
}

interface StateWidget<T> : StateConsumer<T> {
    fun isVisible(): Boolean
    fun showWidget(show: Boolean, withAnimation: Boolean = true)

    /**
     * Calls if user initiate to dismiss dialog
     *
     */
    fun onBottomSheetDismiss()
}

abstract class BaseStateWidget<T>(protected val mainView: View) : StateWidget<T> {

    // callback only if user initiate to close dialog
    var onBottomSheetDismiss: (() -> Unit)? = null

    override fun isVisible(): Boolean = mainView.visibility == View.VISIBLE

    override fun showWidget(show: Boolean, withAnimation: Boolean) {
        mainView.show(show)
    }

    override fun onBottomSheetDismiss() {
        onBottomSheetDismiss?.invoke()
        onBottomSheetDismiss = null
    }

    protected fun getString(id: Int): String = mainView.context.getString(id)

    protected fun getFormattedString(id: Int, name: String): String = mainView.context.getString(id, name)

    protected fun getFormattedString(id: Int, vararg args: Any): String = mainView.context.getString(id, *args)
}

@Suppress("UnnecessaryAbstractClass")
abstract class BaseSessionDelegateStateWidget(mainView: View) : BaseStateWidget<SessionViewDelegateState>(mainView)

package com.tangem.sdk

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.tangem.common.StringsLocator
import com.tangem.common.core.TangemError
import com.tangem.common.extensions.VoidCallback
import com.tangem.operations.resetcode.ResetCodesViewDelegate
import com.tangem.operations.resetcode.ResetCodesViewState
import com.tangem.sdk.extensions.sdkThemeContext
import com.tangem.sdk.ui.ResetCodesDialog

class AndroidResetCodesViewDelegate(val activity: Activity) : ResetCodesViewDelegate {

    override var stopSessionCallback: VoidCallback = {}

    override val stringsLocator: StringsLocator = AndroidStringLocator(activity)

    private var resetCodesDialog: ResetCodesDialog? = null

    override fun setState(state: ResetCodesViewState) {
        postUI {
            if (resetCodesDialog == null) createResetCodesDialog()
            resetCodesDialog?.showState(state)
        }
    }

    override fun hide(callback: VoidCallback) {
        postUI(msTime = 400) {
            resetCodesDialog?.cancel()
            callback()
        }
    }

    override fun showError(error: TangemError) {
        postUI {
            AlertDialog.Builder(activity).apply {
                setTitle(R.string.common_error)
                setMessage(error.customMessage)
                setPositiveButton(R.string.common_ok) { _, _ ->
                }
                setOnDismissListener {
                    hide {}
                }
            }.create().show()
        }
    }

    override fun showAlert(title: String, message: String, onContinue: VoidCallback) {
        postUI {
            AlertDialog.Builder(activity).apply {
                setTitle(title)
                setMessage(message)
                setPositiveButton(R.string.common_ok) { _, _ ->
                }
                setOnDismissListener {
                    hide(onContinue)
                }
            }.create().show()
        }
    }

    private fun createResetCodesDialog() {
        resetCodesDialog = ResetCodesDialog(activity.sdkThemeContext()).apply {
            setOwnerActivity(activity)
            dismissWithAnimation = true
            create()
            setOnCancelListener {
                stopSessionCallback()
            }
        }
    }
}
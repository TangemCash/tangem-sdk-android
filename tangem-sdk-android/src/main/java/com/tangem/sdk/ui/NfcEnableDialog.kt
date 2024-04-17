package com.tangem.sdk.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tangem.sdk.R
import com.tangem.sdk.extensions.sdkThemeContext

class NfcEnableDialog {

    private var dialog: AlertDialog? = null

    fun show(activity: ComponentActivity) {
        val builder = MaterialAlertDialogBuilder(activity.sdkThemeContext())
        builder.setCancelable(false)
            .setIcon(R.drawable.ic_action_nfc_gray)
            .setTitle(R.string.dialog_nfc_enable_title)
            .setMessage(R.string.dialog_nfc_enable_text)
            .setPositiveButton(R.string.common_ok) { _, _ ->
                try {
                    if ((activity as? LifecycleOwner)?.lifecycle?.currentState == Lifecycle.State.STARTED) {
                        activity.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
                    }
                } catch (ex: ActivityNotFoundException) {
                    print(ex.toString())
                }
            }
            .setNegativeButton(R.string.common_cancel) { dialog, _ -> dialog.cancel() }
        dialog = builder.create()
        dialog?.show()
    }

    fun cancel() {
        dialog?.cancel()
    }
}

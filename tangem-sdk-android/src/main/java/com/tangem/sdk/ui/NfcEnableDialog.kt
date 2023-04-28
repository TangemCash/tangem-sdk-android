package com.tangem.sdk.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tangem.sdk.R
import com.tangem.sdk.extensions.sdkThemeContext

class NfcEnableDialog {

    private var dialog: AlertDialog? = null

    fun show(activity: Activity) {
        val builder = MaterialAlertDialogBuilder(activity.sdkThemeContext())
        builder.setCancelable(false)
            .setIcon(R.drawable.ic_action_nfc_gray)
            .setTitle(R.string.dialog_nfc_enable_title)
            .setMessage(R.string.dialog_nfc_enable_text)
            .setPositiveButton(R.string.common_ok) { _, _ ->
                try {
                    activity.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
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

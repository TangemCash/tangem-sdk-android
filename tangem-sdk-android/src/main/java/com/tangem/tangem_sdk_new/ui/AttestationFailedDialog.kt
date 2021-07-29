package com.tangem.tangem_sdk_new.ui

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tangem.common.extensions.VoidCallback
import com.tangem.tangem_sdk_new.R
import com.tangem.tangem_sdk_new.postUI

/**
 * Created by Anton Zhilenkov on 29/07/2021.
 */
class AttestationFailedDialog {
    companion object {

        fun didFail(context: Context, isDevCard: Boolean, positive: VoidCallback, negative: VoidCallback) {
            val title = context.getString(R.string.dialog_attestation_did_failed_title)
            val messageResId = when {
                isDevCard -> R.string.dialog_attestation_did_failed_message_dev_card
                else -> R.string.dialog_attestation_did_failed_message
            }
            val message = context.getString(messageResId)
            val dialog = createDialog(context, title, message, positive, negative)
            postUI { dialog.show() }
        }

        fun completedOffline(context: Context, positive: VoidCallback, negative: VoidCallback, retry: VoidCallback) {
            val title = context.getString(R.string.dialog_attestation_completed_offline_title)
            val message = context.getString(R.string.dialog_attestation_completed_offline_message)
            val dialog = createDialog(context, title, message, positive, negative, retry)
            postUI { dialog.show() }
        }

        fun completedWithWarnings(context: Context, positive: VoidCallback) {
            val title = context.getString(R.string.dialog_attestation_completed_with_warnings_title)
            val message = context.getString(R.string.dialog_attestation_completed_with_warnings_message)
            val dialog = createDialog(context, title, message, positive)
            postUI { dialog.show() }
        }

        private fun createDialog(
            context: Context,
            title: String,
            message: String,
            positive: VoidCallback,
            negative: VoidCallback? = null,
            neutral: VoidCallback? = null
        ): AlertDialog.Builder {
            return MaterialAlertDialogBuilder(context).apply {
                setCancelable(false)
                setTitle(title)
                setMessage(message)
                setPositiveButton(R.string.common_ok) { dialog, wich -> positive() }
                negative?.let { setNegativeButton(R.string.common_cancel) { dialog, wich -> it() } }
                neutral?.let { setNeutralButton(R.string.common_retry) { dialog, wich -> it() } }
            }
        }
    }
}
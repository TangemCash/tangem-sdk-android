package com.tangem.sdk.ui

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tangem.common.extensions.VoidCallback
import com.tangem.sdk.R
import com.tangem.sdk.extensions.sdkThemeContext
import com.tangem.sdk.postUI

/**
 * Created by Anton Zhilenkov on 29/07/2021.
 */
object AttestationFailedDialog {

    fun didFail(context: Context, isDevCard: Boolean, positive: VoidCallback, negative: VoidCallback) {
        val title = context.getString(R.string.error_card_verification_failed)
        val messageResId = when {
            isDevCard -> R.string.attestation_failed_dev_card
            else -> R.string.attestation_failed_card
        }
        val message = context.getString(messageResId)
        val dialog = createDialog(context, title, message, positive, negative)
        postUI { dialog.show() }
    }

    fun completedOffline(context: Context, positive: VoidCallback, negative: VoidCallback, retry: VoidCallback) {
        val title = context.getString(R.string.attestation_online_failed_title)
        val message = context.getString(R.string.attestation_online_failed_body)
        val dialog = createDialog(context, title, message, positive, negative, retry)
        postUI { dialog.show() }
    }

    fun completedWithWarnings(context: Context, positive: VoidCallback) {
        val title = context.getString(R.string.common_warning)
        val message = context.getString(R.string.attestation_warning_attest_wallets)
        val dialog = createDialog(context, title, message, positive)
        postUI { dialog.show() }
    }

    private fun createDialog(
        context: Context,
        title: String,
        message: String,
        positive: VoidCallback,
        negative: VoidCallback? = null,
        neutral: VoidCallback? = null,
    ): AlertDialog.Builder {
        return MaterialAlertDialogBuilder(context.sdkThemeContext()).apply {
            setCancelable(false)
            setTitle(title)
            setMessage(message)
            setPositiveButton(R.string.common_understand) { _, _ -> positive() }
            negative?.let { setNegativeButton(R.string.common_cancel) { _, _ -> it() } }
            neutral?.let { setNeutralButton(R.string.common_retry) { _, _ -> it() } }
        }
    }
}

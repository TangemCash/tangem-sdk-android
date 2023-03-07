package com.tangem.demo.ui.extension

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.core.view.forEach
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.tangem.demo.GlobalLayoutStateHandler

/**
 * Created by Anton Zhilenkov on 08/08/2021.
 */
fun ChipGroup.fitChipsByGroupWidth() {
    val layoutStateHandler = GlobalLayoutStateHandler(this)
    layoutStateHandler.onStateChanged = stateHandler@{
        if (it.childCount < 2) {
            layoutStateHandler.detach()
            return@stateHandler
        }

        val spacingBetweenViews = it.chipSpacingHorizontal * (it.childCount - 1)
        val width = (it.width - spacingBetweenViews) / it.childCount
        it.forEach { chip -> (chip as? Chip)?.width = width }
        layoutStateHandler.detach()
    }
}

fun Context.copyToClipboard(value: Any, label: String = "") {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return

    val clip: ClipData = ClipData.newPlainText(label, value.toString())
    clipboard.setPrimaryClip(clip)
}

fun Context.getFromClipboard(default: CharSequence? = null): CharSequence? {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        ?: return default
    val clipData = clipboard.primaryClip ?: return default
    if (clipData.itemCount == 0) return default

    return clipData.getItemAt(0).text
}
package com.tangem.sdk.ui.common

import android.os.Build
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.google.android.material.textfield.TextInputEditText
import com.tangem.sdk.R

internal fun TextInputEditText.disableContextMenu() {
    setOnCreateContextMenuListener { _, _, _ -> }

    customInsertionActionModeCallback = EmptyActionModeCallback
    customSelectionActionModeCallback = EmptyActionModeCallback

    setTextIsSelectable(false)

    isLongClickable = false
    setOnLongClickListener { false }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        setTextSelectHandle(R.drawable.empty_text_selector)
        setTextSelectHandleLeft(R.drawable.empty_text_selector)
        setTextSelectHandleRight(R.drawable.empty_text_selector)
    }
}

internal fun TextInputEditText.disableAutofill() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        setAutofillHints(null)
        importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
    }
}

private object EmptyActionModeCallback : ActionMode.Callback {
    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean = false
    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false
    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean = false
    override fun onDestroyActionMode(mode: ActionMode?) = Unit
}

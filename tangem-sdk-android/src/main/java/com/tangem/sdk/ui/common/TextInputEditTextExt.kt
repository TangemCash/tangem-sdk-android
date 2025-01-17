package com.tangem.sdk.ui.common

import android.os.Build
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
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

internal fun TextInputEditText.setupImeActionDone(action: () -> Unit) {
    imeOptions = EditorInfo.IME_ACTION_DONE

    setOnEditorActionListener { _, actionId, _ ->
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            action()
            true
        } else {
            false
        }
    }
}

private object EmptyActionModeCallback : ActionMode.Callback {
    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean = false
    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false
    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean = false
    override fun onDestroyActionMode(mode: ActionMode?) = Unit
}

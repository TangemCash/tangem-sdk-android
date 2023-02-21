package com.tangem.tangem_demo.ui.extension

import android.widget.TextView

/**
 * Created by Anton Zhilenkov on 01/07/2022.
 */
fun TextView.setTextFromClipboard() {
    context.getFromClipboard()?.let { this.text = it }
}
package com.tangem.sdk.extensions

import android.content.Context
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.inputmethod.InputMethodManager
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat

internal fun View.show() {
    this.show(true)
}

internal fun View.hide() {
    this.show(false)
}

internal fun View.show(show: Boolean) {
    if (show) {
        if (visibility != View.VISIBLE) visibility = View.VISIBLE
    } else {
        if (visibility != View.GONE) visibility = View.GONE
    }
}

fun View.fadeOut(fadeOutDuration: Long, delayStart: Long = 0, onEnd: (() -> Unit)? = null) {
    animate().apply {
        alpha(0f)
        startDelay = delayStart
        duration = fadeOutDuration
        interpolator = AccelerateDecelerateInterpolator()
        withEndAction { onEnd?.invoke() }
    }.start()
}

fun View.fadeIn(fadeInDuration: Long, delayStart: Long = 0, onEnd: (() -> Unit)? = null) {
    animate().apply {
        this.alpha(1f)
        startDelay = delayStart
        duration = fadeInDuration
        interpolator = AccelerateInterpolator()
        withEndAction { onEnd?.invoke() }
    }.start()
}

fun View.dpToPx(dp: Float): Float = context.dpToPx(dp)
fun View.pxToDp(px: Float): Float = context.pxToDp(px)

fun View.showSoftKeyboard() {
    if (requestFocus()) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }
}

fun View.hideSoftKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(this.windowToken, 0)
}

fun View.parseColor(@ColorRes color: Int): Int {
    return context.parseColor(color)
}

fun Context.parseColor(@ColorRes color: Int): Int {
    return ContextCompat.getColor(this, color)
}

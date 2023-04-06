package com.tangem.demo.ui.extension

import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import androidx.transition.AutoTransition
import androidx.transition.Transition
import androidx.transition.TransitionManager

/**
 * Created by Anton Zhilenkov on 01/07/2022.
 */
fun ViewParent?.beginDelayedTransition(transition: Transition = AutoTransition()) {
    (this as? ViewGroup)?.beginDelayedTransition(transition)
}

fun ViewGroup.beginDelayedTransition(transition: Transition = AutoTransition()) {
    TransitionManager.beginDelayedTransition(this, transition)
}

fun View.beginDelayedTransition(transition: Transition = AutoTransition()) {
    (this as? ViewGroup)?.beginDelayedTransition(transition)
}

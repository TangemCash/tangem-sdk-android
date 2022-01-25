package com.tangem.tangem_sdk_new.ui.widget.leapfrogWidget

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator

/**
 * Created by Anton Zhilenkov on 07/10/2021.
 */

typealias ProgressListener = (Int) -> Unit

fun createProgressListener(duration: Long, listener: ProgressListener?): Animator {
    val valueAnimator = ValueAnimator.ofInt(0, 100)
    valueAnimator.duration = duration
    valueAnimator.addUpdateListener { listener?.invoke((it.animatedValue as? Int) ?: 0) }
    return valueAnimator
}

fun View.unfoldAnimation(animDuration: Long, properties: LeapViewProperties): AnimatorSet {
    val translate = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, properties.yTranslation)
    translate.duration = animDuration
    return AnimatorSet().apply { playTogether(translate) }
}

fun View.foldAnimation(animDuration: Long, properties: LeapViewProperties): AnimatorSet {
    val translate = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, properties.yTranslation)
    translate.duration = animDuration

    return AnimatorSet().apply {
        playSequentially(translate)
    }
}

fun View.leapAnimation(animDuration: Long, properties: LeapViewProperties, overLift: Float): AnimatorSet {
    val halfDuration = animDuration / 2

    val upTo = (height.toFloat() + overLift) * -1
    val translateUp = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, upTo)
    translateUp.duration = halfDuration
    translateUp.interpolator = LinearInterpolator()

    val scaleDuration = halfDuration - halfDuration / 5
    val scaleX = ObjectAnimator.ofFloat(this, View.SCALE_X, properties.scale)
    val scaleY = ObjectAnimator.ofFloat(this, View.SCALE_Y, properties.scale)
    val scale = AnimatorSet().apply {
        startDelay = scaleDuration
        duration = scaleDuration
        interpolator = LinearInterpolator()
        playTogether(scaleX, scaleY)
    }

    val elevation = elevationAnimator(properties.elevationStart, properties.elevationEnd, halfDuration, 100)
    val translateDown = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, properties.yTranslation)
    translateDown.startDelay = halfDuration
    translateDown.duration = halfDuration
    translateDown.interpolator = LinearInterpolator()

    return AnimatorSet().apply {
        playTogether(translateUp, scale, elevation, translateDown)
    }
}

fun View.leapBackAnimation(animDuration: Long, properties: LeapViewProperties, calculator: PropertyCalculator): AnimatorSet {
    val halfDuration = animDuration / 2

    val upTo = ((height).toFloat() - calculator.yTranslationFactor) * -1
    val translateUp = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, upTo)
    translateUp.duration = halfDuration
    translateUp.interpolator = LinearInterpolator()

    val scaleDuration = halfDuration - halfDuration / 5
    val scaleX = ObjectAnimator.ofFloat(this, View.SCALE_X, properties.scale)
    val scaleY = ObjectAnimator.ofFloat(this, View.SCALE_Y, properties.scale)
    val scale = AnimatorSet().apply {
        startDelay = scaleDuration
        duration = scaleDuration
        interpolator = LinearInterpolator()
        playTogether(scaleX, scaleY)
    }

    val elevation = elevationAnimator(properties.elevationStart, properties.elevationEnd, halfDuration, 100)
    val translateDown = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, properties.yTranslation)
    translateDown.startDelay = halfDuration
    translateDown.duration = halfDuration

    return AnimatorSet().apply {
        playTogether(translateUp, scale, elevation, translateDown)
    }
}

fun View.pullUpAnimation(leapDuration: Long, properties: LeapViewProperties): AnimatorSet {
    val pullDuration = leapDuration / 2
    val delayDuration = leapDuration / 2

    val translateUp = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, properties.yTranslation)
    val scaleX = ObjectAnimator.ofFloat(this, View.SCALE_X, properties.scale)
    val scaleY = ObjectAnimator.ofFloat(this, View.SCALE_Y, properties.scale)
    val elevation = elevationAnimator(properties.elevationStart, properties.elevationEnd)

    return AnimatorSet().apply {
        startDelay = delayDuration
        duration = pullDuration
        interpolator = DecelerateInterpolator()
        playTogether(translateUp, scaleX, scaleY, elevation)
    }
}

fun View.pullDownAnimation(leapDuration: Long, properties: LeapViewProperties): AnimatorSet {
    val pullDuration = leapDuration / 2
    val delayDuration = leapDuration / 4

    val translate = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, properties.yTranslation)
    val scaleX = ObjectAnimator.ofFloat(this, View.SCALE_X, properties.scale)
    val scaleY = ObjectAnimator.ofFloat(this, View.SCALE_Y, properties.scale)
    val elevation = elevationAnimator(properties.elevationStart, properties.elevationEnd)

    return AnimatorSet().apply {
        startDelay = delayDuration
        duration = pullDuration
        interpolator = DecelerateInterpolator()
        playTogether(translate, scaleX, scaleY, elevation)
    }
}

fun View.elevationAnimator(start: Float, end: Float, startDelay: Long? = null, duration: Long? = null): Animator {
    val elevationAnimator = ValueAnimator.ofFloat(start, end)
    elevationAnimator.addUpdateListener { this.elevation = (it.animatedValue as? Float) ?: start }
    startDelay?.let { elevationAnimator.startDelay = it }
    duration?.let { elevationAnimator.duration = it }
    return elevationAnimator
}
package com.tangem.sdk.ui.animation

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import com.tangem.common.extensions.VoidCallback

/**
 * Created by Anton Zhilenkov on 12/10/2020.
 */
class FlipAnimator(
    private val frontView: View,
    private val backView: View,
    val flipDuration: Long,
) {
    var onAnimationEnd: VoidCallback? = null

    var visibleSide = Side.FRONT
        private set

    var animationInProgress = false
        private set

    private var animatorSet: AnimatorSet? = null

    fun animate() {
        val toSide = if (visibleSide == Side.FRONT) Side.BACK else Side.FRONT
        animateToSide(toSide, flipDuration)
    }

    fun animateToSide(side: Side, duration: Long) {
        if (visibleSide == side || animationInProgress) return

        animatorSet = when (side) {
            Side.FRONT -> {
                visibleSide = Side.FRONT
                toFrontAnimator(duration)
            }
            Side.BACK -> {
                visibleSide = Side.BACK
                toBackAnimator(duration)
            }
        }
        animatorSet?.doOnStart { animationInProgress = true }
        animatorSet?.doOnEnd {
            animationInProgress = false
            onAnimationEnd?.invoke()
        }
        animatorSet?.start()
    }

    fun cancel() {
        onAnimationEnd = null
        animatorSet?.cancel()
        animatorSet = null
    }

    private fun toFrontAnimator(duration: Long): AnimatorSet {
        return AnimatorSet().apply {
            playTogether(
                frontView.flipEnd(duration),
                frontView.wiggleToRight(duration),
                backView.flipStart(duration),
                backView.wiggleToRight(duration)
            )
        }
    }

    private fun toBackAnimator(duration: Long): AnimatorSet {
        return AnimatorSet().apply {
            playTogether(
                frontView.flipStart(duration),
                frontView.wiggleToRight(duration),
                backView.flipEnd(duration),
                backView.wiggleToRight(duration)
            )
        }
    }
}

enum class Side {
    FRONT, BACK
}

@Suppress("MagicNumber")
private fun View.flipStart(duration: Long): AnimatorSet {
    val rotation = ObjectAnimator.ofFloat(this, View.ROTATION_Y, 0f, -180f)
    rotation.duration = duration
    val fadeIn = ObjectAnimator.ofFloat(this, View.ALPHA, 1f, 0f)
    fadeIn.duration = 0
    fadeIn.startDelay = duration / 2
    return AnimatorSet().apply { playTogether(rotation, fadeIn) }
}

@Suppress("MagicNumber")
private fun View.flipEnd(duration: Long): AnimatorSet {
    val rotation = ObjectAnimator.ofFloat(this, View.ROTATION_Y, 180f, 0f)
    rotation.duration = duration
    rotation.repeatMode = ValueAnimator.REVERSE

    val fadeOut = ObjectAnimator.ofFloat(this, View.ALPHA, 0f, 1f)
    fadeOut.duration = 0
    fadeOut.startDelay = duration / 2

    return AnimatorSet().apply { playTogether(rotation, fadeOut) }
}

private fun View.wiggleToRight(duration: Long, distance: Float = 100f): AnimatorSet {
    val translateRight = ObjectAnimator.ofFloat(this, View.TRANSLATION_X, 0f, distance)
    val translateBack = ObjectAnimator.ofFloat(this, View.TRANSLATION_X, distance, 0f)
    return AnimatorSet().apply {
        this.duration = duration / 2
        playSequentially(translateRight, translateBack)
    }
}
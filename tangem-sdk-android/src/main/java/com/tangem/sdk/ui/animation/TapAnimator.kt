package com.tangem.sdk.ui.animation

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.core.animation.addListener
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import com.tangem.common.extensions.VoidCallback

/**
 * Created by Anton Zhilenkov on 11/10/2020.
 */
abstract class TapAnimator(
    protected val view: View,
    protected val properties: AnimationProperty,
) {
    var onAnimationEnd: VoidCallback? = null

    var tapAnimationCallback: TapAnimationCallback? = null

    var animatorCallback: Animator.AnimatorListener? = null
        set(value) {
            tapAnimator?.removeListener(field)
            tapAnimator?.addListener(value)
            field = value
        }

    private var tapAnimator: AnimatorSet? = null
    private var repeatedCount = 0

    fun animate() {
        tapAnimator?.cancel()
        tapAnimator = AnimatorSet()
        tapAnimator?.playSequentially(
            tapInAnimation(),
            downTime(properties.tapInDownTimeDuration),
            tapOutAnimation(),
            downTime(properties.tapOutDownTimeDuration),
        )

        var isCancelled = false
        animatorCallback?.let { tapAnimator?.addListener(it) }
        tapAnimator?.addListener(
            onStart = { isCancelled = false },
            onEnd = {
                repeatedCount++
                if (!isCancelled) {
                    if (properties.repeatCount == -1 || repeatedCount < properties.repeatCount) {
                        tapAnimator?.start()
                    } else {
                        onAnimationEnd?.invoke()
                    }
                }
            },
            onCancel = { isCancelled = true },
            onRepeat = { isCancelled = false },
        )
        tapAnimator?.start()
    }

    fun cancel() {
        tapAnimator?.cancel()
        tapAnimator?.removeListener(animatorCallback)
        tapAnimator = null

        view.scaleX = 1f
        view.scaleY = 1f
        view.alpha = 0f
        repeatedCount = 0
    }

    private fun downTime(duration: Long): Animator {
        return ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, 1f).apply { this.duration = duration }
    }

    protected abstract fun tapInAnimation(): AnimatorSet

    protected abstract fun tapOutAnimation(): AnimatorSet

    companion object {
        fun create(view: View, nfcOnTheBack: Boolean, property: AnimationProperty): TapAnimator {
            return if (nfcOnTheBack) {
                TapBackAnimator(view, property)
            } else {
                TapFrontAnimator(view, property)
            }
        }
    }
}

class TapAnimationCallback(
    val onTapInStarted: VoidCallback? = null,
    val onTapInFinished: VoidCallback? = null,
    val onTapOutStarted: VoidCallback? = null,
    val onTapOutFinished: VoidCallback? = null,
)

data class AnimationProperty(
    val xStart: Float,
    val xEnd: Float,
    val yStart: Float,
    val tapDuration: Long = 1200,
    val tapInStartDelay: Long = 500,
    val tapInDownTimeDuration: Long = 3000,
    val tapOutDownTimeDuration: Long = 400,
    val repeatCount: Int = 2,
)

class TapBackAnimator(
    view: View,
    property: AnimationProperty,
) : TapAnimator(view, property) {

    @Suppress("MagicNumber")
    override fun tapInAnimation(): AnimatorSet {
        val scaleUpX = ObjectAnimator.ofFloat(view, View.SCALE_X, 0.5f, 1f)
        val scaleUpY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 0.5f, 1f)
        val alpha = ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f)
        val slideRight = ObjectAnimator.ofFloat(
            view,
            View.TRANSLATION_X,
            properties.xStart,
            view.translationX + properties.xEnd,
        )
        slideRight.interpolator = DecelerateInterpolator()

        val animator = AnimatorSet()
        animator.startDelay = properties.tapInStartDelay
        animator.duration = properties.tapDuration
        animator.playTogether(scaleUpX, scaleUpY, slideRight, alpha)
        animator.doOnStart { tapAnimationCallback?.onTapInStarted?.invoke() }
        animator.doOnEnd { tapAnimationCallback?.onTapInFinished?.invoke() }
        return animator
    }

    @Suppress("MagicNumber")
    override fun tapOutAnimation(): AnimatorSet {
        val scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, 0.5f)
        val scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, 0.5f)
        val alpha = ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0f)
        val slideLeft = ObjectAnimator.ofFloat(
            view,
            View.TRANSLATION_X,
            view.translationX + properties.xEnd,
            properties.xStart,
        )
        slideLeft.interpolator = AccelerateInterpolator()

        val animator = AnimatorSet()
        animator.duration = properties.tapDuration
        animator.playTogether(scaleX, scaleY, slideLeft, alpha)
        animator.doOnStart { tapAnimationCallback?.onTapOutStarted?.invoke() }
        animator.doOnEnd { tapAnimationCallback?.onTapOutFinished?.invoke() }
        return animator
    }
}

class TapFrontAnimator(
    view: View,
    property: AnimationProperty,
) : TapAnimator(view, property) {

    private val fadeDuration = 250L

    @Suppress("MagicNumber")
    override fun tapInAnimation(): AnimatorSet {
        val scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 1.5f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1.5f, 1f)
        val slideRight = ObjectAnimator.ofFloat(
            view,
            View.TRANSLATION_X,
            properties.xStart,
            view.translationX + properties.xEnd,
        )
        val slideUp = ObjectAnimator.ofFloat(
            view,
            View.TRANSLATION_Y,
            view.translationY + properties.yStart,
            view.translationY,
        )
        slideRight.interpolator = DecelerateInterpolator()
        slideUp.interpolator = DecelerateInterpolator()

        val animator = AnimatorSet()
        animator.startDelay = properties.tapInStartDelay
        animator.duration = properties.tapDuration
        animator.playTogether(scaleX, scaleY, slideRight, slideUp)
        animator.doOnStart {
            ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f).setDuration(fadeDuration).apply {
                startDelay = properties.tapInStartDelay
            }.start()
            tapAnimationCallback?.onTapInStarted?.invoke()
        }
        animator.doOnEnd { tapAnimationCallback?.onTapInFinished?.invoke() }
        return animator
    }

    @Suppress("MagicNumber")
    override fun tapOutAnimation(): AnimatorSet {
        val scaleUpX = ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, 1.5f)
        val scaleUpY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, 1.5f)
        val slideLeft = ObjectAnimator.ofFloat(
            view,
            View.TRANSLATION_X,
            view.translationX + properties.xEnd,
            properties.xStart,
        )
        val slideDown = ObjectAnimator.ofFloat(
            view,
            View.TRANSLATION_Y,
            view.translationY,
            view.translationY + properties.yStart,
        )
        slideLeft.interpolator = AccelerateInterpolator()
        slideDown.interpolator = AccelerateInterpolator()

        val animator = AnimatorSet()
        animator.duration = properties.tapDuration
        animator.playTogether(scaleUpX, scaleUpY, slideLeft, slideDown)
        animator.doOnStart {
            ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0f).setDuration(fadeDuration).apply {
                startDelay = properties.tapDuration - fadeDuration - 10
            }.start()
            tapAnimationCallback?.onTapOutStarted?.invoke()
        }
        animator.doOnEnd { tapAnimationCallback?.onTapOutFinished?.invoke() }
        return animator
    }
}

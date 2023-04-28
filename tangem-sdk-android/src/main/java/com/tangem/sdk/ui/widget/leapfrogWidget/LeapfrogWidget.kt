package com.tangem.sdk.ui.widget.leapfrogWidget

import android.animation.Animator
import android.animation.AnimatorSet
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.animation.doOnEnd
import androidx.core.view.children
import com.tangem.common.extensions.VoidCallback
import com.tangem.sdk.extensions.dpToPx

/**
 * Created by Anton Zhilenkov on 06/10/2021.
 */
class LeapfrogWidget(
    private val parentContainer: FrameLayout,
    private val calculator: PropertyCalculator = PropertyCalculator(),
) {
    private val foldAnimationDuration = 300L
    private val leapAnimationDuration = 500L
    private val leapBackAnimationDuration = 500L

    private val leapViews = mutableListOf<LeapView>()

    private var foldUnfoldProgress: Int = 100
    private var leapProgress: Int = 100
    private var leapBackProgress: Int = 100

    private val children = parentContainer.children.filter { it is ImageView }

    init {
        calculator.setTranslationConverter { parentContainer.dpToPx(it) }
        if (viewIsFullFledged()) {
            val maxPosition = children.count() - 1
            children.forEachIndexed { index, view ->
                val initialPosition = maxPosition - index
                leapViews.add(LeapView(view, index, initialPosition, maxPosition, calculator))
            }
        }
    }

    /**
     * Apply initial properties to the views
     */
    fun initViews() {
        leapViews.forEach { it.initView() }
    }

    fun fold(animate: Boolean = true, onEnd: VoidCallback = {}) {
        val animator = foldAnimator(animate)
        if (animator == null) {
            onEnd()
        } else {
            animator.doOnEnd { onEnd() }
            animator.start()
        }
    }

    fun unfold(animate: Boolean = true, onEnd: VoidCallback = {}) {
        val animator = unfoldAnimator(animate)
        if (animator == null) {
            onEnd()
        } else {
            animator.doOnEnd { onEnd() }
            animator.start()
        }
    }

    fun foldAnimator(animate: Boolean = true): Animator? {
        if (!canFoldUnfold()) return null

        return createFoldUnfoldAnimator(true, animate, foldAnimationDuration)
    }

    fun unfoldAnimator(animate: Boolean = true): Animator? {
        if (!canFoldUnfold()) return null

        return createFoldUnfoldAnimator(false, animate, foldAnimationDuration)
    }

    private fun createFoldUnfoldAnimator(isFoldAnimation: Boolean, animate: Boolean, duration: Long): Animator {
        val animatorsList = mutableListOf<Animator>()
        val animateDuration = if (animate) duration else 0
        leapViews.forEach {
            if (isFoldAnimation) {
                it.fold()
                animatorsList.add(it.view.foldAnimation(animateDuration, it.state.properties))
            } else {
                it.unfold()
                animatorsList.add(it.view.unfoldAnimation(animateDuration, it.state.properties))
            }
        }
        animatorsList.add(createProgressListener(animateDuration) { foldUnfoldProgress = it })

        val animator = AnimatorSet()
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.playTogether(animatorsList.toList())
        return animator
    }

    fun leap(animate: Boolean = true, onEnd: VoidCallback = {}) {
        if (!canLeap()) {
            onEnd()
            return
        }

        val duration = if (animate) leapAnimationDuration else 0
        val animatorsList = mutableListOf<Animator>()
        leapViews.forEach {
            when (it.leap()) {
                LeapFrogAnimation.LEAP -> {
                    val overLift = calculator.overLift(leapViews.size)
                    animatorsList.add(it.view.leapAnimation(duration, it.state.properties, overLift))
                }
                LeapFrogAnimation.PULL -> {
                    animatorsList.add(it.view.pullUpAnimation(duration, it.state.properties))
                }
            }
        }
        animatorsList.add(createProgressListener(duration) { leapProgress = it })

        val animator = AnimatorSet()
        animator.playTogether(animatorsList.toList())
        animator.doOnEnd { onEnd() }
        animator.start()
    }

    fun leapBack(animate: Boolean = true, onEnd: VoidCallback = {}) {
        if (!canLeapBack()) {
            onEnd()
            return
        }

        val duration = if (animate) leapBackAnimationDuration else 0
        val animatorsList = mutableListOf<Animator>()
        leapViews.forEach {
            when (it.leapBack()) {
                LeapFrogAnimation.LEAP -> {
                    animatorsList.add(it.view.leapBackAnimation(duration, it.state.properties, calculator))
                }
                LeapFrogAnimation.PULL -> {
                    animatorsList.add(it.view.pullDownAnimation(duration, it.state.properties))
                }
            }
        }
        animatorsList.add(createProgressListener(duration) { leapBackProgress = it })

        val animator = AnimatorSet()
        animator.playTogether(animatorsList.toList())
        animator.doOnEnd { onEnd() }
        animator.start()
    }

    fun getViewsCount(): Int = children.count()

    fun getViewPositionByIndex(index: Int): Int = leapViews.first { it.index == index }.state.currentPosition

    fun getViewByPosition(position: Int): LeapView = leapViews.first { it.state.currentPosition == position }

    fun getState(): LeapfrogWidgetState = LeapfrogWidgetState(leapViews.map { it.state })

    fun applyState(state: LeapfrogWidgetState) {
        state.leapViewStates.forEach { viewState ->
            leapViews.firstOrNull { it.index == viewState.index }?.applyState(viewState)
        }
    }

    private fun viewIsFullFledged(): Boolean = children.count() > 1

    private fun canFoldUnfold(): Boolean {
        return when {
            !viewIsFullFledged() -> false
            foldUnfoldInProgress() -> false
            leapInProgress() || leapBackInProgress() -> false
            else -> true
        }
    }

    @Suppress("MagicNumber")
    private fun canLeap(): Boolean {
        return when {
            !viewIsFullFledged() -> false
            leapBackInProgress() -> false
            leapProgress < 70 -> false
            else -> true
        }
    }

    @Suppress("MagicNumber")
    private fun canLeapBack(): Boolean {
        return when {
            !viewIsFullFledged() -> false
            leapInProgress() -> false
            leapBackProgress < 70 -> false
            else -> true
        }
    }

    @Suppress("MagicNumber")
    private fun foldUnfoldInProgress(): Boolean = foldUnfoldProgress != 100

    @Suppress("MagicNumber")
    private fun leapInProgress(): Boolean = leapProgress != 100

    @Suppress("MagicNumber")
    private fun leapBackInProgress(): Boolean = leapBackProgress != 100
}

data class LeapfrogWidgetState(
    val leapViewStates: List<LeapViewState>,
)

class PropertyCalculator(
    val elevationFactor: Float = 1f,
    val decreaseScaleFactor: Float = 0.15f,
    val yTranslationFactor: Float = 35f,
    val decreaseOverLiftingFactor: Float = 0.85f,
) {
    // 0 view it is view on the top of stack, but it is last in parent.children

    private var translationValueConverter: ((Float) -> Float)? = null
    fun setTranslationConverter(converter: (Float) -> Float) {
        translationValueConverter = converter
    }

    fun scale(position: Int): Float {
        return 1f - decreaseScaleFactor * position
    }

    fun elevation(position: Int, count: Int): Float {
        return elevationFactor * (count - 1 - position)
    }

    fun yTranslation(position: Int): Float {
        val yTranslation = yTranslationFactor * position
        return convertTranslation(yTranslation)
    }

    fun hasForeground(position: Int): Boolean {
        return position != 0
    }

    @Suppress("MagicNumber")
    fun overLift(viewCount: Int): Float {
        val initialOverLift = when {
            yTranslationFactor < 0f -> yTranslationFactor * decreaseOverLiftingFactor * viewCount * -1
            else -> 10f
        }
        val scaleOverLift = when {
            decreaseScaleFactor == 0f -> initialOverLift
            else -> initialOverLift * decreaseOverLiftingFactor - initialOverLift * decreaseScaleFactor
        }
        return convertTranslation(scaleOverLift)
    }

    private fun convertTranslation(value: Float): Float {
        return translationValueConverter?.invoke(value) ?: value
    }
}

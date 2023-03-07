package com.tangem.sdk.ui.animation

import android.animation.Animator
import android.view.View
import com.tangem.common.extensions.VoidCallback
import com.tangem.sdk.extensions.dpToPx
import com.tangem.sdk.extensions.fadeIn
import com.tangem.sdk.extensions.fadeOut
import com.tangem.sdk.extensions.isHorizontal
import com.tangem.sdk.extensions.isOnTheBack
import com.tangem.sdk.ui.NfcLocation

/**
 * Created by Anton Zhilenkov on 13/10/2020.
 */
class TouchCardAnimation(
    private val phone: View,
    private val cardHorizontal: View,
    private val cardVertical: View,
    private val animationProperty: AnimationProperty,
    private val nfcLocation: NfcLocation
) {

    var onAnimationEnd: VoidCallback? = null

    var tapAnimationCallback: TapAnimationCallback? = null
        set(value) {
            field = value
            tapAnimator?.tapAnimationCallback = value
        }

    var animatorCallback: Animator.AnimatorListener? = null
        set(value) {
            field = value
            tapAnimator?.animatorCallback = value
        }

    private val touchView: View = if (nfcLocation.isHorizontal()) cardHorizontal else cardVertical
    private var tapAnimator: TapAnimator? = null

    init {
        touchView.elevation = touchView.dpToPx(getTouchViewElevation(nfcLocation))
    }

    fun animate() {
        tapAnimator?.cancel()
        translateToNfcLocation(touchView, phone, nfcLocation)
        tapAnimator = TapAnimator.create(touchView, nfcLocation.isOnTheBack(), animationProperty)
        tapAnimator?.tapAnimationCallback = tapAnimationCallback
        tapAnimator?.animatorCallback = animatorCallback
        tapAnimator?.onAnimationEnd = onAnimationEnd
        tapAnimator?.animate()
    }

    fun showTouchViewAtNfcPosition(duration: Long, onEnd: VoidCallback? = null) {
        translateToNfcLocation(touchView, phone, nfcLocation)
        touchView.translationX = touchView.translationX + animationProperty.xEnd
        touchView.fadeIn(duration, onEnd = onEnd)
    }

    fun hideTouchView(duration: Long, onEnd: VoidCallback? = null) {
        touchView.fadeOut(duration, onEnd = onEnd)
    }

    fun cancel() {
        tapAnimator?.cancel()
    }

    private fun translateToNfcLocation(tappedView: View, inView: View, nfcLocation: NfcLocation) {
        val yCardCenter = tappedView.height * getYCenterOfCardRelativeToSelf()
        val xPositionFactor = if (nfcLocation.isOnTheBack()) -1 else 1
        tappedView.translationX = calculateRelativePosition(nfcLocation.x, inView.width) * xPositionFactor
        tappedView.translationY = calculateRelativePosition(nfcLocation.y, inView.height) + yCardCenter
    }

    @Suppress("MagicNumber")
    private fun getYCenterOfCardRelativeToSelf(): Float = if (nfcLocation.isHorizontal()) 0.33f else 0.27f

    companion object {
        // Only for views centered in a relative view. Works with positioning on x and y vectors
        fun calculateRelativePosition(location: Float, sizePx: Int): Float {
            val positionInView = location * sizePx
            val translateToStartOfView = sizePx / 2 * -1
            return translateToStartOfView + positionInView
        }

        @Suppress("MagicNumber")
        fun getTouchViewElevation(nfcLocation: NfcLocation): Float = if (nfcLocation.isOnTheBack()) 0f else 4f
    }
}
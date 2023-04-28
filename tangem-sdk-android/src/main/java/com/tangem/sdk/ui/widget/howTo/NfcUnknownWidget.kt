package com.tangem.sdk.ui.widget.howTo

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.core.animation.addListener
import androidx.core.animation.doOnStart
import com.tangem.sdk.R
import com.tangem.sdk.extensions.fadeIn
import com.tangem.sdk.extensions.fadeOut
import com.tangem.sdk.postUI

/**
 * Created by Anton Zhilenkov on 03/08/2020.
 */
class NfcUnknownWidget(
    mainView: View,
) : NfcHowToWidget(mainView) {

    private val handWithCard: View = mainView.findViewById(R.id.imvHandWithCardH)
    private val animatorSet: AnimatorSet = AnimatorSet()

    init {
        initHandAnimators()
    }

    @Suppress("MagicNumber")
    private fun initHandAnimators() {
        val list = mutableListOf<Animator>()
        list.add(getSlideRightAnimation())
        list.add(getSlideDownAnimation())
        list.add(getSlideLeftAnimation())
        animatorSet.playSequentially(list)
        animatorSet.addListener(
            onStart = { handWithCard.alpha = 1f },
            onEnd = {
                if (!isCancelled) {
                    postUI(3000) { onAnimationEnd?.invoke() }
                }
            },
            onCancel = { handWithCard.fadeOut(1000) },
        )
        animatorSet.startDelay = 1000
    }

    @Suppress("MagicNumber")
    private fun getSlideRightAnimation(): Animator {
        val scaleUpX = ObjectAnimator.ofFloat(handWithCard, View.SCALE_X, 0.5f, 1f)
        val scaleUpY = ObjectAnimator.ofFloat(handWithCard, View.SCALE_Y, 0.5f, 1f)
        val xToRight = ObjectAnimator.ofFloat(handWithCard, View.TRANSLATION_X, dpToPx(-395f), dpToPx(-95f))
        xToRight.interpolator = DecelerateInterpolator()

        return AnimatorSet().apply {
            duration = 2000
            playTogether(scaleUpX, scaleUpY, xToRight)
        }
    }

    @Suppress("MagicNumber")
    private fun getSlideDownAnimation(): Animator {
        return ObjectAnimator.ofFloat(handWithCard, View.TRANSLATION_Y, dpToPx(-45f), dpToPx(135f)).apply {
            duration = 9500
            doOnStart { setText(R.string.how_to_unknown_move_card) }
        }
    }

    @Suppress("MagicNumber")
    private fun getSlideLeftAnimation(): Animator {
        val scaleDownX = ObjectAnimator.ofFloat(handWithCard, View.SCALE_X, 1f, 0.5f)
        val scaleDownY = ObjectAnimator.ofFloat(handWithCard, View.SCALE_Y, 1f, 0.5f)

        val xToLeft = ObjectAnimator.ofFloat(handWithCard, View.TRANSLATION_X, dpToPx(-95f), dpToPx(-395f))
        xToLeft.interpolator = AccelerateInterpolator()

        return AnimatorSet().apply {
            duration = 2000
            playTogether(xToLeft, scaleDownX, scaleDownY)
            doOnStart { setText(R.string.how_to_unknown_nothing_happened) }
        }
    }

    override fun setState(params: HowToState) {
        when (params) {
            HowToState.Init -> {
                isCancelled = false
                setMainButtonText(R.string.common_cancel)
                setText(R.string.how_to_unknown_tap_card)
                btnShowAgain.fadeOut(fadeDuration)
                imvSuccess.fadeOut(fadeDuration)
            }
            HowToState.Animate -> {
                rippleView.stopRippleAnimation()
                animatorSet.start()
            }
            HowToState.AntennaFound -> {
                if (currentState == HowToState.AntennaFound) return

                isCancelled = true
                animatorSet.cancel()
                btnShowAgain.fadeIn(fadeDuration)
                imvSuccess.fadeIn(fadeDuration)
                setText(R.string.how_to_nfc_detected)
                setMainButtonText(R.string.how_to_got_it_button)
            }
            HowToState.Cancel -> {
                isCancelled = true
                animatorSet.cancel()
            }
        }
        currentState = params
    }
}

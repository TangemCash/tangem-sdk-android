package com.tangem.tangem_sdk_new.ui.widget.howTo

import android.os.Handler
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.transition.TransitionManager
import com.tangem.common.extensions.VoidCallback
import com.tangem.tangem_sdk_new.R
import com.tangem.tangem_sdk_new.extensions.*
import com.tangem.tangem_sdk_new.ui.NfcLocation
import com.tangem.tangem_sdk_new.ui.animation.AnimationProperty
import com.tangem.tangem_sdk_new.ui.animation.FlipAnimator
import com.tangem.tangem_sdk_new.ui.animation.Side
import com.tangem.tangem_sdk_new.ui.animation.TouchCardAnimation
import com.tangem.tangem_sdk_new.ui.animation.TouchCardAnimation.Companion.calculateRelativePosition


/**
 * Created by Anton Zhilenkov on 11/10/2020.
 */
class NfcKnownWidget(
    mainView: View,
    private val nfcLocation: NfcLocation,
    private val onSwitch: VoidCallback,
) : NfcHowToWidget(mainView) {

    private val phoneBack: ImageView = mainView.findViewById(R.id.imvPhoneBack)
    private val nfcBadge: ImageView = mainView.findViewById(R.id.imvNfcBadge)
    private val btnSwitchMode: Button = mainView.findViewById(R.id.btnSwitchMode)

    private val touchCardAnimation = TouchCardAnimation(
        phone,
        mainView.findViewById(R.id.imvHandWithCardH),
        mainView.findViewById(R.id.imvHandWithCardV),
        AnimationProperty(context.dpToPx(-395f), context.dpToPx(-95f), context.dpToPx(200f)),
        nfcLocation
    )
    private val animationScheduler = Handler()
    private val phoneFlipAnimator: FlipAnimator = FlipAnimator(phone, phoneBack, FLIP_DURATION)

    init {
        changeCameraDistance()
        btnSwitchMode.setOnClickListener { handleSwitchMode() }
        touchCardAnimation.hideTouchView(0)
        phone.hide()
        phoneFlipAnimator.animateToSide(Side.FRONT, 0)
        phone.show()

        btnShowAgain.alpha = 0f
        btnSwitchMode.alpha = 1f

        rippleView.alpha = 0f
        nfcBadge.alpha = 0f
        imvSuccess.alpha = 0f
    }

    private fun changeCameraDistance() {
        val distance = 2500
        val scale: Float = mainView.resources.displayMetrics.density * distance
        phone.cameraDistance = scale
        phoneBack.cameraDistance = scale
    }

    private fun handleSwitchMode() {
        nfcBadge.fadeOut(FADE_DURATION)
        rippleView.fadeOut(FADE_DURATION)
        touchCardAnimation.hideTouchView(FADE_DURATION) {
            rippleView.stopRippleAnimation()
            setState(HowToState.Cancel)
            flipCardToFront(onSwitch)
        }
    }

    override fun setState(params: HowToState) {
        when (params) {
            HowToState.Init -> {
                prepareInitialState()
            }
            HowToState.Animate -> {
                animationScheduler.postDelayed(::showNfcPosition, 3000)
                animationScheduler.postDelayed(::tapToKnownPosition, 9000)
            }
            HowToState.AntennaFound -> {
                if (currentState == HowToState.AntennaFound) return

                antennaIsFound()
            }
            HowToState.Cancel -> {
                cancelAndRemoveCallbacks()
            }
        }
        currentState = params
    }

    private fun cancelAndRemoveCallbacks() {
        isCancelled = true
        animationScheduler.removeCallbacksAndMessages(null)
        touchCardAnimation.cancel()
        phoneFlipAnimator.cancel()
    }

    private fun setDefaultElevations() {
        // touchViewElevation = 0 or 4
        val phoneElevation = dpToPx(2f)
        val badgeElevation = dpToPx(3f)
        phone.elevation = phoneElevation
        phoneBack.elevation = phoneElevation
        rippleView.elevation = badgeElevation
        nfcBadge.elevation = badgeElevation
    }

    private fun translateViewToNfcLocation(view: View) {
        view.translationX = calculateRelativePosition(nfcLocation.x, phone.width)
        view.translationY = calculateRelativePosition(nfcLocation.y, phone.height)
    }

    private fun prepareInitialState() {
        isCancelled = false
        setMainButtonText(R.string.common_cancel)
        setText(R.string.how_to_known_here_how_to_use)

        setDefaultElevations()
        TransitionManager.beginDelayedTransition(mainView as ViewGroup)

        touchCardAnimation.hideTouchView(FADE_DURATION)
        phone.hide()
        phoneFlipAnimator.animateToSide(Side.FRONT, 0)
        phone.show()
        btnShowAgain.hideWithFade(FADE_DURATION_HALF) { btnSwitchMode.showWithFade(FADE_DURATION_HALF) }

        rippleView.fadeOut(FADE_DURATION)
        nfcBadge.fadeOut(FADE_DURATION)
        imvSuccess.fadeOut(FADE_DURATION)
    }

    private fun showNfcPosition() {
        setText(R.string.how_to_known_antenna_is_here)
        val fadeIn = {
            val duration = 1000L
            rippleView.fadeIn(duration)
            nfcBadge.fadeIn(duration)
        }

        translateViewToNfcLocation(rippleView)
        translateViewToNfcLocation(nfcBadge)

        rippleView.startRippleAnimation()

        if (nfcLocation.isOnTheBack()) {
            phoneFlipAnimator.onAnimationEnd = fadeIn
            phoneFlipAnimator.animate()
        } else {
            fadeIn()
        }
    }

    private fun tapToKnownPosition() {
        touchCardAnimation.onAnimationEnd = { if (!isCancelled) onAnimationEnd?.invoke() }

        nfcBadge.fadeOut(FADE_DURATION)
        rippleView.fadeOut(FADE_DURATION) {
            rippleView.stopRippleAnimation()
            if (nfcLocation.isOnTheBack()) {
                setText(R.string.how_to_known_tap_card_to_the_back)
                phoneFlipAnimator.onAnimationEnd = { touchCardAnimation.animate() }
                phoneFlipAnimator.animate()
            } else {
                setText(R.string.how_to_known_tap_card_to_the_front)
                touchCardAnimation.animate()
            }
        }
    }

    private fun antennaIsFound() {
        setText(R.string.how_to_nfc_detected)
        setMainButtonText(R.string.how_to_got_it_button)

        touchCardAnimation.cancel()

        btnSwitchMode.hideWithFade(FADE_DURATION_HALF) { btnShowAgain.showWithFade(FADE_DURATION_HALF) }
        nfcBadge.fadeOut(FADE_DURATION)
        rippleView.fadeOut(FADE_DURATION)
        rippleView.stopRippleAnimation()

        translateViewToNfcLocation(rippleView)
        translateViewToNfcLocation(nfcBadge)

        val showCardAndRippleView = {
            touchCardAnimation.showTouchViewAtNfcPosition(FADE_DURATION) {
                rippleView.elevation = if (nfcLocation.isOnTheBack()) dpToPx(1f) else rippleView.elevation
                rippleView.alpha = 1f
                rippleView.startRippleAnimation()
                imvSuccess.fadeIn(FADE_DURATION)
            }
        }

        val flipToFrontAndShow = {
            cancelAndRemoveCallbacks()
            flipCardToFront(showCardAndRippleView)
        }

        if (phoneFlipAnimator.animationInProgress) {
            phoneFlipAnimator.onAnimationEnd = flipToFrontAndShow
        } else {
            flipToFrontAndShow()
        }
    }

    private fun flipCardToFront(onFlipDone: VoidCallback? = null) {
        if (phoneFlipAnimator.visibleSide == Side.FRONT) {
            onFlipDone?.invoke()
        } else {
            phoneFlipAnimator.onAnimationEnd = onFlipDone
            phoneFlipAnimator.animateToSide(Side.FRONT, FLIP_DURATION)
        }
    }
}
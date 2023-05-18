package com.tangem.sdk.ui.widget

import android.animation.Animator
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import com.skyfishjy.library.RippleBackground
import com.tangem.common.core.ScanTagImage
import com.tangem.sdk.R
import com.tangem.sdk.SessionViewDelegateState
import com.tangem.sdk.extensions.dpToPx
import com.tangem.sdk.extensions.fadeOut
import com.tangem.sdk.extensions.hide
import com.tangem.sdk.extensions.isOnTheBack
import com.tangem.sdk.extensions.show
import com.tangem.sdk.ui.NfcLocation
import com.tangem.sdk.ui.animation.AnimationProperty
import com.tangem.sdk.ui.animation.TapAnimationCallback
import com.tangem.sdk.ui.animation.TouchCardAnimation
import com.tangem.sdk.ui.animation.TouchCardAnimation.Companion.calculateRelativePosition

/**
 * Created by Anton Zhilenkov on 09/08/2020.
 */
class TouchCardWidget(
    mainView: View,
    private val nfcLocation: NfcLocation,
    private var scanImage: ScanTagImage = ScanTagImage.GenericCard,
) : BaseSessionDelegateStateWidget(mainView) {

    private val genericImageContainer = mainView.findViewById<ViewGroup>(R.id.clGenericImage)
    private val customImageContainer = mainView.findViewById<ViewGroup>(R.id.flCustomImage)
    private val customImageView = mainView.findViewById<AppCompatImageView>(R.id.customImage)

    private val rippleBackgroundNfc = mainView.findViewById<RippleBackground>(R.id.rippleBackgroundNfc)
    private val ivHandCardHorizontal = mainView.findViewById<ImageView>(R.id.ivHandCardHorizontal)
    private val ivHandCardVertical = mainView.findViewById<ImageView>(R.id.ivHandCardVertical)
    private val ivPhone = mainView.findViewById<ImageView>(R.id.ivPhone)

    private val touchCardAnimation = TouchCardAnimation(
        ivPhone,
        ivHandCardHorizontal,
        ivHandCardVertical,
        AnimationProperty(mainView.dpToPx(-160f), mainView.dpToPx(-70f), mainView.dpToPx(150f), repeatCount = -1),
        nfcLocation,
    )

    private var customBitmapHolder: BitmapImageHolder? = null

    init {
        rippleBackgroundNfc.alpha = 0f
    }

    override fun setState(params: SessionViewDelegateState) {
        when (val scanTagImage = scanImage) {
            ScanTagImage.GenericCard -> {
                when (params) {
                    is SessionViewDelegateState.Ready -> animate()
                    is SessionViewDelegateState.TagLost -> animate()
                    else -> stopAnimation()
                }
            }
            is ScanTagImage.Image -> {
                var holder = customBitmapHolder
                if (holder == null) {
                    holder = BitmapImageHolder(scanTagImage.bitmapArray)
                } else if (!holder.bitmapArray.contentEquals(scanTagImage.bitmapArray)) {
                    holder.bitmap.recycle()
                    holder = BitmapImageHolder(scanTagImage.bitmapArray)
                }

                if (scanTagImage.verticalOffset != customImageView.paddingTop) {
                    customImageView.setPadding(
                        customImageView.paddingStart,
                        scanTagImage.verticalOffset,
                        customImageView.paddingEnd,
                        customImageView.paddingBottom,
                    )
                }
                customImageView.setImageBitmap(holder.bitmap)
                customBitmapHolder = holder
            }
        }
        switchScanTagImageVisibility()
    }

    private fun switchScanTagImageVisibility() {
        when (scanImage) {
            ScanTagImage.GenericCard -> {
                customImageContainer.hide()
                genericImageContainer.show()
            }
            is ScanTagImage.Image -> {
                genericImageContainer.hide()
                customImageContainer.show()
            }
        }
    }

    fun setScanImage(scanImage: ScanTagImage) {
        this.scanImage = scanImage
    }

    private fun animate() {
        setCallbacks()
        ivPhone.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    ivPhone.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    val rippleElevation =
                        if (nfcLocation.isOnTheBack()) ivPhone.elevation - 1 else ivPhone.elevation + 1
                    rippleBackgroundNfc.elevation = rippleElevation
                    rippleBackgroundNfc.translationX = calculateRelativePosition(nfcLocation.x, ivPhone.width)
                    rippleBackgroundNfc.translationY = calculateRelativePosition(nfcLocation.y, ivPhone.height)
                    touchCardAnimation.animate()
                }
            },
        )
    }

    private fun setCallbacks() {
        touchCardAnimation.tapAnimationCallback = TapAnimationCallback(
            onTapInFinished = {
                rippleBackgroundNfc.alpha = 1f
                rippleBackgroundNfc.startRippleAnimation()
            },
            onTapOutStarted = {
                rippleBackgroundNfc.fadeOut(fadeOutDuration = 800) { rippleBackgroundNfc.stopRippleAnimation() }
            },
        )
        touchCardAnimation.animatorCallback = object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {}
            override fun onAnimationCancel(animation: Animator) {
                rippleBackgroundNfc.stopRippleAnimation()
                rippleBackgroundNfc.hide()
            }

            override fun onAnimationRepeat(animation: Animator) {}
        }
    }

    private fun stopAnimation() {
        rippleBackgroundNfc.stopRippleAnimation()
        touchCardAnimation.cancel()
    }

    override fun onBottomSheetDismiss() {
        stopAnimation()
        customBitmapHolder?.bitmap?.recycle()
    }

    private data class BitmapImageHolder(
        val bitmapArray: ByteArray,
        val bitmap: Bitmap = BitmapFactory.decodeByteArray(bitmapArray, 0, bitmapArray.size, BitmapFactory.Options()),
    )
}

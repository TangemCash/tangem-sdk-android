package com.tangem.sdk.ui.widget.progressBar

import android.graphics.drawable.Animatable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.tangem.sdk.R
import com.tangem.sdk.SessionViewDelegateState
import com.tangem.sdk.extensions.hide
import com.tangem.sdk.extensions.parseColor
import com.tangem.sdk.extensions.show
import com.tangem.sdk.postUI
import com.tangem.sdk.ui.widget.BaseSessionDelegateStateWidget

/**
 * Created by Anton Zhilenkov on 27/07/2020.
 */

class ProgressbarStateWidget(mainView: View) : BaseSessionDelegateStateWidget(mainView) {

    private val progressIndicator: CircularProgressIndicator = mainView.findViewById(R.id.progressBar)
    private val tvProgressValue: TextView = mainView.findViewById(R.id.tvProgressValue)
    private val doneView: ImageView = mainView.findViewById(R.id.imvSuccess)
    private val exclamationView: ImageView = mainView.findViewById(R.id.imvExclamation)

    private var isSDCountDownActive: Boolean = false
    private var isSDCountDownInterrupted: Boolean = false

    init {
        hideViews(tvProgressValue, progressIndicator, doneView, exclamationView)
        progressIndicator.applyPrimaryColor()
    }

    private var prevState: SessionViewDelegateState? = null

    override fun setState(params: SessionViewDelegateState) {
        when (params) {
            is SessionViewDelegateState.TagConnected -> handleTagConnected()
            is SessionViewDelegateState.TagLost -> handleTagLost()
            is SessionViewDelegateState.Success -> handleSuccessState()
            is SessionViewDelegateState.WrongCard -> handleErrorState()
            is SessionViewDelegateState.Error -> handleErrorState()
            is SessionViewDelegateState.SecurityDelay -> handleSecurityDelayState(params)
            is SessionViewDelegateState.Delay -> handleDelayState()
            is SessionViewDelegateState.PinRequested -> handleWarningState() // it's never used
            else -> handleNoneState()
        }
        prevState = params
    }

    private fun handleTagConnected() {
        hideViews(progressIndicator, tvProgressValue, doneView, exclamationView)
        progressIndicator.isIndeterminate = true
        progressIndicator.applyPrimaryColor()
        progressIndicator.disableTrackColor()
        showViews(progressIndicator)
    }

    private fun handleTagLost() {
        if (isSDCountDownActive) isSDCountDownInterrupted = true
    }

    private fun handleSuccessState() {
        updateWidgetIntonation(R.color.sdk_progress_bar_success, doneView, exclamationView)
    }

    private fun handleWarningState() {
        updateWidgetIntonation(R.color.sdk_progress_bar_warning, exclamationView, doneView)
    }

    @Suppress("MagicNumber")
    private fun handleErrorState() {
        val delay = if (prevState is SessionViewDelegateState.TagConnected) 500L else 0L
        postUI(delay) {
            updateWidgetIntonation(R.color.sdk_progress_bar_error, exclamationView, doneView)
        }
    }

    private fun updateWidgetIntonation(@ColorRes color: Int, toShow: ImageView, toHide: ImageView) {
        hideViews(tvProgressValue, progressIndicator, toHide)

        progressIndicator.applyProgressColor(color)
        exclamationView.drawable.setTint(exclamationView.parseColor(color))

        setMaxProgress()
        showViews(progressIndicator)
        showAndAnimateDrawable(toShow)
    }

    private fun handleSecurityDelayState(params: SessionViewDelegateState.SecurityDelay) {
        val delay = params as? SessionViewDelegateState.SecurityDelay ?: return

        hideViews(doneView, exclamationView)
        showViews(progressIndicator, tvProgressValue)

        if (delay.isUserCodeFailsSD()) {
            userCodesFailsSD(delay)
        } else {
            standardSD(delay)
        }
    }

    @Suppress("MagicNumber")
    private fun userCodesFailsSD(delay: SessionViewDelegateState.SecurityDelay) {
        val seconds = delay.ms.div(100)
        val isSingleTick = !isSDCountDownActive && seconds == 0
        val isCountDownFinished = isSDCountDownActive && seconds == 0

        when {
            isSingleTick -> {
                setProgress(progressIndicator.max)
                showAndAnimateDrawable(doneView)
            }
            !isSDCountDownActive -> {
                isSDCountDownActive = true
                progressIndicator.applyTrackColor()
                progressIndicator.isIndeterminate = false
                setProgress(0, false)
                progressIndicator.max = seconds
                tvProgressValue.text = seconds.toString()
            }
            isCountDownFinished -> {
                isSDCountDownActive = false
                setProgress(progressIndicator.max - seconds, !isSDCountDownInterrupted)
                tvProgressValue.text = seconds.toString()
            }
            else -> {
                setProgress(progressIndicator.max - seconds, !isSDCountDownInterrupted)
                tvProgressValue.text = seconds.toString()
            }
        }
        isSDCountDownInterrupted = false
    }

    private fun standardSD(delay: SessionViewDelegateState.SecurityDelay) {
        val seconds = delay.ms.div(other = 100)
        if (seconds == 0) {
            isSDCountDownActive = false
            progressIndicator.disableTrackColor()
            hideViews(tvProgressValue)
        } else if (!isSDCountDownActive) {
            isSDCountDownActive = true
            progressIndicator.isIndeterminate = false
            setProgress(0, false)
            progressIndicator.applyTrackColor()
            progressIndicator.max = seconds
        }

        setProgress(progressIndicator.max - seconds)
        tvProgressValue.text = seconds.toString()
    }

    private fun handleDelayState() {
        hideViews(progressIndicator, tvProgressValue, doneView, exclamationView)
        progressIndicator.isIndeterminate = true
        progressIndicator.applyPrimaryColor()
        showViews(progressIndicator)
    }

    private fun handleNoneState() {
        hideViews(progressIndicator, tvProgressValue, doneView, exclamationView)
        setProgress(0, false)
        progressIndicator.applyPrimaryColor()
    }

    @Suppress("MagicNumber")
    private fun setMaxProgress() {
        val max = progressIndicator.max
        val current = progressIndicator.progress
        val nearOfMax = ((max * 0.8).toInt()..max).contains(current)
        setProgress(progressIndicator.max, nearOfMax)
    }

    private fun setProgress(progress: Int, withAnimation: Boolean = true) {
        progressIndicator.setProgressCompat(progress, withAnimation)
    }

    private fun showViews(vararg views: View) {
        views.forEach { it.show() }
    }

    private fun hideViews(vararg views: View) {
        views.forEach { it.hide() }
    }

    private fun showAndAnimateDrawable(view: ImageView) {
        view.show()
        (view.drawable as? Animatable)?.start()
    }

    override fun onBottomSheetDismiss() {
        handleNoneState()
    }
}

private fun CircularProgressIndicator.applyPrimaryColor() {
    applyProgressColor(R.color.sdk_progress_bar_primary)
}

private fun CircularProgressIndicator.applyProgressColor(@ColorRes color: Int) {
    setIndicatorColor(parseColor(color))
}

private fun CircularProgressIndicator.applyTrackColor() {
    trackColor = parseColor(R.color.sdk_progress_bar_secondary)
}

private fun CircularProgressIndicator.disableTrackColor() {
    trackColor = parseColor(android.R.color.transparent)
}

@Suppress("MagicNumber")
private fun SessionViewDelegateState.SecurityDelay.isUserCodeFailsSD(): Boolean {
    return this.ms.mod(100) != 0
}
package com.tangem.tangem_sdk_new.ui.widget.progressBar

import android.graphics.drawable.Animatable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.tangem.tangem_sdk_new.R
import com.tangem.tangem_sdk_new.SessionViewDelegateState
import com.tangem.tangem_sdk_new.extensions.hide
import com.tangem.tangem_sdk_new.extensions.show
import com.tangem.tangem_sdk_new.ui.widget.BaseSessionDelegateStateWidget

/**
 * Created by Anton Zhilenkov on 27/07/2020.
 */

class ProgressbarStateWidget(mainView: View) : BaseSessionDelegateStateWidget(mainView) {

    init {
        NoneState(mainView)
    }

    override fun setState(params: SessionViewDelegateState) {
        val viewState = when (params) {
            is SessionViewDelegateState.SecurityDelay -> SecurityDelayState(mainView)
            is SessionViewDelegateState.Delay -> DelayState(mainView)
            is SessionViewDelegateState.Success -> SuccessState(mainView)
            is SessionViewDelegateState.Error -> ErrorState(mainView)
            is SessionViewDelegateState.WrongCard -> ErrorState(mainView)
            is SessionViewDelegateState.PinRequested -> WarningState(mainView)
            is SessionViewDelegateState.TagConnected -> IndeterminateProgressState(mainView)
            else -> NoneState(mainView)
        }
        viewState.setState(params)
    }

    override fun onBottomSheetDismiss() {
        NoneState(mainView)
    }
}

abstract class BaseProgressState(mainView: View) : BaseSessionDelegateStateWidget(mainView) {
    protected val progressBar: SdkProgressBar = mainView.findViewById(R.id.progressBar)
    protected val tvProgressValue: TextView = mainView.findViewById(R.id.tvProgressValue)

    protected val doneView: ImageView = mainView.findViewById(R.id.imvSuccess)
    protected val exclamationView: ImageView = mainView.findViewById(R.id.imvExclamation)


    init {
        hideViews(tvProgressValue, progressBar, doneView, exclamationView)
    }

    protected fun setMaxProgress() {
        setProgress(progressBar.progressMax)
    }

    protected fun setProgress(progress: Float, withAnimation: Boolean = true) {
        if (progressBar.isIndeterminate) progressBar.isIndeterminate = false

        if (withAnimation) progressBar.setProgressWithAnimation(progress)
        else progressBar.progress = progress
    }

    protected fun changeProgressColor(color: Int, withAnimation: Boolean = true) {
        val parsedColor = ContextCompat.getColor(progressBar.context, color)

        if (withAnimation) progressBar.setProgressBarColorWithAnimation(parsedColor)
        else progressBar.progressBarColor = parsedColor

    }

    protected fun changeExclamationColor(color: Int) {
        exclamationView.drawable.setTint(ContextCompat.getColor(progressBar.context, color))
    }

    protected fun showViews(vararg views: View) {
        views.forEach { it.show() }
    }

    protected fun hideViews(vararg views: View) {
        views.forEach { it.hide() }
    }

    protected fun showAndAnimateDrawable(view: ImageView) {
        view.show()
        (view.drawable as? Animatable)?.start()
    }
}

class NoneState(mainView: View) : BaseProgressState(mainView) {
    override fun setState(params: SessionViewDelegateState) {
        hideViews(progressBar, tvProgressValue, doneView, exclamationView)

        changeProgressColor(R.color.sdk_progress_bar_secondary, false)
        setProgress(0f, false)
    }
}

class SuccessState(mainView: View) : BaseProgressState(mainView) {
    override fun setState(params: SessionViewDelegateState) {
        hideViews(tvProgressValue, progressBar, exclamationView)
        showViews(progressBar)

        changeProgressColor(R.color.sdk_progress_bar_success)
        setMaxProgress()
        showAndAnimateDrawable(doneView)
    }
}


class ErrorState(mainView: View) : BaseProgressState(mainView) {
    override fun setState(params: SessionViewDelegateState) {
        hideViews(tvProgressValue, doneView)
        showViews(progressBar)

        val color = R.color.sdk_progress_bar_error
        changeProgressColor(color)
        setMaxProgress()
        changeExclamationColor(color)
        showAndAnimateDrawable(exclamationView)
    }
}

class WarningState(mainView: View) : BaseProgressState(mainView) {
    override fun setState(params: SessionViewDelegateState) {
        hideViews(tvProgressValue, doneView)
        showViews(progressBar)

        val color = R.color.sdk_progress_bar_warning
        changeProgressColor(color)
        setMaxProgress()
        changeExclamationColor(color)
        showAndAnimateDrawable(exclamationView)
    }
}

class IndeterminateProgressState(mainView: View) : BaseProgressState(mainView) {
    override fun setState(params: SessionViewDelegateState) {
        changeProgressColor(R.color.sdk_progress_bar_primary, false)

        hideViews(tvProgressValue, doneView, exclamationView)
        showViews(progressBar)

        progressBar.isIndeterminate = true
    }
}

class SecurityDelayState(mainView: View) : BaseProgressState(mainView) {

    override fun setState(params: SessionViewDelegateState) {
        val delay = params as? SessionViewDelegateState.SecurityDelay ?: return

        changeProgressColor(R.color.sdk_progress_bar_primary, false)
        hideViews(doneView, exclamationView)
        showViews(progressBar, tvProgressValue)

        if (delay.totalDurationSeconds == 0) {
            pin1FailsSd(delay)
        } else {
            otherSd(delay)
        }
    }

    private fun pin1FailsSd(delay: SessionViewDelegateState.SecurityDelay) {
        fun isCountDownInActiveState(): Boolean = progressBar.isCountDownActive
        fun isCountDownFinished(seconds: Int): Boolean = isCountDownInActiveState() && seconds == 0

        val seconds = delay.ms.div(100)
        when {
            !isCountDownInActiveState() && seconds == 0 -> {
                // handle the single tick of starting count down SD
                setProgress(progressBar.progressMax)
                showViews(progressBar, doneView)
                return
            }
            !isCountDownInActiveState() -> {
                setProgress(0f, false)
                progressBar.isCountDownActive = true
                progressBar.progressMax = seconds.toFloat()
            }
            isCountDownFinished(seconds) -> {
                progressBar.isCountDownActive = false
                hideViews(tvProgressValue)
                showViews(progressBar, doneView)
                setProgress(progressBar.progressMax)
            }
        }
        if (isCountDownFinished(seconds)) return

        val progress = progressBar.progressMax - seconds
        setProgress(progress)
        tvProgressValue.text = delay.ms.div(100).toString()
    }

    private fun otherSd(delay: SessionViewDelegateState.SecurityDelay) {
        val seconds = delay.ms.div(100).toFloat()
        if (seconds == 0f) {
            progressBar.isCountDownActive = false
            hideViews(tvProgressValue)
            showViews(progressBar, doneView)
            setProgress(progressBar.progressMax)
        } else if (!progressBar.isCountDownActive) {
            progressBar.isCountDownActive = true
            progressBar.progressMax = seconds
            setProgress(0f, false)
        }
        setProgress(progressBar.progressMax - seconds)
        tvProgressValue.text = seconds.toInt().toString()
    }
}

class DelayState(mainView: View) : BaseProgressState(mainView) {
    override fun setState(params: SessionViewDelegateState) {
        changeProgressColor(R.color.sdk_progress_bar_primary, false)

        hideViews(tvProgressValue, doneView, exclamationView)
        showViews(progressBar)

        progressBar.isIndeterminate = true
    }
}
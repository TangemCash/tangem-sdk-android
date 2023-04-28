package com.tangem.sdk.ui.widget.howTo

import android.content.Context
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextSwitcher
import com.skyfishjy.library.RippleBackground
import com.tangem.common.extensions.VoidCallback
import com.tangem.sdk.R
import com.tangem.sdk.SessionViewDelegateState
import com.tangem.sdk.extensions.dpToPx
import com.tangem.sdk.extensions.fadeIn
import com.tangem.sdk.extensions.fadeOut
import com.tangem.sdk.extensions.hide
import com.tangem.sdk.extensions.show
import com.tangem.sdk.nfc.NfcLocationProvider
import com.tangem.sdk.nfc.NfcManager
import com.tangem.sdk.ui.widget.BaseSessionDelegateStateWidget
import com.tangem.sdk.ui.widget.BaseStateWidget

/**
 * Created by Anton Zhilenkov on 11/10/2020.
 */
class HowToTapWidget constructor(
    mainView: View,
    private val nfcManager: NfcManager,
    private val nfcLocationProvider: NfcLocationProvider,
) : BaseSessionDelegateStateWidget(mainView) {

    var onCloseListener: VoidCallback? = null
        set(value) {
            field = value
            controller?.onClose = value
        }
    var previousState: SessionViewDelegateState? = null

    private val vibrator = mainView.context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    private val nfcLocation = nfcLocationProvider.getLocation()

    private var initialMode: HowToMode = if (nfcLocation == null) HowToMode.UNKNOWN else HowToMode.KNOWN
    private var currentMode: HowToMode = initialMode
    private var controller: HowToController? = null

    private val viewContainer = mainView as ViewGroup

    override fun setState(params: SessionViewDelegateState) {
        when (params) {
            SessionViewDelegateState.HowToTap -> {
                controller?.stop()
                controller = HowToController(createWidget(currentMode), vibrator, nfcManager)
                controller?.onClose = onCloseListener
                viewContainer.removeAllViews()
                viewContainer.addView(controller?.getView())
                controller?.start()
            }
            else -> {
                currentMode = initialMode
                controller?.stop()
                viewContainer.removeAllViews()
            }
        }
    }

    override fun onBottomSheetDismiss() {
        controller?.stop()
    }

    private fun createWidget(mode: HowToMode): NfcHowToWidget {
        val layoutInflater = LayoutInflater.from(mainView.context)
        return when (mode) {
            HowToMode.KNOWN -> {
                val view = layoutInflater.inflate(R.layout.how_to_known, null)
                NfcKnownWidget(
                    mainView = view,
                    nfcLocation = nfcLocation!!,
                    onSwitch = {
                        currentMode = HowToMode.UNKNOWN
                        setState(SessionViewDelegateState.HowToTap)
                    },
                )
            }
            HowToMode.UNKNOWN -> {
                NfcUnknownWidget(layoutInflater.inflate(R.layout.how_to_unknown, null))
            }
        }
    }
}

abstract class NfcHowToWidget(mainView: View) : BaseStateWidget<HowToState>(mainView) {

    var onClose: VoidCallback? = null
    var onAnimationEnd: VoidCallback? = null

    val view: View
        get() = mainView

    protected val flipDuration = 650L
    protected val fadeDuration = 400L
    protected val fadeDurationHalf = fadeDuration / 2

    protected val context = mainView.context
    protected val rippleView: RippleBackground = mainView.findViewById(R.id.rippleBg)
    protected val tvSwitcher: TextSwitcher = mainView.findViewById(R.id.tvHowToSwitcher)
    protected val phone: ImageView = mainView.findViewById(R.id.imvPhone)
    protected val btnCancel: Button = mainView.findViewById(R.id.btnCancel)
    protected val btnShowAgain: Button = mainView.findViewById(R.id.btnShowAgain)
    protected val imvSuccess: ImageView = mainView.findViewById(R.id.imvSuccess)

    protected var currentState: HowToState? = null
    protected var isCancelled = false

    init {
        initTextChangesAnimation()
        btnShowAgain.hideWithFade(duration = 0)
        btnShowAgain.setOnClickListener {
            setState(HowToState.Init)
            setState(HowToState.Animate)
        }
        btnCancel.setOnClickListener { onClose?.invoke() }
        imvSuccess.alpha = 0f
        imvSuccess.elevation = imvSuccess.dpToPx(dp = 5f)
    }

    override fun onBottomSheetDismiss() {
        setState(HowToState.Cancel)
    }

    protected fun setText(textId: Int) {
        tvSwitcher.setText(tvSwitcher.context.getString(textId))
    }

    protected fun setMainButtonText(textId: Int) {
        btnCancel.setText(textId)
    }

    protected fun dpToPx(value: Float): Float = mainView.dpToPx(value)

    private fun initTextChangesAnimation() {
        tvSwitcher.setInAnimation(context, android.R.anim.slide_in_left)
        tvSwitcher.setOutAnimation(context, android.R.anim.slide_out_right)
    }
}

fun View.hideWithFade(duration: Long, onEnd: VoidCallback? = null) {
    this.fadeOut(duration) {
        this.hide()
        onEnd?.invoke()
    }
}

fun View.showWithFade(duration: Long, onEnd: VoidCallback? = null) {
    this.show()
    this.fadeIn(duration, onEnd = onEnd)
}

package com.tangem.sdk.ui.widget.howTo

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import com.tangem.common.extensions.VoidCallback
import com.tangem.sdk.nfc.NfcManager
import com.tangem.sdk.postUI

/**
 * Created by Anton Zhilenkov on 11/10/2020.
 */
class HowToController(
    private val stateWidget: NfcHowToWidget,
    private val vibrator: Vibrator,
    private val nfcManager: NfcManager,
) {

    var onClose: VoidCallback?
        get() = stateWidget.onClose
        set(value) {
            stateWidget.onClose = value
        }

    private val tagDiscoveredListener = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(100)
        }
        postUI { stateWidget.setState(HowToState.AntennaFound) }
    }

    fun getView(): View = stateWidget.view

    fun start() {
        nfcManager.readingIsActive = false
        stateWidget.setState(HowToState.Cancel)
        stateWidget.setState(HowToState.Init)
        stateWidget.setState(HowToState.Animate)
        stateWidget.onAnimationEnd = { start() }
        nfcManager.removeTagDiscoveredListener(tagDiscoveredListener)
        nfcManager.addTagDiscoveredListener(tagDiscoveredListener)
    }

    fun stop() {
        nfcManager.removeTagDiscoveredListener(tagDiscoveredListener)
        nfcManager.readingIsActive = true
        stateWidget.setState(HowToState.Cancel)
    }
}

enum class HowToMode {
    KNOWN, UNKNOWN
}

enum class HowToState {
    Init,
    Animate,
    AntennaFound,
    Cancel,
}
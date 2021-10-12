package com.tangem.tangem_sdk_new.ui.widget.howTo

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import com.tangem.common.extensions.VoidCallback
import com.tangem.tangem_sdk_new.nfc.NfcManager
import com.tangem.tangem_sdk_new.postUI

/**
 * Created by Anton Zhilenkov on 11/10/2020.
 */
class HowToController(
    private val stateWidget: NfcHowToWidget,
    private val vibrator: Vibrator,
    private val nfcManager: NfcManager
) {

    var onClose: VoidCallback?
        get() = stateWidget.onClose
        set(value) {
            stateWidget.onClose = value
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

    private val tagDiscoveredListener = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(100)
        }
        postUI { stateWidget.setState(HowToState.AntennaFound) }
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
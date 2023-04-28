package com.tangem.sdk

import android.os.Handler
import android.os.Looper

val uiHandler = Handler(Looper.getMainLooper())

internal fun postUI(msTime: Long = 0, func: () -> Unit) {
    if (msTime > 0) {
        uiHandler.postDelayed(func, msTime)
    } else {
        uiHandler.post(func)
    }
}

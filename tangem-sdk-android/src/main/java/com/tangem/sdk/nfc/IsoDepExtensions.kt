package com.tangem.sdk.nfc

import android.nfc.tech.IsoDep
import com.tangem.Log
import java.io.IOException

internal fun IsoDep.connectInternal(onError: () -> Unit) {
    Log.nfc { "connectInternal" }
    try {
        this.connect()
    } catch (e: IOException) {
        Log.nfc { "connectInternal error $e" }
        onError()
    }
}

internal fun IsoDep.closeInternal(onError: () -> Unit) {
    Log.nfc { "closeInternal" }
    try {
        this.close()
    } catch (e: IOException) {
        Log.nfc { "closeInternal error $e" }
        onError()
    }
}

package com.tangem.sdk.nfc

import android.nfc.tech.IsoDep
import com.tangem.Log

internal fun IsoDep.connectInternal() {
    Log.nfc { "connectInternal" }
    this.connect()
}

internal fun IsoDep.closeInternal() {
    Log.nfc { "closeInternal" }
    this.close()
}

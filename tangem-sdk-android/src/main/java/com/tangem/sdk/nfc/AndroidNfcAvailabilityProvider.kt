package com.tangem.sdk.nfc

import android.content.Context
import android.content.pm.PackageManager
import com.tangem.common.nfc.NfcAvailabilityProvider

class AndroidNfcAvailabilityProvider(private val context: Context) : NfcAvailabilityProvider {

    override fun isNfcFeatureAvailable(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC)
    }
}

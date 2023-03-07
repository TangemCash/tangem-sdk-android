package com.tangem.sdk.nfc

import com.tangem.sdk.ui.NfcLocation

/**
 * Created by Anton Zhilenkov on 13/10/2020.
 */
interface NfcLocationProvider {
    fun getLocation(): NfcLocation?
}

class NfcAntennaLocationProvider(val roProductDevice: String) : NfcLocationProvider {

    private var nfcLocation: NfcLocation? = null

    init {
        val foundDevices = NfcLocation.values().filter { roProductDevice.startsWith(it.codename) }
        if (foundDevices.isNotEmpty()) {
            val location = foundDevices[0]
            nfcLocation = location
        }
    }

    override fun getLocation(): NfcLocation? = nfcLocation
}
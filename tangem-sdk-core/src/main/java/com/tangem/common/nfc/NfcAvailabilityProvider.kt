package com.tangem.common.nfc

/**
 * Provides info about device NFC feature availability
 */
interface NfcAvailabilityProvider {

    fun isNfcFeatureAvailable(): Boolean
}

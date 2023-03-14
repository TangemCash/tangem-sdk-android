package com.tangem.sdk.extensions

import com.tangem.sdk.ui.NfcLocation

/**
 * Created by Anton Zhilenkov on 13/10/2020.
 */
fun NfcLocation.isHorizontal(): Boolean = orientation == 0
fun NfcLocation.isOnTheBack(): Boolean = z == 0
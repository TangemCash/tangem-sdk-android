package com.tangem.common.apdu

import com.tangem.common.extensions.hexToBytes

/**
 * Created by Anton Zhilenkov on 27.02.2023.
 * Application Protocol Data Unit
 */
object Apdu {

    /**
     * Select command (ISO7816)
     */
    const val SELECT = "00A4040008"

    /**
     * AID of the Tangem Wallet
     */
    const val TANGEM_WALLET_AID = "A000000812010208"

    fun build(vararg sequence: String): ByteArray = sequence.joinToString(separator = "").hexToBytes()
}
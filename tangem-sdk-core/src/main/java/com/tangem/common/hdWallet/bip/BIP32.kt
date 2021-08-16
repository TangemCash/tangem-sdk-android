package com.tangem.common.hdWallet.bip

/**
 * Created by Anton Zhilenkov on 06/08/2021.
 */
class BIP32 {
    companion object {
        const val hardenedOffset: Long = 2147483648
        const val hardenedSymbol: String = "'"
        const val alternativeHardenedSymbol: String = "’"
        const val masterKeySymbol: String = "m"
        const val separatorSymbol: String = "/"
    }
}
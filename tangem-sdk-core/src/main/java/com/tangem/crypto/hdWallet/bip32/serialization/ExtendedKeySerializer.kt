package com.tangem.crypto.hdWallet.bip32.serialization

import com.tangem.crypto.NetworkType

class ExtendedKeySerializer {
    enum class Version {
        Public, Private;

        @Suppress("MagicNumber")
        fun getPrefix(networkType: NetworkType): Long {
            return when (this) {
                Public -> when (networkType) {
                    NetworkType.Mainnet -> 0x0488b21eL
                    NetworkType.Testnet -> 0x043587cfL
                }
                Private -> when (networkType) {
                    NetworkType.Mainnet -> 0x0488ADE4L
                    NetworkType.Testnet -> 0x04358394L
                }
            }
        }
    }

    object Constants {
        const val DATA_LENGTH: Int = 78
    }
}

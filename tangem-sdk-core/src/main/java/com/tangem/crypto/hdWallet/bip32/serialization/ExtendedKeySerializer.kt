package com.tangem.crypto.hdWallet.bip32.serialization

import com.tangem.crypto.NetworkType

class ExtendedKeySerializer {
    enum class Version {
        Public, Private;

        // https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki#serialization-format
        // Because of the choice of the version bytes,
        // the Base58 representation will start with "xprv" or "xpub" on mainnet, "tprv" or "tpub" on testnet
        @Suppress("MagicNumber")
        fun getPrefix(networkType: NetworkType): Long {
            return when (this) {
                Public -> when (networkType) {
                    NetworkType.Mainnet -> 0x0488b21eL // xpub
                    NetworkType.Testnet -> 0x043587cfL // tpub
                }
                Private -> when (networkType) {
                    NetworkType.Mainnet -> 0x0488ADE4L // xprv
                    NetworkType.Testnet -> 0x04358394L // tprv
                }
            }
        }
    }

    object Constants {
        const val DATA_LENGTH: Int = 78
    }
}

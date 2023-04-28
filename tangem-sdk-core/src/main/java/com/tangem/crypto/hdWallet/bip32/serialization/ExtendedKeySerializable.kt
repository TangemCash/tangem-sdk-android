package com.tangem.crypto.hdWallet.bip32.serialization

import com.tangem.crypto.NetworkType

interface ExtendedKeySerializable {
    @Throws(Exception::class)
    fun serialize(networkType: NetworkType): String
}

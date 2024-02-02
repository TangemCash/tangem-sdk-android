package com.tangem.crypto.hdWallet.masterkey

import com.tangem.crypto.Bls
import com.tangem.crypto.hdWallet.bip32.ExtendedPrivateKey

internal class Eip2333MasterKeyFactory(private val seed: ByteArray) : MaserKeyFactory {

    @OptIn(ExperimentalUnsignedTypes::class)
    override fun makePrivateKey(): ExtendedPrivateKey {
        val keyData = Bls.makeMasterKey(seed)
        return ExtendedPrivateKey(privateKey = keyData, chainCode = byteArrayOf())
    }
}

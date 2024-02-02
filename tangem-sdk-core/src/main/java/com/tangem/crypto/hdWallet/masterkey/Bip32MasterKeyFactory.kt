package com.tangem.crypto.hdWallet.masterkey

import com.tangem.common.card.EllipticCurve
import com.tangem.crypto.hdWallet.bip32.BIP32
import com.tangem.crypto.hdWallet.bip32.ExtendedPrivateKey

internal class Bip32MasterKeyFactory(private val seed: ByteArray, private val curve: EllipticCurve) : MaserKeyFactory {
    override fun makePrivateKey(): ExtendedPrivateKey {
        return BIP32.makeMasterKey(seed, curve)
    }
}

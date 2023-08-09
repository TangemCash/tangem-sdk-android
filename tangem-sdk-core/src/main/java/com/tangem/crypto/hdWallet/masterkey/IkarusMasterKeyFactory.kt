package com.tangem.crypto.hdWallet.masterkey

import com.tangem.crypto.hdWallet.Slip23
import com.tangem.crypto.hdWallet.bip32.ExtendedPrivateKey

internal class IkarusMasterKeyFactory(
    private val entropy: ByteArray,
    private val passphrase: String,
) : MaserKeyFactory {
    override fun makePrivateKey(): ExtendedPrivateKey {
        return Slip23().makeIkarusMasterKey(entropy, passphrase)
    }
}

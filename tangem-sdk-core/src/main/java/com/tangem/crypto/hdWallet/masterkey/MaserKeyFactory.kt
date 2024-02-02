package com.tangem.crypto.hdWallet.masterkey

import com.tangem.crypto.hdWallet.bip32.ExtendedPrivateKey

interface MaserKeyFactory {
    fun makePrivateKey(): ExtendedPrivateKey
}

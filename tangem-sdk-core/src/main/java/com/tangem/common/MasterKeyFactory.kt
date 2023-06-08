package com.tangem.common

import com.tangem.common.card.EllipticCurve
import com.tangem.crypto.Bls
import com.tangem.crypto.hdWallet.bip32.BIP32
import com.tangem.crypto.hdWallet.bip32.ExtendedPrivateKey
import kotlin.jvm.Throws

internal object MasterKeyFactory {
    @Throws(Exception::class)
    internal fun makePrivateKey(seed: ByteArray, curve: EllipticCurve): ExtendedPrivateKey {
        return when (curve) {
            EllipticCurve.Secp256k1, EllipticCurve.Bip0340, EllipticCurve.Secp256r1, EllipticCurve.Ed25519 ->
                BIP32.makeMasterKey(seed = seed, curve = curve)
            EllipticCurve.Bls12381G2, EllipticCurve.Bls12381G2Aug, EllipticCurve.Bls12381G2Pop ->
                ExtendedPrivateKey(privateKey = Bls.makeMasterKey(seed), chainCode = byteArrayOf())
        }
    }
}

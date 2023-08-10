package com.tangem.crypto.hdWallet.masterkey

import com.tangem.common.CompletionResult
import com.tangem.common.card.EllipticCurve
import com.tangem.crypto.bip39.Mnemonic
import com.tangem.crypto.hdWallet.bip32.ExtendedPrivateKey

class AnyMasterKeyFactory(private val mnemonic: Mnemonic, private val passphrase: String) {

    fun makeMasterKey(curve: EllipticCurve): ExtendedPrivateKey {
        return makeFactory(curve).makePrivateKey()
    }

    private fun makeFactory(curve: EllipticCurve): MaserKeyFactory {
        return when (curve) {
            EllipticCurve.Secp256k1, EllipticCurve.Secp256r1, EllipticCurve.Bip0340, EllipticCurve.Ed25519Slip0010,
            -> Bip32MasterKeyFactory(seed = getSeed(), curve = curve)
            EllipticCurve.Bls12381G2, EllipticCurve.Bls12381G2Aug, EllipticCurve.Bls12381G2Pop,
            -> Eip2333MasterKeyFactory(seed = getSeed())
            EllipticCurve.Ed25519 -> IkarusMasterKeyFactory(entropy = mnemonic.getEntropy(), passphrase = passphrase)
        }
    }

    private fun getSeed(): ByteArray {
        return when (val result = mnemonic.generateSeed(passphrase)) {
            is CompletionResult.Failure -> throw result.error
            is CompletionResult.Success -> result.data
        }
    }
}

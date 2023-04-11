package com.tangem.crypto.hdWallet.bip32

import com.tangem.common.card.EllipticCurve
import com.tangem.crypto.CryptoUtils
import com.tangem.crypto.hdWallet.HDWalletError
import com.tangem.crypto.hmacSha512

/**
 * Created by Anton Zhilenkov on 06/08/2021.
 */
object BIP32 {

    @Suppress("MagicNumber")
    @Throws(Exception::class)
    fun makeMasterKey(seed: ByteArray, curve: EllipticCurve): ExtendedPrivateKey {
        // The seed must be between 128 and 512 bits
        if (seed.size !in 16..64) {
            throw HDWalletError.InvalidSeed
        }

        val keyData = curve.hmacKey.rawValue.toByteArray(Charsets.UTF_8)
        val authenticationCode = keyData.hmacSha512(seed)
        val i = authenticationCode
        val iL = i.copyOfRange(0, 32)
        val iR = i.copyOfRange(32, 64)

        // Verify the key
        // https://github.com/satoshilabs/slips/blob/master/slip-0010.md
        if (curve != EllipticCurve.Ed25519 && !CryptoUtils.isPrivateKeyValid(iL, curve)) {
            return makeMasterKey(i, curve)
        }

        return ExtendedPrivateKey(privateKey = iL, chainCode = iR)
    }

    enum class HMACKey(val rawValue: String) {
        Secp256k1("Bitcoin seed"),
        Secp256r1("Nist256p1 seed"),
        Ed25519("ed25519 seed"),
    }

    object Constants {
        const val hardenedOffset: Long = 2147483648
        const val hardenedSymbol: String = "'"
        const val alternativeHardenedSymbol: String = "’"
        const val masterKeySymbol: String = "m"
        const val separatorSymbol: String = "/"
    }
}

private val EllipticCurve.hmacKey: BIP32.HMACKey
    get() = when (this) {
        EllipticCurve.Secp256k1 -> BIP32.HMACKey.Secp256k1
        EllipticCurve.Ed25519 -> BIP32.HMACKey.Ed25519
        EllipticCurve.Secp256r1 -> BIP32.HMACKey.Secp256r1
    }

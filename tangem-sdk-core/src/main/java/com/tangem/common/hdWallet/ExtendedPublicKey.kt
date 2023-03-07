package com.tangem.common.hdWallet

import com.squareup.moshi.JsonClass
import com.tangem.common.extensions.calculateHashCode
import com.tangem.common.extensions.toByteArray
import com.tangem.common.hdWallet.bip.BIP32
import com.tangem.crypto.Secp256k1
import com.tangem.crypto.hmacSha512
import com.tangem.operations.CommandResponse

/**
 * Created by Anton Zhilenkov on 03/08/2021.
 */
@JsonClass(generateAdapter = true)
class ExtendedPublicKey(
    val publicKey: ByteArray,
    val chainCode: ByteArray,
) : CommandResponse {

    /**
     * This function performs CKDpub((Kpar, cpar), i) → (Ki, ci) to compute a child extended public key from
     * the parent extended public key.
     * It is only defined for non-hardened child keys. `secp256k1` only
     */
    @Suppress("MagicNumber")
    @Throws(HDWalletError::class)
    fun derivePublicKey(node: DerivationNode): ExtendedPublicKey {
        if (publicKey.size != 33) { // secp256k1 only
            throw HDWalletError.UnsupportedCurve
        }
        val index = node.index

        // We can derive only non-hardened keys
        if (index >= BIP32.hardenedOffset) throw HDWalletError.HardenedNotSupported

        val data = publicKey + index.toByteArray(4)
        val i = chainCode.hmacSha512(data).clone()
        val iL = i.sliceArray(0 until 32)
        val chainCode = i.sliceArray(32 until 64)

        val ki = Secp256k1.gMultiplyAndAddPoint(iL, publicKey)
        val derivedPublicKey = ki.getEncoded(true)

        return ExtendedPublicKey(derivedPublicKey, chainCode)
    }

    /**
     * This function performs CKDpub((Kpar, cpar), i) → (Ki, ci) to compute a child extended public key from the
     * parent extended public key.
     * It is only defined for non-hardened child keys. `secp256k1` only
     */
    @Throws(HDWalletError::class)
    fun derivePublicKey(derivationPath: DerivationPath): ExtendedPublicKey {
        var key: ExtendedPublicKey = this
        derivationPath.nodes.forEach {
            key = key.derivePublicKey(it)
        }
        return key
    }

    override fun equals(other: Any?): Boolean {
        val other = other as? ExtendedPublicKey ?: return false

        return publicKey.contentEquals(other.publicKey) &&
            chainCode.contentEquals(other.chainCode)
    }

    override fun hashCode(): Int = calculateHashCode(
        publicKey.contentHashCode(),
        chainCode.contentHashCode(),
    )
}
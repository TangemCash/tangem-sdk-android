package com.tangem.crypto

import com.tangem.common.extensions.calculateSha256
import com.tangem.crypto.schnorr.Schnorr

internal object Bip0340 {
    internal fun verify(publicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean {
        return Schnorr.verify(message.calculateSha256(), publicKey, signature)
    }

    internal fun verifyHash(publicKey: ByteArray, hash: ByteArray, signature: ByteArray): Boolean {
        return Schnorr.verify(hash, publicKey, signature)
    }

    internal fun sign(data: ByteArray, privateKeyArray: ByteArray): ByteArray {
        return Schnorr.sign(data.calculateSha256(), privateKeyArray, byteArrayOf())
    }

    internal fun generatePublicKey(privateKeyArray: ByteArray): ByteArray {
        return Schnorr.generatePublicKey(privateKeyArray)
    }
}

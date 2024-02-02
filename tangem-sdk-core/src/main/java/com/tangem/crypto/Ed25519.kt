package com.tangem.crypto

import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.calculateSha512
import net.i2p.crypto.eddsa.EdDSAEngine
import net.i2p.crypto.eddsa.EdDSAPrivateKey
import net.i2p.crypto.eddsa.EdDSAPublicKey
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec
import java.security.MessageDigest
import java.security.PublicKey

object Ed25519 {

    private const val ED25519_PRIVATE_KEY_SIZE = 32

    internal fun verify(publicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean {
        val messageSha512 = message.calculateSha512()
        return verifyHash(publicKey, messageSha512, signature)
    }

    internal fun verifyHash(publicKey: ByteArray, hash: ByteArray, signature: ByteArray): Boolean {
        val loadedPublicKey = loadPublicKey(publicKey)
        val spec = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519)
        val signatureInstance = EdDSAEngine(MessageDigest.getInstance(spec.hashAlgorithm))
        signatureInstance.initVerify(loadedPublicKey)
        signatureInstance.update(hash)
        return signatureInstance.verify(signature)
    }

    internal fun loadPublicKey(publicKeyArray: ByteArray): PublicKey {
        val spec = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519)
        val pubKey = EdDSAPublicKeySpec(publicKeyArray, spec)
        return EdDSAPublicKey(pubKey)
    }

    internal fun sign(data: ByteArray, privateKeyArray: ByteArray): ByteArray {
        val dataSha512 = data.calculateSha512()
        val spec = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519)
        val signatureInstance = EdDSAEngine(MessageDigest.getInstance(spec.hashAlgorithm))

        val privateKeySpec = EdDSAPrivateKeySpec(privateKeyArray, spec)
        val privateKey = EdDSAPrivateKey(privateKeySpec)

        signatureInstance.initSign(privateKey)
        signatureInstance.update(dataSha512)

        return signatureInstance.sign()
    }

    internal fun generatePublicKey(privateKeyArray: ByteArray): ByteArray {
        if (privateKeyArray.size > ED25519_PRIVATE_KEY_SIZE) {
            throw TangemSdkError.UnsupportedCurve()
        }
        val spec = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519)
        val privateKeySpec = EdDSAPrivateKeySpec(privateKeyArray, spec)
        val publicKeySpec = EdDSAPublicKeySpec(privateKeySpec.a, spec)
        val publicKey = EdDSAPublicKey(publicKeySpec)
        return publicKey.abyte
    }

    internal fun isPrivateKeyValid(privateKey: ByteArray): Boolean {
        return try {
            val dummyMessage = byteArrayOf(0x00)
            val signature = sign(dummyMessage, privateKey)
            verify(generatePublicKey(privateKey), dummyMessage, signature)
        } catch (e: Exception) {
            false
        }
    }
}

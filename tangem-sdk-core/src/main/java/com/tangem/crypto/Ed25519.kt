package com.tangem.crypto

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

    internal fun verify(publicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean {
        val messageSha512 = message.calculateSha512()
        val loadedPublicKey = loadPublicKey(publicKey)
        val spec = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519)
        val signatureInstance = EdDSAEngine(MessageDigest.getInstance(spec.hashAlgorithm))
        signatureInstance.initVerify(loadedPublicKey)

        signatureInstance.update(messageSha512)

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
        val spec = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519)
        val privateKeySpec = EdDSAPrivateKeySpec(privateKeyArray, spec)
        val publicKeySpec = EdDSAPublicKeySpec(privateKeySpec.a, spec)
        val publicKey = EdDSAPublicKey(publicKeySpec)
        return publicKey.abyte
    }
}
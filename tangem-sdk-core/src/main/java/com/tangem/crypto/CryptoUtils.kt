package com.tangem.crypto

import com.tangem.common.KeyPair
import com.tangem.common.card.EllipticCurve
import com.tangem.common.core.TangemSdkError
import net.i2p.crypto.eddsa.EdDSASecurityProvider
import org.spongycastle.crypto.digests.SHA512Digest
import org.spongycastle.crypto.generators.PKCS5S2ParametersGenerator
import org.spongycastle.crypto.macs.HMac
import org.spongycastle.crypto.params.KeyParameter
import org.spongycastle.jce.provider.BouncyCastleProvider
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Security
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {

    var isInitialized = false
        private set

    fun initCrypto() {
        if (isInitialized) return
        isInitialized = true

        Security.addProvider(BouncyCastleProvider())
        Security.addProvider(EdDSASecurityProvider())
    }

    /**
     * Generates ByteArray of random bytes.
     * It is used, among other things, to generate helper private keys
     * (not the one for the blockchains, that one is generated on the card and does not leave the card).
     *
     * @param length length of the ByteArray that is to be generated.
     */
    fun generateRandomBytes(length: Int): ByteArray {
        val bytes = ByteArray(length)
        SecureRandom().nextBytes(bytes)
        return bytes
    }

    /**
     * Helper function to verify that the data was signed with a private key that corresponds
     * to the provided public key.
     *
     * @param publicKey Corresponding to the private key that was used to sing a message
     * @param message The data that was signed
     * @param signature Signed data
     * @param curve Elliptic curve used
     *
     * @return Result of a verification
     */
    fun verify(
        publicKey: ByteArray,
        message: ByteArray,
        signature: ByteArray,
        curve: EllipticCurve = EllipticCurve.Secp256k1,
    ): Boolean {
        return when (curve) {
            EllipticCurve.Secp256k1 -> Secp256k1.verify(publicKey, message, signature)
            EllipticCurve.Secp256r1 -> Secp256r1.verify(publicKey, message, signature)
            EllipticCurve.Ed25519 -> Ed25519.verify(publicKey, message, signature)
        }
    }

    /**
     * Helper function that generates public key from a private key.
     *
     * @param privateKey  A private key from which a public key is generated
     * @param curve Elliptic curve used
     *
     * @return Public key [ByteArray]
     */
    fun generatePublicKey(
        privateKey: ByteArray,
        curve: EllipticCurve = EllipticCurve.Secp256k1,
        compressed: Boolean = false,
    ): ByteArray {
        return when (curve) {
            EllipticCurve.Secp256k1 -> Secp256k1.generatePublicKey(privateKey, compressed)
            EllipticCurve.Secp256r1 -> Secp256r1.generatePublicKey(privateKey, compressed)
            EllipticCurve.Ed25519 -> Ed25519.generatePublicKey(privateKey)
        }
    }

    fun loadPublicKey(publicKey: ByteArray, curve: EllipticCurve = EllipticCurve.Secp256k1): PublicKey {
        return when (curve) {
            EllipticCurve.Secp256k1 -> Secp256k1.loadPublicKey(publicKey)
            EllipticCurve.Secp256r1 -> Secp256r1.loadPublicKey(publicKey)
            EllipticCurve.Ed25519 -> Ed25519.loadPublicKey(publicKey)
        }
    }

    fun compressPublicKey(key: ByteArray, curve: EllipticCurve = EllipticCurve.Secp256k1): ByteArray {
        return when (curve) {
            EllipticCurve.Secp256k1 -> Secp256k1.compressPublicKey(key)
            else -> throw UnsupportedOperationException()
        }
    }

    fun decompressPublicKey(key: ByteArray, curve: EllipticCurve = EllipticCurve.Secp256k1): ByteArray {
        return when (curve) {
            EllipticCurve.Secp256k1 -> Secp256k1.decompressPublicKey(key)
            else -> throw UnsupportedOperationException()
        }
    }

    fun normalize(signature: ByteArray, curve: EllipticCurve = EllipticCurve.Secp256k1): ByteArray {
        return when (curve) {
            EllipticCurve.Secp256k1 -> Secp256k1.normalize(signature)
            else -> throw UnsupportedOperationException()
        }
    }

    fun isPrivateKeyValid(privateKey: ByteArray, curve: EllipticCurve = EllipticCurve.Secp256k1): Boolean {
        return when (curve) {
            EllipticCurve.Secp256k1 -> Secp256k1.isPrivateKeyValid(privateKey)
            EllipticCurve.Secp256r1 -> Secp256r1.isPrivateKeyValid(privateKey)
            EllipticCurve.Ed25519 -> Ed25519.isPrivateKeyValid(privateKey)
        }
    }
}

fun Secp256k1.generateKeyPair(): KeyPair {
    val privateKey = CryptoUtils.generateRandomBytes(length = 32)
    val publicKey = CryptoUtils.generatePublicKey(privateKey)
    return KeyPair(publicKey, privateKey)
}

/**
 * Extension function to sign a ByteArray with an elliptic curve cryptography.
 *
 * @param privateKeyArray Key to sign data
 * @param curve Elliptic curve that is used to sign data
 *
 * @return Signed data
 */
fun ByteArray.sign(privateKeyArray: ByteArray, curve: EllipticCurve = EllipticCurve.Secp256k1): ByteArray {
    return when (curve) {
        EllipticCurve.Secp256k1 -> Secp256k1.sign(this, privateKeyArray)
        EllipticCurve.Secp256r1 -> Secp256r1.sign(this, privateKeyArray)
        EllipticCurve.Ed25519 -> Ed25519.sign(this, privateKeyArray)
    }
}

@Suppress("MagicNumber")
fun ByteArray.encrypt(key: ByteArray, usePkcs7: Boolean = true): ByteArray {
    val spec = if (usePkcs7) ENCRYPTION_SPEC_PKCS7 else ENCRYPTION_SPEC_NO_PADDING
    val secretKeySpec = SecretKeySpec(key, spec)
    val cipher = Cipher.getInstance(spec, "SC")
    cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, IvParameterSpec(ByteArray(16)))
    return cipher.doFinal(this)
}

@Suppress("MagicNumber")
fun ByteArray.decrypt(key: ByteArray, usePkcs7: Boolean = true): ByteArray {
    val spec = if (usePkcs7) ENCRYPTION_SPEC_PKCS7 else ENCRYPTION_SPEC_NO_PADDING
    val secretKeySpec = SecretKeySpec(key, spec)
    val cipher = Cipher.getInstance(spec)
    cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, IvParameterSpec(ByteArray(16)))
    return cipher.doFinal(this.copyOfRange(0, this.size))
}

fun ByteArray.pbkdf2Hash(salt: ByteArray, iterations: Int): ByteArray {
    return Pbkdf2().deriveKey(this, salt, iterations)
}

@Throws(TangemSdkError.KeyGenerationException::class)
fun ByteArray.pbkdf2sha512(salt: ByteArray, iterations: Int, keyByteCount: Int = 64): ByteArray {
    val generator = PKCS5S2ParametersGenerator(SHA512Digest())
    generator.init(this, salt, iterations)
    val key = generator.generateDerivedMacParameters(keyByteCount * BYTE_SIZE) as? KeyParameter
    return key?.key ?: throw TangemSdkError.KeyGenerationException(customMessage = "pbkdf2sha512 generation")
}

fun ByteArray.hmacSha512(input: ByteArray): ByteArray {
    val key = this
    val hMac = HMac(SHA512Digest())

    hMac.init(KeyParameter(key))
    hMac.update(input, 0, input.size)

    val out = ByteArray(size = 64)
    hMac.doFinal(out, 0)

    return out
}

private const val ENCRYPTION_SPEC_PKCS7 = "AES/CBC/PKCS7PADDING"
private const val ENCRYPTION_SPEC_NO_PADDING = "AES/CBC/NOPADDING"
private const val BYTE_SIZE = 8

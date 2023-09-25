package com.tangem.crypto.operations

import com.tangem.Log
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.Cipher
import javax.crypto.SecretKey

/**
 * Utility object to handle RSA cipher operations.
 */
object RSACipherOperations {

    private const val TAG = "RSA Cipher Operations"

    private const val KEY_ALGORITHM = "RSA"
    private const val KEY_BLOCK_MODE = "ECB"
    private const val KEY_PADDING = "OAEPPadding"

    private val cipherInstance: Cipher
        get() = Cipher.getInstance("$KEY_ALGORITHM/$KEY_BLOCK_MODE/$KEY_PADDING")

    /**
     * Unwraps a wrapped secret key using the given [cipher] instance.
     *
     * @param cipher The initialized cipher instance to use for unwrapping.
     * @param wrappedKeyBytes The wrapped bytes of the secret key.
     * @param wrappedKeyAlgorithm The algorithm of the secret key.
     * @return The unwrapped [SecretKey].
     */
    fun unwrapKey(cipher: Cipher, wrappedKeyBytes: ByteArray, wrappedKeyAlgorithm: String): SecretKey {
        Log.debug {
            """
                $TAG - Unwrapping a secret key
                |- Wrapped key bytes: $wrappedKeyBytes
                |- Wrapped key algorithm: $wrappedKeyAlgorithm
            """.trimIndent()
        }

        return cipher.unwrap(wrappedKeyBytes, wrappedKeyAlgorithm, Cipher.SECRET_KEY) as SecretKey
    }

    /**
     * Wraps a [secretKey] using the given [cipher] instance.
     *
     * @param cipher The initialized cipher instance to use for wrapping.
     * @param secretKey The secret key to wrap.
     * @return The wrapped key in byte array format.
     */
    fun wrapKey(cipher: Cipher, secretKey: SecretKey): ByteArray {
        Log.debug { "$TAG - Wrapping a secret key" }

        return cipher.wrap(secretKey)
    }

    /**
     * Initializes the cipher for wrapping a secret key using the provided [publicKey].
     *
     * @param publicKey The public key to use for initialization.
     * @return The initialized [Cipher] instance in wrap mode.
     */
    fun initWrapKeyCipher(publicKey: PublicKey): Cipher {
        Log.debug { "$TAG - Initializing the cipher for a secret key wrapping" }

        return cipherInstance.apply { init(Cipher.WRAP_MODE, publicKey) }
    }

    /**
     * Initializes the cipher for unwrapping a secret key using the provided [privateKey].
     *
     * @param privateKey The private key to use for initialization.
     * @return The initialized [Cipher] instance in unwrap mode.
     */
    fun initUnwrapKeyCipher(privateKey: PrivateKey): Cipher {
        Log.debug { "$TAG - Initializing the cipher for a secret key unwrapping" }

        return cipherInstance.apply { init(Cipher.UNWRAP_MODE, privateKey) }
    }

    /**
     * Generates a key pair using the provided [keyGenSpec] and the specified [keyStoreProvider].
     *
     * @param keyGenSpec The specifications to use when generating the key pair.
     * @param keyStoreProvider The provider for the key store.
     * @return The generated [KeyPair].
     */
    fun generateKeyPair(keyGenSpec: AlgorithmParameterSpec, keyStoreProvider: String): KeyPair {
        Log.debug { "$TAG - Generating a key pair" }

        return KeyPairGenerator.getInstance(KEY_ALGORITHM, keyStoreProvider)
            .also { it.initialize(keyGenSpec) }
            .generateKeyPair()
    }
}

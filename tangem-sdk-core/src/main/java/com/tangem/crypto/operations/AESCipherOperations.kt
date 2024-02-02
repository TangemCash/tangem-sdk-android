package com.tangem.crypto.operations

import com.tangem.Log
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

/**
 * Utility object to handle AES cipher operations.
 */
object AESCipherOperations {

    private const val TAG = "AES Cipher Operations"

    const val KEY_ALGORITHM = "AES"
    private const val KEY_BLOCK_MODE = "CBC"
    private const val KEY_PADDING = "PKCS7PADDING"

    private val cipherInstance: Cipher
        get() = Cipher.getInstance("$KEY_ALGORITHM/$KEY_BLOCK_MODE/$KEY_PADDING")

    /**
     * Encrypts the provided data using the given [cipher] instance.
     *
     * @param cipher The initialized cipher instance to use for encryption.
     * @param decryptedData The data to encrypt.
     *
     * @return The encrypted data in byte array format.
     */
    fun encrypt(cipher: Cipher, decryptedData: ByteArray): ByteArray {
        Log.debug {
            """
                $TAG - Encrypting
                |- Decrypted data: $decryptedData
            """.trimIndent()
        }

        return cipher.doFinal(decryptedData)
    }

    /**
     * Decrypts the provided data using the given [cipher] instance.
     *
     * @param cipher The initialized cipher instance to use for decryption.
     * @param encryptedData The data to decrypt.
     *
     * @return The decrypted data in byte array format.
     */
    fun decrypt(cipher: Cipher, encryptedData: ByteArray): ByteArray {
        Log.debug {
            """
                $TAG - Decrypting
                |- Encrypted data: $encryptedData
            """.trimIndent()
        }

        return cipher.doFinal(encryptedData)
    }

    /**
     * Initializes the cipher for encrypting data using the provided [key].
     *
     * @param key The secret key to use for initialization.
     *
     * @return The initialized [Cipher] instance in encrypt mode.
     */
    fun initEncryptionCipher(key: SecretKey): Cipher {
        Log.debug { "$TAG - Initializing the cipher for encryption" }

        return cipherInstance.apply { init(Cipher.ENCRYPT_MODE, key) }
    }

    /**
     * Initializes the cipher for decrypting data using the provided [key].
     *
     * @param key The secret key to use for initialization.
     * @param iv The initialization vector to use during decryption.
     *        This ensures that the same plaintext encrypted with the same key, but with a
     *        different initialization vector, will produce a different ciphertext.
     * @return The initialized [Cipher] instance in decrypt mode.
     */
    fun initDecryptionCipher(key: SecretKey, iv: ByteArray): Cipher {
        Log.debug { "$TAG - Initializing the cipher for decryption" }

        return cipherInstance.apply { init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv)) }
    }

    /**
     * Generates an AES [SecretKey] with the specified [keySize].
     *
     * @param keySize The size of the key in bits. Default is 256 bits.
     * @return The generated [SecretKey].
     */
    fun generateKey(keySize: Int = 256): SecretKey {
        return KeyGenerator.getInstance(KEY_ALGORITHM)
            .also { it.init(keySize) }
            .generateKey()
    }
}

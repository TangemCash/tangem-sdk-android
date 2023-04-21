package com.tangem.sdk.biometrics

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import com.tangem.common.services.secure.SecureStorage
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import kotlin.properties.Delegates

@RequiresApi(Build.VERSION_CODES.M)
internal class HybridCryptographyManager(
    private val secureStorage: SecureStorage,
) {
    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(keyStoreProvider)
    }

    private val masterCipherInstance: Cipher by lazy {
        Cipher.getInstance("$masterKeyAlgorithm/$masterKeyBlockMode/$masterKeyPadding")
    }

    private val dataCipherInstance: Cipher by lazy {
        Cipher.getInstance("$dataKeyAlgorithm/$dataKeyBlockMode/$dataKeyPadding")
    }

    private val masterKeyGenSpecBuilder: KeyGenParameterSpec.Builder by lazy {
        KeyGenParameterSpec.Builder(
            masterKeyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setKeySize(masterKeySize)
            .setBlockModes(masterKeyBlockMode)
            .setEncryptionPaddings(masterKeyPadding)
            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA1)
            .setUserAuthenticationRequired(true)
            .let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    it.setInvalidatedByBiometricEnrollment(true)
                } else {
                    it
                }
            }
            .let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    it.setUserAuthenticationParameters(keyTimeoutSeconds, KeyProperties.AUTH_BIOMETRIC_STRONG)
                } else {
                    it.setUserAuthenticationValidityDurationSeconds(keyTimeoutSeconds)
                }
            }
    }

    private val masterPublicKey: PublicKey
        get() {
            keyStore.load(null)
            return keyStore.getCertificate(masterKeyAlias)?.publicKey ?: generateMasterKey().public
        }

    private val masterPrivateKey: PrivateKey
        get() {
            keyStore.load(null)
            return keyStore.getKey(masterKeyAlias, null) as? PrivateKey
                ?: throw KeyPermanentlyInvalidatedException()
        }

    private var cryptoOperation: (keyAlias: String, data: ByteArray) -> ByteArray by Delegates.notNull()

    /**
     * Initializes the encryption operation by initializing a master cipher instance with the master public key.
     */
    fun initEncryption() {
        val masterCipher = masterCipherInstance.also { it.init(Cipher.WRAP_MODE, masterPublicKey) }

        cryptoOperation = { keyAlias, data ->
            val encryptionCipher = initDataEncryptionCipher(keyAlias, masterCipher)

            encryptionCipher.doFinal(data)
                .also {
                    secureStorage.store(encryptionCipher.iv, StorageKey.DataInitializationVector(keyAlias).name)
                }
        }
    }

    /**
     * Initializes the decryption operation by initializing a master cipher instance with the master private key
     *
     * Requires user authentication
     * */
    fun initDecryption() {
        val masterCipher = masterCipherInstance.also { it.init(Cipher.UNWRAP_MODE, masterPrivateKey) }

        cryptoOperation = { keyAlias, data ->
            val iv = requireNotNull(secureStorage.get(StorageKey.DataInitializationVector(keyAlias).name)) {
                "Unable to find IV for data decryption"
            }
            val decryptionCipher = initDataDecryptionCipher(keyAlias, iv, masterCipher)

            decryptionCipher.doFinal(data)
        }
    }

    /**
     * Invokes initialized cryptography operation
     *
     * @param keyAlias String alias used to receive or generate cryptography key for [data]
     * @param data [ByteArray] that will be encrypted or decrypted
     *
     * @return Encrypted or decrypted [ByteArray]
     * */
    fun invoke(keyAlias: String, data: ByteArray): ByteArray {
        return cryptoOperation.invoke(keyAlias, data)
    }

    /**
     * Deletes the master key
     * */
    fun deleteMasterKey() {
        keyStore.load(null)
        keyStore.deleteEntry(masterKeyAlias)
    }

    private fun initDataEncryptionCipher(dataKeyAlias: String, masterCipher: Cipher): Cipher {
        val key = generateAndStoreDataKey(dataKeyAlias, masterCipher)
        return dataCipherInstance
            .apply { init(Cipher.ENCRYPT_MODE, key) }
    }

    private fun initDataDecryptionCipher(dataKeyAlias: String, dataIv: ByteArray, masterCipher: Cipher): Cipher {
        val key = getDataKey(dataKeyAlias, masterCipher)
        return dataCipherInstance
            .apply { init(Cipher.DECRYPT_MODE, key, IvParameterSpec(dataIv)) }
    }

    private fun generateAndStoreDataKey(dataKeyAlias: String, masterCipher: Cipher): SecretKey {
        val dataKey = generateDataKey()

        val wrappedKeyBytes = masterCipher.wrap(dataKey)
        secureStorage.store(wrappedKeyBytes, StorageKey.DataKey(dataKeyAlias).name)

        return dataKey
    }

    private fun getDataKey(dataKeyAlias: String, masterCipher: Cipher): SecretKey {
        val wrappedKeyBytes = requireNotNull(secureStorage.get(StorageKey.DataKey(dataKeyAlias).name)) {
            "Unable to find wrapped data key for data decryption"
        }

        return masterCipher.unwrap(wrappedKeyBytes, dataKeyAlgorithm, Cipher.SECRET_KEY) as SecretKey
    }

    private fun generateMasterKey(): KeyPair {
        return KeyPairGenerator.getInstance(masterKeyAlgorithm, keyStoreProvider)
            .also { it.initialize(masterKeyGenSpecBuilder.build()) }
            .generateKeyPair()
    }

    private fun generateDataKey(): SecretKey {
        return KeyGenerator.getInstance(dataKeyAlgorithm)
            .also { it.init(dataKeySize) }
            .generateKey()
    }

    sealed interface StorageKey {
        val name: String

        class DataInitializationVector(keyAlias: String) : StorageKey {
            override val name: String = "data_iv_$keyAlias"
        }

        class DataKey(keyAlias: String) : StorageKey {
            override val name: String = "data_key_$keyAlias"
        }
    }

    companion object {
        @androidx.annotation.IntRange(from = 1)
        private const val keyTimeoutSeconds = 5
        private const val keyStoreProvider = "AndroidKeyStore"

        private const val masterKeyAlias = "master_key"
        private const val masterKeySize = 1024
        private const val masterKeyAlgorithm = KeyProperties.KEY_ALGORITHM_RSA
        private const val masterKeyBlockMode = KeyProperties.BLOCK_MODE_ECB
        private const val masterKeyPadding = KeyProperties.ENCRYPTION_PADDING_RSA_OAEP

        private const val dataKeySize = 256
        private const val dataKeyAlgorithm = KeyProperties.KEY_ALGORITHM_AES
        private const val dataKeyBlockMode = KeyProperties.BLOCK_MODE_CBC
        private const val dataKeyPadding = KeyProperties.ENCRYPTION_PADDING_PKCS7
    }
}

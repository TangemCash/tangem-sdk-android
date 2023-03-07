package com.tangem.sdk.biometrics

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import com.tangem.common.services.secure.SecureStorage
import com.tangem.crypto.CryptoUtils
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

@RequiresApi(Build.VERSION_CODES.M)
internal class EncryptionManager(
    private val secureStorage: SecureStorage,
) {
    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(keyStoreProvider)
            .also { it.load(null) }
    }

    /**
     * Cipher used for data keys encryption/decryption with [masterPublicKey] and [masterPrivateKey]
     * */
    private val masterCipher: Cipher by lazy {
        Cipher.getInstance("$masterKeyAlgorithm/$masterKeyBlockMode/$masterKeyPadding")
    }

    /**
     * Cipher used for provided data encryption/decryption with data keys
     * */
    private val dataCipher: Cipher by lazy {
        Cipher.getInstance("$dataKeyAlgorithm/$dataKeyBlockMode/$dataKeyPadding")
    }

    /**
     * Public key used for data keys encryption, not requires user authentication
     * */
    private val masterPublicKey: PublicKey
        get() {
            generateMasterKeyIfNeeded()
            return keyStore.getCertificate(masterKeyAlias).publicKey
        }

    /**
     * Private key used for data keys decryption, requires user authentication (Biometry)
     * */
    private val masterPrivateKey: PrivateKey
        get() {
            generateMasterKeyIfNeeded()
            return keyStore.getKey(masterKeyAlias, null) as PrivateKey
        }

    /**
     * Generate new secret key with provided [keyAlias] and encrypt [decryptedData] with it
     * */
    fun encrypt(keyAlias: String, decryptedData: ByteArray): ByteArray {
        val key = generateAndStoreDataKey(keyAlias)
        val encryptedData = dataCipher
            .also { it.init(Cipher.ENCRYPT_MODE, key) }
            .doFinal(decryptedData)

        secureStorage.store(dataCipher.iv, StorageKey.DataIv(keyAlias).name)

        return encryptedData
    }

    /**
     * Find secret key with provided [keyAlias] and cipher IV, then decrypt [encryptedData]
     * */
    fun decrypt(keyAlias: String, encryptedData: ByteArray): ByteArray {
        val key = getDataKey(keyAlias)
        val iv = secureStorage.get(StorageKey.DataIv(keyAlias).name)
            ?: error("IV for key $keyAlias is null")

        return dataCipher
            .also { it.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv)) }
            .doFinal(encryptedData)
    }

    /**
     * Generate new asymmetric master key which uses for data keys encryption/decryption
     *
     * Attention, this will make all encrypted data inaccessible
     * */
    fun regenerateMasterKey() {
        keyStore.deleteEntry(masterKeyAlias)
        generateMasterKey()
    }

    /**
     * Generate new asymmetric master key which uses for data keys encryption/decryption if it has not already been
     * generated
     * */
    private fun generateMasterKeyIfNeeded() {
        if (!keyStore.containsAlias(masterKeyAlias)) {
            generateMasterKey()
        }
    }

    /**
     * Generate new asymmetric master key which uses for data keys encryption/decryption
     * */
    private fun generateMasterKey() {
        KeyPairGenerator.getInstance(masterKeyAlgorithm, keyStoreProvider)
            .also { it.initialize(createMasterKeyGenParameterSpec()) }
            .generateKeyPair()
    }

    /**
     * Find data key in [secureStorage] with provided [keyAlias] and decrypt it with [masterPrivateKey]
     * */
    private fun getDataKey(keyAlias: String): SecretKey {
        val encryptedKeyBytes = secureStorage.get(StorageKey.DataKey(keyAlias).name)
            ?: error("Key $keyAlias has not been generated")
        val decryptedKeyBytes = masterCipher
            .also { it.init(Cipher.DECRYPT_MODE, masterPrivateKey) }
            .doFinal(encryptedKeyBytes)

        return SecretKeySpec(decryptedKeyBytes, dataKeyAlgorithm)
    }

    /**
     * Generate random data key with [keyAlias], encrypt it with [masterPublicKey] and store in [secureStorage]
     * */
    private fun generateAndStoreDataKey(keyAlias: String): SecretKey {
        val keyBytes = CryptoUtils.generateRandomBytes(length = dataKeySize.div(other = 8))
        val encryptedKeyBytes = masterCipher
            .also { it.init(Cipher.ENCRYPT_MODE, masterPublicKey) }
            .doFinal(keyBytes)

        secureStorage.store(encryptedKeyBytes, StorageKey.DataKey(keyAlias).name)

        return SecretKeySpec(keyBytes, dataKeyAlgorithm)
    }

    private fun createMasterKeyGenParameterSpec(): KeyGenParameterSpec {
        return KeyGenParameterSpec.Builder(
            masterKeyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(masterKeySize)
            .setBlockModes(masterKeyBlockMode)
            .setEncryptionPaddings(masterKeyPadding)
            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA1)
            .setUserAuthenticationRequired(true)
            .setUserAuthenticationParameters()
            .build()
    }

    private fun KeyGenParameterSpec.Builder.setUserAuthenticationParameters(): KeyGenParameterSpec.Builder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            this
                .setInvalidatedByBiometricEnrollment(true)
                .setUserAuthenticationParameters(
                    keyTimeoutSeconds,
                    KeyProperties.AUTH_BIOMETRIC_STRONG
                )
        } else {
            this.setUserAuthenticationValidityDurationSeconds(keyTimeoutSeconds)
        }
    }

    sealed interface StorageKey {
        val name: String

        class DataIv(keyAlias: String) : StorageKey {
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

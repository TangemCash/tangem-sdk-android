package com.tangem.tangem_sdk_new.biometrics

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import com.tangem.common.services.secure.SecureStorage
import com.tangem.crypto.CryptoUtils
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

@RequiresApi(Build.VERSION_CODES.M)
internal class EncryptionManager(
    private val secureStorage: SecureStorage,
) {
    private val keyGenSpecBuilder by lazy(mode = LazyThreadSafetyMode.NONE) {
        KeyGenParameterSpec.Builder(
            authenticationKeyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(authenticationKeySize)
            .setBlockModes(blockMode)
            .setEncryptionPaddings(encryptionPadding)
            .setUserAuthenticationRequired(true)
            .setUserAuthenticationParameters()
    }

    private val keyStore: KeyStore by lazy(mode = LazyThreadSafetyMode.NONE) {
        KeyStore.getInstance(keyStoreProvider)
            .also { it.load(null) }
    }

    private val cipher: Cipher by lazy(mode = LazyThreadSafetyMode.NONE) {
        Cipher.getInstance("$algorithm/$blockMode/$encryptionPadding")
    }

    private val authenticationKey: SecretKey by lazy(mode = LazyThreadSafetyMode.NONE) {
        keyStore.getKey(authenticationKeyAlias, null) as SecretKey
    }

    private val authenticatedKeys: HashMap<String, SecretKey> = hashMapOf()

    fun encrypt(keyName: String, data: ByteArray): ByteArray {
        return authenticatedKeys[keyName]?.let {
            encryptInternal(
                decryptedData = data,
                key = it,
                getIvStorageKey = { encryptedData -> StorageKey.DataIv(encryptedData) },
            )
        }
            ?: error("Authenticated key $keyName is not initialized")
    }

    fun decrypt(keyName: String, data: ByteArray): ByteArray {
        return authenticatedKeys[keyName]?.let {
            decryptInternal(
                encryptedData = data,
                key = it,
                getIvStorageKey = { encryptedData -> StorageKey.DataIv(encryptedData) },
            )
        }
            ?: error("Authenticated key $keyName is not initialized")
    }

    fun unauthenticateSecretKey(keyName: String?) {
        if (keyName == null) {
            authenticatedKeys.clear()
        } else {
            authenticatedKeys.remove(keyName)
        }
    }

    fun authenticateSecretKeysIfNot(names: List<String>) {
        names.forEach { keyName ->
            val authenticatedKey = authenticatedKeys[keyName]
            if (authenticatedKey != null) return
            generateAuthenticationKeyIfNeeded(keyGenSpecBuilder.build())
            authenticatedKeys[keyName] = when (val key = secureStorage.get(StorageKey.AuthenticatedKey(keyName).name)) {
                null -> generateAuthenticatedKey(keyName)
                else -> decryptAuthenticatedKey(keyName, key)
            }
        }
    }

    private fun encryptInternal(
        decryptedData: ByteArray,
        key: SecretKey,
        getIvStorageKey: (ByteArray) -> StorageKey,
    ): ByteArray {
        return cipher
            .also { it.init(Cipher.ENCRYPT_MODE, key) }
            .doFinal(decryptedData)
            .also { encryptedData ->
                secureStorage.store(cipher.iv, getIvStorageKey(encryptedData).name)
            }
    }

    private fun decryptInternal(
        encryptedData: ByteArray,
        key: SecretKey,
        getIvStorageKey: (ByteArray) -> StorageKey,
    ): ByteArray {
        val iv = secureStorage.get(getIvStorageKey(encryptedData).name)
            ?: error("IV must not be null on decrypting")
        val ivParam = IvParameterSpec(iv)
        return cipher
            .also { it.init(Cipher.DECRYPT_MODE, key, ivParam) }
            .doFinal(encryptedData)
    }

    private fun generateAuthenticationKeyIfNeeded(keyGenParameterSpec: KeyGenParameterSpec) {
        if (!keyStore.containsAlias(authenticationKeyAlias)) {
            KeyGenerator.getInstance(algorithm, keyStoreProvider)
                .also { it.init(keyGenParameterSpec) }
                .generateKey()
        }
    }

    private fun decryptAuthenticatedKey(keyName: String, key: ByteArray): SecretKey {
        return SecretKeySpec(
            decryptInternal(
                encryptedData = key,
                key = authenticationKey,
                getIvStorageKey = { StorageKey.AuthenticatedKeyIv(keyName) }
            ),
            algorithm
        )
    }

    private fun generateAuthenticatedKey(keyName: String): SecretKey {
        val key = CryptoUtils.generateRandomBytes(32)
            .also { key ->
                val encryptedKey = encryptInternal(
                    decryptedData = key,
                    key = authenticationKey,
                    getIvStorageKey = { StorageKey.AuthenticatedKeyIv(keyName) }
                )
                secureStorage.store(encryptedKey, StorageKey.AuthenticatedKey(keyName).name)
            }

        return SecretKeySpec(key, algorithm)
    }

    private fun KeyGenParameterSpec.Builder.setUserAuthenticationParameters(): KeyGenParameterSpec.Builder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            this.setUserAuthenticationParameters(
                keyTimeoutSeconds,
                KeyProperties.AUTH_BIOMETRIC_STRONG
            )
        } else {
            this.setUserAuthenticationValidityDurationSeconds(keyTimeoutSeconds)
        }
    }

    sealed interface StorageKey {
        val name: String

        class DataIv(data: ByteArray) : StorageKey {
            override val name: String = "data_iv_${data.size}"
        }
        class AuthenticatedKeyIv(name: String) : StorageKey {
            override val name: String = "authenticated_key_iv_$name"
        }
        class AuthenticatedKey(name: String) : StorageKey {
            override val name: String = "authenticated_key_$name"
        }
    }

    companion object {
        @androidx.annotation.IntRange(from = 1)
        private const val keyTimeoutSeconds = 1
        private const val authenticationKeyAlias = "authentication_key"
        private const val keyStoreProvider = "AndroidKeyStore"
        private const val authenticationKeySize = 256
        private const val algorithm = KeyProperties.KEY_ALGORITHM_AES
        private const val blockMode = KeyProperties.BLOCK_MODE_CBC
        private const val encryptionPadding = KeyProperties.ENCRYPTION_PADDING_PKCS7
    }
}

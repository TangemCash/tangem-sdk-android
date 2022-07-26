package com.tangem.tangem_sdk_new.biometrics

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import com.tangem.common.services.secure.SecureStorage
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

@RequiresApi(Build.VERSION_CODES.M)
class CryptoManager(
    private val keyTimeoutSeconds: Int,
    private val secureStorage: SecureStorage,
) {
    private val keyGenSpecBuilder = KeyGenParameterSpec.Builder(
        secretKeyAlias,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
    )
        .setKeySize(256)
        .setBlockModes(blockMode)
        .setEncryptionPaddings(encryptionPadding)
        .setUserAuthenticationRequired(true)
        .let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                it.setUserAuthenticationParameters(
                    keyTimeoutSeconds,
                    KeyProperties.AUTH_BIOMETRIC_STRONG
                )
            } else {
                it.setUserAuthenticationValidityDurationSeconds(keyTimeoutSeconds)
            }
        }
        .let {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) it
            else it.setInvalidatedByBiometricEnrollment(true)
        }
    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(keyStoreProvider)
            .also { it.load(null) }
    }
    private val cipher: Cipher by lazy {
        Cipher.getInstance("$algorithm/$blockMode/$encryptionPadding")
    }
    private val secretKey: SecretKey by lazy {
        keyStore.getKey(secretKeyAlias, null) as SecretKey
    }

    init {
        generateSecretKey(keyGenSpecBuilder.build())
    }

    fun decrypt(data: ByteArray): ByteArray {
        val iv = secureStorage.get(STORAGE_KEY_IV)
            ?: error("IV must not be null on decrypting")
        val ivParam = IvParameterSpec(iv)

        return cipher
            .also { it.init(Cipher.DECRYPT_MODE, secretKey, ivParam) }
            .doFinal(data)
    }

    fun encrypt(data: ByteArray): ByteArray {
        return cipher
            .also { it.init(Cipher.ENCRYPT_MODE, secretKey) }
            .doFinal(data)
            .also {
                if (cipher.iv != null && secureStorage.get(STORAGE_KEY_IV) == null) {
                    secureStorage.store(cipher.iv, STORAGE_KEY_IV)
                }
            }
    }

    private fun generateSecretKey(keyGenParameterSpec: KeyGenParameterSpec) {
        if (!keyStore.containsAlias(secretKeyAlias)) {
            KeyGenerator.getInstance(algorithm, keyStoreProvider)
                .also { it.init(keyGenParameterSpec) }
                .generateKey()
        }
    }

    companion object {
        private const val secretKeyAlias = "secret_key"
        private const val keyStoreProvider = "AndroidKeyStore"
        private const val algorithm = KeyProperties.KEY_ALGORITHM_AES
        private const val blockMode = KeyProperties.BLOCK_MODE_CBC
        private const val encryptionPadding = KeyProperties.ENCRYPTION_PADDING_PKCS7
        private const val STORAGE_KEY_IV = "cipher_iv"
    }
}

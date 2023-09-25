package com.tangem.sdk.authentication

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.UserNotAuthenticatedException
import androidx.annotation.RequiresApi
import com.tangem.Log
import com.tangem.common.authentication.AuthenticationManager
import com.tangem.common.authentication.KeystoreManager
import com.tangem.common.core.TangemSdkError
import com.tangem.common.services.secure.SecureStorage
import com.tangem.crypto.operations.AESCipherOperations
import com.tangem.crypto.operations.RSACipherOperations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.InvalidKeyException
import java.security.KeyPair
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.Cipher
import javax.crypto.SecretKey

@RequiresApi(Build.VERSION_CODES.M)
internal class AndroidKeystoreManager(
    private val authenticationManager: AuthenticationManager,
    private val secureStorage: SecureStorage,
) : KeystoreManager {

    private val keyStore: KeyStore = KeyStore.getInstance(KEY_STORE_PROVIDER)
        .apply { load(null) }

    private val masterPublicKey: PublicKey
        get() = keyStore.getCertificate(MASTER_KEY_ALIAS)?.publicKey ?: generateMasterKey().public

    private val masterPrivateKey: PrivateKey
        get() = keyStore.getKey(MASTER_KEY_ALIAS, null) as? PrivateKey
            ?: throw TangemSdkError.KeystoreInvalidated(
                cause = IllegalStateException("The master key is not stored in the keystore"),
            )

    override suspend fun authenticateAndGetKey(keyAlias: String): SecretKey? = withContext(Dispatchers.IO) {
        val wrappedKeyBytes = secureStorage.get(getStorageKeyForWrappedSecretKey(keyAlias))
            ?.takeIf { it.isNotEmpty() }

        if (wrappedKeyBytes == null) {
            Log.warning {
                """
                    $TAG - The secret key is not stored
                    |- Key alias: $keyAlias
                """.trimIndent()
            }

            return@withContext null
        }

        val cipher = initUnwrapCipher()
        val unwrappedKey = RSACipherOperations.unwrapKey(
            cipher = cipher,
            wrappedKeyBytes = wrappedKeyBytes,
            wrappedKeyAlgorithm = AESCipherOperations.KEY_ALGORITHM,
        )

        Log.debug {
            """
                $TAG - The secret key was retrieved
                |- Key alias: $keyAlias
            """.trimIndent()
        }

        unwrappedKey
    }

    override suspend fun storeKey(keyAlias: String, key: SecretKey) = withContext(Dispatchers.IO) {
        val masterCipher = RSACipherOperations.initWrapKeyCipher(masterPublicKey)
        val wrappedKey = RSACipherOperations.wrapKey(masterCipher, key)

        secureStorage.store(wrappedKey, getStorageKeyForWrappedSecretKey(keyAlias))

        Log.debug {
            """
                $TAG - The secret key was stored
                |- Key alias: $keyAlias
            """.trimIndent()
        }
    }

    private suspend fun initUnwrapCipher(): Cipher {
        Log.debug { "$TAG - Initializing the unwrap cipher" }

        return try {
            RSACipherOperations.initUnwrapKeyCipher(masterPrivateKey)
        } catch (e: UserNotAuthenticatedException) {
            Log.warning { "$TAG - Unable to initialize the cipher because the user is not authenticated" }

            authenticationManager.authenticate()

            initUnwrapCipherAfterAuthentication()
        }
    }

    private fun initUnwrapCipherAfterAuthentication(): Cipher {
        Log.debug { "$TAG - Reinitializing the unwrap cipher" }

        return try {
            RSACipherOperations.initUnwrapKeyCipher(masterPrivateKey)
        } catch (e: InvalidKeyException) {
            handleInvalidKeyException(e)
        }
    }

    /**
     * If the master key has been invalidated due to new biometric enrollment, the [UserNotAuthenticatedException]
     * will be thrown anyway because the master key has the positive timeout.
     *
     * @see KeyGenParameterSpec.Builder.setInvalidatedByBiometricEnrollment
     * */
    private fun handleInvalidKeyException(e: InvalidKeyException): Nothing {
        Log.error {
            """
                $TAG - Unable to initialize the unwrap cipher because the master key is invalid
                |- Cause: $e
            """.trimIndent()
        }

        keyStore.deleteEntry(MASTER_KEY_ALIAS)
        keyStore.load(null)

        throw TangemSdkError.KeystoreInvalidated(e)
    }

    private fun generateMasterKey(): KeyPair {
        return RSACipherOperations.generateKeyPair(
            keyStoreProvider = keyStore.provider.name,
            keyGenSpec = buildMasterKeyGenSpec(),
        )
    }

    private fun buildMasterKeyGenSpec(): KeyGenParameterSpec {
        return KeyGenParameterSpec.Builder(
            MASTER_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setKeySize(MASTER_KEY_SIZE)
            .setBlockModes(KeyProperties.BLOCK_MODE_ECB)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA1)
            .setUserAuthenticationRequired(true)
            .let { builder ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    builder.setInvalidatedByBiometricEnrollment(true)
                } else {
                    builder
                }
            }
            .let { builder ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    builder
                        .setUnlockedDeviceRequired(true)
                        .setUserAuthenticationParameters(
                            MASTER_KEY_TIMEOUT_SECONDS,
                            KeyProperties.AUTH_BIOMETRIC_STRONG,
                        )
                } else {
                    builder.setUserAuthenticationValidityDurationSeconds(MASTER_KEY_TIMEOUT_SECONDS)
                }
            }
            .build()
    }

    private fun getStorageKeyForWrappedSecretKey(keyAlias: String): String {
        return "data_key_$keyAlias"
    }

    private companion object {
        const val KEY_STORE_PROVIDER = "AndroidKeyStore"

        const val MASTER_KEY_ALIAS = "master_key"
        const val MASTER_KEY_SIZE = 1024
        const val MASTER_KEY_TIMEOUT_SECONDS = 5

        const val TAG = "Keystore Manager"
    }
}

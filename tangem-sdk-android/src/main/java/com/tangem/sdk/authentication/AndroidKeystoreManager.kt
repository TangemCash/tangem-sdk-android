package com.tangem.sdk.authentication

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import com.tangem.Log
import com.tangem.common.authentication.AuthenticationManager
import com.tangem.common.authentication.keystore.KeystoreManager
import com.tangem.common.core.TangemSdkError
import com.tangem.common.services.secure.SecureStorage
import com.tangem.crypto.operations.AESCipherOperations
import com.tangem.crypto.operations.RSACipherOperations
import com.tangem.sdk.authentication.AndroidAuthenticationManager.AndroidAuthenticationParams
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

    override suspend fun get(masterKeyConfig: KeystoreManager.MasterKeyConfig, keyAlias: String): SecretKey? {
        val wrappedKeyBytes = secureStorage.get(getStorageKeyForWrappedSecretKey(keyAlias))
            ?.takeIf { it.isNotEmpty() }

        if (wrappedKeyBytes == null) {
            Log.biometric {
                """
                    The secret key is not stored
                    |- Key alias: $keyAlias
                """.trimIndent()
            }

            return null
        }

        val privateKey = getPrivateMasterKey(masterKeyConfig) ?: return null
        val cipher = authenticateAndInitUnwrapCipher(privateKey, masterKeyConfig)
        val unwrappedKey = RSACipherOperations.unwrapKey(
            cipher = cipher,
            wrappedKeyBytes = wrappedKeyBytes,
            wrappedKeyAlgorithm = AESCipherOperations.KEY_ALGORITHM,
        )

        Log.biometric {
            """
                The secret key was retrieved
                |- Key alias: $keyAlias
            """.trimIndent()
        }

        return unwrappedKey
    }

    override suspend fun get(
        masterKeyConfig: KeystoreManager.MasterKeyConfig,
        keyAliases: Set<String>,
    ): Map<String, SecretKey> {
        val wrappedKeysBytes = keyAliases
            .mapNotNull { keyAlias ->
                val wrappedKeyBytes = secureStorage.get(getStorageKeyForWrappedSecretKey(keyAlias))
                    ?.takeIf { it.isNotEmpty() }
                    ?: return@mapNotNull null

                keyAlias to wrappedKeyBytes
            }
            .toMap()

        if (wrappedKeysBytes.isEmpty()) {
            Log.biometric {
                """
                    The secret keys are not stored
                    |- Key aliases: $keyAliases
                """.trimIndent()
            }

            return emptyMap()
        }

        val privateKey = getPrivateMasterKey(masterKeyConfig) ?: return emptyMap()
        val cipher = authenticateAndInitUnwrapCipher(privateKey, masterKeyConfig)
        val unwrappedKeys = wrappedKeysBytes
            .mapValues { (_, wrappedKeyBytes) ->
                RSACipherOperations.unwrapKey(
                    cipher = cipher,
                    wrappedKeyBytes = wrappedKeyBytes,
                    wrappedKeyAlgorithm = AESCipherOperations.KEY_ALGORITHM,
                )
            }

        Log.biometric {
            """
                The secret keys were retrieved
                |- Key aliases: $keyAliases
            """.trimIndent()
        }

        return unwrappedKeys
    }

    private fun getPrivateMasterKey(masterKeyConfig: KeystoreManager.MasterKeyConfig): PrivateKey? {
        val key = keyStore.getKey(masterKeyConfig.alias, null) as? PrivateKey

        if (key == null) {
            Log.biometric {
                """
                    The private master key is not generated
                    |- Alias: ${masterKeyConfig.alias}
                """.trimIndent()
            }
        }

        return key
    }

    override suspend fun store(masterKeyConfig: KeystoreManager.MasterKeyConfig, keyAlias: String, key: SecretKey) {
        val publicKey = getPublicMasterKey(masterKeyConfig)
        val cipher = RSACipherOperations.initWrapKeyCipher(publicKey)
        val wrappedKey = RSACipherOperations.wrapKey(cipher, key)

        secureStorage.store(wrappedKey, getStorageKeyForWrappedSecretKey(keyAlias))

        Log.biometric {
            """
                The secret key was stored
                |- Key alias: $keyAlias
            """.trimIndent()
        }
    }

    private fun getPublicMasterKey(masterKeyConfig: KeystoreManager.MasterKeyConfig): PublicKey {
        var key = keyStore.getCertificate(masterKeyConfig.alias)?.publicKey

        if (key == null) {
            Log.biometric { "The public master key is not generated" }

            generateInvalidationCheckKey()
            key = generateMasterKey(masterKeyConfig).public!!
        }

        return key
    }

    private suspend fun authenticateAndInitUnwrapCipher(
        privateKey: PrivateKey,
        masterKeyConfig: KeystoreManager.MasterKeyConfig,
    ): Cipher {
        Log.biometric { "Initializing the unwrap cipher" }

        authenticateUser(masterKeyConfig)

        return try {
            RSACipherOperations.initUnwrapKeyCipher(privateKey)
        } catch (e: Throwable) {
            Log.biometric {
                """
                    Unable to initialize the unwrap cipher
                    |- Cause: $e
                """.trimIndent()
            }

            throw e
        }
    }

    private suspend fun authenticateUser(masterKeyConfig: KeystoreManager.MasterKeyConfig) {
        try {
            /**
             * Authentication timeout is reduced by [AUTHENTICATION_TIMEOUT_MULTIPLIER] of the master key timeout
             * to avoid the situation when the master key is invalidated due to the timeout while the user is authenticating.
             * */
            authenticationManager.authenticate(
                params = AndroidAuthenticationParams(
                    cipher = getInvalidationCheckCipher(),
                    timeout = masterKeyConfig.securityDelay * AUTHENTICATION_TIMEOUT_MULTIPLIER,
                ),
            )
        } catch (e: KeyPermanentlyInvalidatedException) {
            handleKeyInvalidationException(masterKeyConfig.alias, e)
        } catch (e: Throwable) {
            Log.biometric {
                """
                    Unable to authenticate the user
                    |- Cause: $e
                """.trimIndent()
            }

            throw e
        }
    }

    private fun getInvalidationCheckCipher(): Cipher {
        var key = keyStore.getKey(INVALIDATION_CHECK_KEY_ALIAS, null) as? SecretKey

        if (key == null) {
            Log.biometric { "The invalidation check key is not generated" }

            key = generateInvalidationCheckKey()
        }

        return AESCipherOperations.initEncryptionCipher(key)
    }

    private fun handleKeyInvalidationException(
        privateKeyAlias: String,
        e: KeyPermanentlyInvalidatedException,
    ): Nothing {
        Log.biometric {
            """
                Unable to initialize the unwrap cipher because the key is permanently invalidated
                |- Cause: $e
            """.trimIndent()
        }

        keyStore.deleteEntry(privateKeyAlias)
        keyStore.load(null)

        throw TangemSdkError.KeystoreInvalidated(e)
    }

    private fun generateMasterKey(masterKeyConfig: KeystoreManager.MasterKeyConfig): KeyPair {
        Log.biometric {
            """
                Generating the master key
                |- Alias: ${masterKeyConfig.alias}
            """.trimIndent()
        }

        return RSACipherOperations.generateKeyPair(
            keyStoreProvider = keyStore.provider.name,
            keyGenSpec = buildMasterKeySpec(masterKeyConfig),
        )
    }

    private fun generateInvalidationCheckKey(): SecretKey {
        Log.biometric { "Generating the key for invalidation check" }

        return AESCipherOperations.generateKey(
            keyStoreProvider = keyStore.provider.name,
            keyGetSpec = buildInvalidationCheckKeySpec(),
        )
    }

    /** Key regeneration is required to edit these parameters */
    private fun buildMasterKeySpec(masterKeyConfig: KeystoreManager.MasterKeyConfig): KeyGenParameterSpec {
        val securityDelaySeconds = masterKeyConfig.securityDelay.inWholeSeconds.toInt()

        return KeyGenParameterSpec.Builder(
            masterKeyConfig.alias,
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
                        .setUserConfirmationRequired(masterKeyConfig.userConfirmationRequired)
                        .setUserAuthenticationParameters(
                            securityDelaySeconds,
                            KeyProperties.AUTH_BIOMETRIC_STRONG,
                        )
                } else {
                    builder.setUserAuthenticationValidityDurationSeconds(securityDelaySeconds)
                }
            }
            .build()
    }

    /** Key regeneration is required to edit these parameters */
    private fun buildInvalidationCheckKeySpec(): KeyGenParameterSpec {
        return KeyGenParameterSpec.Builder(
            INVALIDATION_CHECK_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT,
        )
            .setKeySize(AUTHENTICATION_CHECK_KEY_SIZE)
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
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
                        .setUserConfirmationRequired(false)
                        .setUserAuthenticationParameters(
                            0,
                            KeyProperties.AUTH_BIOMETRIC_STRONG,
                        )
                } else {
                    builder.setUserAuthenticationValidityDurationSeconds(-1)
                }
            }
            .build()
    }

    private fun getStorageKeyForWrappedSecretKey(keyAlias: String): String {
        return "data_key_$keyAlias"
    }

    private companion object {
        const val KEY_STORE_PROVIDER = "AndroidKeyStore"
        const val MASTER_KEY_SIZE = 1024
        const val AUTHENTICATION_TIMEOUT_MULTIPLIER = 0.9

        const val INVALIDATION_CHECK_KEY_ALIAS = "invalidation_check_key"
        const val AUTHENTICATION_CHECK_KEY_SIZE = 128
    }
}

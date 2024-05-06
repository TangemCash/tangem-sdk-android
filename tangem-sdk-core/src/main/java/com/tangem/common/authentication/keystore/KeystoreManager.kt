package com.tangem.common.authentication.keystore

import com.tangem.common.core.TangemSdkError
import javax.crypto.SecretKey
import kotlin.time.Duration

/**
 * Represents a manager for managing and accessing encrypted keys in a secure manner.
 */
interface KeystoreManager {

    /**
     * Retrieves the [SecretKey] for a given [keyAlias].
     *
     * This operation requires user authentication.
     *
     * @param masterKeyConfig The configuration for the master key.
     * @param keyAlias The alias of the key to be retrieved.
     * @param forceAuthentication Whether to force user authentication. If `true`, the user will be prompted
     * to authenticate even if the master key security delay is not reached.
     *
     * @return The [SecretKey] if found. If the key cannot be found or unwrapped, then `null` will be returned.
     *
     * @throws TangemSdkError.KeystoreInvalidated if the keystore is invalidated.
     */
    suspend fun get(masterKeyConfig: MasterKeyConfig, keyAlias: String, forceAuthentication: Boolean): SecretKey?

    /**
     * Retrieves the map of key alias to [SecretKey] for a given [keyAliases].
     *
     * This operation requires user authentication.
     *
     * @param masterKeyConfig The configuration for the master key.
     * @param keyAliases The aliases of the keys to be retrieved.
     * @param forceAuthentication Whether to force user authentication. If `true`, the user will be prompted
     * to authenticate even if the master key security delay is not reached.
     *
     * @return The map of key alias to [SecretKey] if found. If the key cannot be found or unwrapped,
     * then the key will not be included in the map.
     *
     * @throws TangemSdkError.KeystoreInvalidated if the keystore is invalidated.
     */
    suspend fun get(
        masterKeyConfig: MasterKeyConfig,
        keyAliases: Set<String>,
        forceAuthentication: Boolean,
    ): Map<String, SecretKey>

    /**
     * Stores the given [SecretKey] with a specified [keyAlias] in the keystore.
     *
     * @param masterKeyConfig The configuration for the master key.
     * @param keyAlias The alias under which the key should be stored.
     * @param key The [SecretKey] to be stored.
     */
    suspend fun store(masterKeyConfig: MasterKeyConfig, keyAlias: String, key: SecretKey)

    /**
     * The configuration for the master key.
     *
     * @property alias The alias of the master key.
     * @property securityDelaySeconds The delay in seconds before the user is required to authenticate again.
     * @property userConfirmationRequired Whether the user confirmation is required for the authentication.
     * */
    interface MasterKeyConfig {
        val alias: String
        val securityDelay: Duration
        val userConfirmationRequired: Boolean
    }
}

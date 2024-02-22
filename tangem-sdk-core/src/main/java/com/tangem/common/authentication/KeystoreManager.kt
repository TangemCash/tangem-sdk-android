package com.tangem.common.authentication

import com.tangem.common.core.TangemSdkError
import javax.crypto.SecretKey

/**
 * Represents a manager for managing and accessing encrypted keys in a secure manner.
 */
interface KeystoreManager {

    /**
     * Retrieves the [SecretKey] for a given [keyAlias].
     *
     * This operation requires user authentication.
     *
     * @param keyAlias The alias of the key to be retrieved.
     * @return The [SecretKey] if found. If the key cannot be found, then `null` will be returned.
     *
     * @throws TangemSdkError.KeystoreInvalidated if the keystore is invalidated.
     */
    suspend fun get(keyAlias: String): SecretKey?

    /**
     * Retrieves the map of key alias to [SecretKey] for a given [keyAliases].
     *
     * This operation requires user authentication.
     *
     * @param keyAliases The aliases of the keys to be retrieved.
     * @return The map of key alias to [SecretKey] if found. If the key cannot be found, then the key will not be
     * included in the map.
     *
     * @throws TangemSdkError.KeystoreInvalidated if the keystore is invalidated.
     */
    suspend fun get(keyAliases: Collection<String>): Map<String, SecretKey>

    /**
     * Stores the given [SecretKey] with a specified [keyAlias] in the keystore.
     *
     * @param keyAlias The alias under which the key should be stored.
     * @param key The [SecretKey] to be stored.
     */
    suspend fun store(keyAlias: String, key: SecretKey)
}

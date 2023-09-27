package com.tangem.common.authentication

import javax.crypto.SecretKey

/**
 * Represents a manager for managing and accessing encrypted keys in a secure manner.
 */
interface KeystoreManager {

    /**
     * Retrieves the [SecretKey] for a given [keyAlias].
     *
     * This requires user authentication (e.g. biometric authentication).
     *
     * @param keyAlias The alias of the key to be retrieved.
     * @return The [SecretKey] if found. If the keystore is locked or the key cannot be found,
     * then `null` will be returned.
     */
    suspend fun authenticateAndGetKey(keyAlias: String): SecretKey?

    /**
     * Stores the given [SecretKey] with a specified [keyAlias] in the keystore.
     *
     * @param keyAlias The alias under which the key should be stored.
     * @param key The [SecretKey] to be stored.
     */
    suspend fun storeKey(keyAlias: String, key: SecretKey)
}

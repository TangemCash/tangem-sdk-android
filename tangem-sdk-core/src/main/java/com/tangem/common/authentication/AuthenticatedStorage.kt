package com.tangem.common.authentication

import com.tangem.Log
import com.tangem.common.services.secure.SecureStorage
import com.tangem.crypto.operations.AESCipherOperations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.crypto.SecretKey

/**
 * A storage utility class that provides encryption and decryption capabilities for secure data persistence.
 *
 * @property secureStorage A storage utility to fetch and store encrypted data.
 * @property keystoreManager A manager to handle and provide encryption keys.
 */
class AuthenticatedStorage(
    private val secureStorage: SecureStorage,
    private val keystoreManager: KeystoreManager,
) {

    /**
     * Retrieves and decrypts data from the storage after necessary user authentication.
     *
     * @param keyAlias The unique identifier for the stored encrypted data.
     *
     * @return The decrypted data as [ByteArray] or `null` if data is not found.
     */
    suspend fun get(keyAlias: String): ByteArray? = withContext(Dispatchers.IO) {
        val encryptedData = secureStorage.get(keyAlias)
            ?.takeIf(ByteArray::isNotEmpty)

        if (encryptedData == null) {
            Log.warning {
                """
                    $TAG - Data not found in storage
                    |- Key: $keyAlias
                """.trimIndent()
            }

            return@withContext null
        }

        decrypt(keyAlias, encryptedData)
    }

    /**
     * Retrieves and decrypts data from the storage after necessary user authentication.
     *
     * @param keysAliases The unique identifiers for the stored encrypted data.
     *
     * @return The decrypted data as a map of key-alias to [ByteArray] or an empty map if data is not found.
     */
    suspend fun get(keysAliases: Collection<String>): Map<String, ByteArray> = withContext(Dispatchers.IO) {
        val encryptedData = keysAliases
            .mapNotNull { keyAlias ->
                val data = secureStorage.get(keyAlias)
                    ?.takeIf(ByteArray::isNotEmpty)
                    ?: return@mapNotNull null

                keyAlias to data
            }
            .toMap()

        if (encryptedData.isEmpty()) {
            Log.warning {
                """
                    $TAG - Data not found in storage
                    |- Keys: $keysAliases
                """.trimIndent()
            }

            return@withContext emptyMap()
        }

        decrypt(encryptedData)
    }

    /**
     * Encrypts and stores data securely in the storage.
     *
     * @param keyAlias The unique identifier which will be associated with the encrypted data.
     * @param data The plain data to be encrypted and stored.
     */
    suspend fun store(keyAlias: String, data: ByteArray) = withContext(Dispatchers.IO) {
        val encryptedData = encrypt(keyAlias, data)

        secureStorage.store(encryptedData, keyAlias)
    }

    /**
     * Deletes the encrypted data associated with the provided key from storage.
     *
     * @param key The unique identifier of the encrypted data to be deleted.
     */
    fun delete(key: String) {
        secureStorage.delete(key)
    }

    private suspend fun encrypt(keyAlias: String, decryptedData: ByteArray): ByteArray {
        val key = generateAndStoreDataKey(keyAlias)
        val encryptionCipher = AESCipherOperations.initEncryptionCipher(key)
        val encryptedData = AESCipherOperations.encrypt(encryptionCipher, decryptedData)

        storeDataIv(keyAlias, encryptionCipher.iv)

        return encryptedData
    }

    private suspend fun decrypt(keyAlias: String, encryptedData: ByteArray): ByteArray? {
        val key = keystoreManager.get(keyAlias)

        if (key == null) {
            Log.warning {
                """
                    $TAG - The data key is not stored
                    |- Key alias: $keyAlias
                """.trimIndent()
            }

            return null
        }

        val iv = getDataIv(keyAlias)
        val decryptionCipher = AESCipherOperations.initDecryptionCipher(key, iv)

        return AESCipherOperations.decrypt(decryptionCipher, encryptedData)
    }

    private suspend fun decrypt(keyAliasToEncryptedData: Map<String, ByteArray>): Map<String, ByteArray> {
        val keys = keystoreManager.get(keyAliasToEncryptedData.keys)

        if (keys.isEmpty()) {
            Log.warning {
                """
                    $TAG - The data keys are not stored
                    |- Key aliases: ${keyAliasToEncryptedData.keys}
                """.trimIndent()
            }

            return emptyMap()
        }

        return keyAliasToEncryptedData
            .mapNotNull { (keyAlias, encryptedData) ->
                val key = keys[keyAlias] ?: return@mapNotNull null
                val iv = getDataIv(keyAlias)
                val decryptionCipher = AESCipherOperations.initDecryptionCipher(key, iv)
                val decryptedData = AESCipherOperations.decrypt(decryptionCipher, encryptedData)

                keyAlias to decryptedData
            }
            .toMap()
    }

    private suspend fun generateAndStoreDataKey(keyAlias: String): SecretKey {
        val dataKey = AESCipherOperations.generateKey()

        keystoreManager.store(keyAlias, dataKey)

        return dataKey
    }

    private fun getDataIv(dataKeyAlias: String): ByteArray {
        val iv = secureStorage.get(getStorageKeyForDataIv(dataKeyAlias))

        return requireNotNull(iv) {
            val msg = "Unable to find IV for data decryption: $dataKeyAlias"

            Log.error { "$TAG - $msg" }
            msg
        }
    }

    private fun storeDataIv(dataKeyAlias: String, initializationVector: ByteArray) {
        secureStorage.store(
            data = initializationVector,
            account = getStorageKeyForDataIv(dataKeyAlias),
        )
    }

    private fun getStorageKeyForDataIv(dataKeyAlias: String): String = "data_iv_$dataKeyAlias"

    private companion object {
        const val TAG = "Authenticated Storage"
    }
}

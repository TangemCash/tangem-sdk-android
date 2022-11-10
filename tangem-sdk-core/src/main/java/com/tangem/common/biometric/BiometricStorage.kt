package com.tangem.common.biometric

import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.common.map
import com.tangem.common.services.secure.SecureStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BiometricStorage(
    private val biometricManager: BiometricManager,
    private val secureStorage: SecureStorage,
) {
    suspend fun get(key: String): CompletionResult<ByteArray?> {
        if (!biometricManager.canAuthenticate)
            return CompletionResult.Failure(TangemSdkError.BiometricsUnavailable())

        val decryptedData = withContext(Dispatchers.IO) { secureStorage.get(key) }
            ?.takeIf { it.isNotEmpty() }
            ?: return CompletionResult.Success(null)
        val mode = BiometricManager.AuthenticationMode.Decryption(decryptedData)
        return biometricManager.authenticate(mode).map { it }
    }

    suspend fun store(key: String, data: ByteArray): CompletionResult<Unit> {
        if (!biometricManager.canAuthenticate)
            return CompletionResult.Failure(TangemSdkError.BiometricsUnavailable())

        val mode = BiometricManager.AuthenticationMode.Encryption(data)
        return biometricManager.authenticate(mode = mode)
            .map { encryptedData ->
                withContext(Dispatchers.IO) {
                    secureStorage.store(encryptedData, key)
                }
            }
    }

    suspend fun delete(key: String): CompletionResult<Unit> {
        withContext(Dispatchers.IO) { secureStorage.delete(key) }
        return CompletionResult.Success(Unit)
    }
}

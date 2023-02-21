package com.tangem.common.biometric

import com.tangem.common.CompletionResult

interface BiometricManager {
    val canAuthenticate: Boolean
    val canEnrollBiometrics: Boolean

    suspend fun authenticate(mode: AuthenticationMode): CompletionResult<ByteArray>

    sealed interface AuthenticationMode {
        class Encryption(
            val keyName: String,
            val data: ByteArray,
        ) : AuthenticationMode

        class Decryption(
            val keyName: String,
            val data: ByteArray,
        ) : AuthenticationMode
    }
}

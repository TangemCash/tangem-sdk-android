package com.tangem.common.biometric

import com.tangem.common.CompletionResult

interface BiometricManager {
    val canAuthenticate: Boolean
    val canEnrollBiometrics: Boolean

    suspend fun authenticate(mode: AuthenticationMode): CompletionResult<ByteArray>

    sealed interface AuthenticationMode {
        val keyName: String
        val data: ByteArray

        class Encryption(
            override val keyName: String,
            override val data: ByteArray,
        ) : AuthenticationMode

        class Decryption(
            override val keyName: String,
            override val data: ByteArray,
        ) : AuthenticationMode
    }
}

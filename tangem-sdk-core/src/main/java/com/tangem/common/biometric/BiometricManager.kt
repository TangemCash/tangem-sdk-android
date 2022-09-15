package com.tangem.common.biometric

import com.tangem.common.CompletionResult

interface BiometricManager {
    val canAuthenticate: Boolean

    suspend fun authenticate(mode: AuthenticationMode): CompletionResult<ByteArray>

    fun unauthenticate()

    sealed interface AuthenticationMode {
        val data: ByteArray

        class Encryption(
            override val data: ByteArray,
        ) : AuthenticationMode

        class Decryption(
            override val data: ByteArray,
        ) : AuthenticationMode
    }
}

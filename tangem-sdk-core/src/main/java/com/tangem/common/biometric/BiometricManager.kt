package com.tangem.common.biometric

import com.tangem.common.CompletionResult

interface BiometricManager {
    val canAuthenticate: Boolean
    val canEnrollBiometrics: Boolean

    suspend fun authenticate(mode: AuthenticationMode): CompletionResult<ByteArray>

    fun unauthenticate(keyName: String? = null)

    sealed interface AuthenticationMode {
        val keys: List<String>

        class Keys(
            vararg keys: String
        ) : AuthenticationMode {
            override val keys: List<String> = keys.toList()
        }

        class Encryption(
            val keyName: String,
            val data: ByteArray,
        ) : AuthenticationMode {
            override val keys: List<String> = listOf(keyName)
        }

        class Decryption(
            val keyName: String,
            val data: ByteArray,
        ) : AuthenticationMode {
            override val keys: List<String> = listOf(keyName)
        }
    }
}

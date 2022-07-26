package com.tangem.common.biometric

import com.tangem.common.CompletionResult

class DummyBiometricManager : BiometricManager {
    override val canAuthenticate: Boolean
        get() = false

    override suspend fun authenticate(mode: BiometricManager.AuthenticationMode): CompletionResult<ByteArray> {
        return CompletionResult.Success(byteArrayOf())
    }
}

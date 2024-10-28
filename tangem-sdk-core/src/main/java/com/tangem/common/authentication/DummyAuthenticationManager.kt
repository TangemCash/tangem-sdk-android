package com.tangem.common.authentication

import javax.crypto.Cipher

class DummyAuthenticationManager : AuthenticationManager {

    override val isInitialized: Boolean = true

    override val canAuthenticate: Boolean = false

    override val needEnrollBiometrics: Boolean = false

    override suspend fun authenticate(
        params: AuthenticationManager.AuthenticationParams,
    ): AuthenticationManager.AuthenticationResult = object : AuthenticationManager.AuthenticationResult {
        override val cipher: Cipher? = null
    }
}

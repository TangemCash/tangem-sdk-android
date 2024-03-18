package com.tangem.common.authentication

import javax.crypto.Cipher

class DummyAuthenticationManager : AuthenticationManager {
    override val canAuthenticate: Boolean
        get() = false

    override val needEnrollBiometrics: Boolean
        get() = false

    override suspend fun authenticate(
        params: AuthenticationManager.AuthenticationParams,
    ): AuthenticationManager.AuthenticationResult = object : AuthenticationManager.AuthenticationResult {
        override val cipher: Cipher? = null
    }
}

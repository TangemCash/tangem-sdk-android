package com.tangem.common.authentication

class DummyAuthenticationManager : AuthenticationManager {
    override val canAuthenticate: Boolean
        get() = false

    override val canEnrollBiometrics: Boolean
        get() = false

    override suspend fun authenticate() = Unit
}

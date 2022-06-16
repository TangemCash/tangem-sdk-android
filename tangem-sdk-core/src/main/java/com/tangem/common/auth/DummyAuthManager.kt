package com.tangem.common.auth

class DummyAuthManager : AuthManager {
    override val canAuthenticate: Boolean
        get() = false

    override fun authenticate(block: (isAuthenticated: Boolean) -> Unit) {
        /* no-op */
    }
}

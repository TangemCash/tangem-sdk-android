package com.tangem.jvm

import com.tangem.common.biomteric.AuthManager

internal class DummyAuthManager : AuthManager {
    override val canAuthenticate: Boolean
        get() = false

    override fun authenticate(block: (isAuthenticated: Boolean) -> Unit) {
        /* no-op */
    }
}

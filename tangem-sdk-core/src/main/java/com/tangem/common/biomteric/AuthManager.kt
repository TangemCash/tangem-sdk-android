package com.tangem.common.biomteric

interface AuthManager {
    val canAuthenticate: Boolean

    fun authenticate(block: (isAuthenticated: Boolean) -> Unit)
}

package com.tangem.common.auth

interface AuthManager {
    val canAuthenticate: Boolean

    fun authenticate(block: (isAuthenticated: Boolean) -> Unit)
}

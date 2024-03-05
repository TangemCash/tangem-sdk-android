package com.tangem.common.authentication

import com.tangem.common.core.TangemSdkError

/**
 * Represents a manager that handles authentication processes.
 */
interface AuthenticationManager {

    /**
     * Checks if authentication is possible. Returns `true` if there is biometric
     * data enrolled and the hardware is available.
     */
    val canAuthenticate: Boolean

    /**
     * Checks if biometrics can be enrolled. Returns `true` if the hardware is available
     * but there is no biometric data enrolled.
     */
    val needEnrollBiometrics: Boolean

    /**
     * Initiates the authentication process. Depending on the implementation,
     * this might trigger biometric prompts or other authentication mechanisms.
     *
     * @throws TangemSdkError.AuthenticationUnavailable if authentication is unavailable.
     * @throws TangemSdkError.UserCanceledAuthentication if the user cancels the authentication process.
     * @throws TangemSdkError.AuthenticationFailed if authentication fails for any other reason.
     */
    suspend fun authenticate()
}

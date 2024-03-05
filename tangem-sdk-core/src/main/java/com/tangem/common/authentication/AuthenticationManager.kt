package com.tangem.common.authentication

import com.tangem.common.core.TangemSdkError
import kotlin.time.Duration

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
     * @param params [AuthenticationParams] that specify the authentication process.
     *
     * @throws TangemSdkError.AuthenticationUnavailable if authentication is unavailable.
     * @throws TangemSdkError.AuthenticationCanceled if the authentication was cancelled.
     * @throws TangemSdkError.AuthenticationFailed if authentication fails for any other reason.
     */
    suspend fun authenticate(params: AuthenticationParams)

    /**
     * Parameters that specify the authentication process.
     *
     * @property timeout The maximum time to wait for the authentication to complete. If it's reached, the
     * authentication process will be cancelled.
     */
    interface AuthenticationParams {
        val timeout: Duration?
    }
}

package com.tangem.common.authentication

import com.tangem.common.core.TangemSdkError
import javax.crypto.Cipher
import kotlin.time.Duration

/**
 * Represents a manager that handles authentication processes.
 */
interface AuthenticationManager {

    /**
     * Checks if the authentication manager is initialized.
     * */
    val isInitialized: Boolean

    /**
     * Checks if authentication is possible. Returns `true` if there is biometric
     * data enrolled and the hardware is available.
     *
     * @throws TangemSdkError.AuthenticationNotInitialized if the authentication manager is not initialized or paused.
     *
     * @see isInitialized
     */
    val canAuthenticate: Boolean

    /**
     * Checks if biometrics can be enrolled. Returns `true` if the hardware is available
     * but there is no biometric data enrolled.
     *
     * @throws TangemSdkError.AuthenticationNotInitialized if the authentication manager is not initialized or paused.
     *
     * @see isInitialized
     */
    val needEnrollBiometrics: Boolean

    /**
     * Initiates the authentication process. Depending on the implementation,
     * this might trigger biometric prompts or other authentication mechanisms.
     *
     * @param params [AuthenticationParams] that specify the authentication process.
     *
     * @return [AuthenticationResult] that contains the successful result of the authentication process.
     *
     * @throws TangemSdkError.AuthenticationAlreadyInProgress if another authentication process is already in progress.
     * @throws TangemSdkError.AuthenticationUnavailable if authentication is unavailable.
     * @throws TangemSdkError.AuthenticationCanceled if the authentication was cancelled.
     * @throws TangemSdkError.AuthenticationFailed if authentication fails for any other reason.
     * @throws TangemSdkError.AuthenticationNotInitialized if the authentication manager is not initialized or paused.
     *
     * @see isInitialized
     */
    suspend fun authenticate(params: AuthenticationParams): AuthenticationResult

    /**
     * Parameters that specify the authentication process.
     *
     * @property cipher The cipher that will be checked with authentication and can be used to encrypt or decrypt data.
     * @property timeout The maximum time to wait for the authentication to complete. If it's reached, the
     * authentication process will be cancelled.
     */
    interface AuthenticationParams {
        val cipher: Cipher?
        val timeout: Duration?
    }

    /**
     * The successful result of the authentication process.
     *
     * @property cipher The cipher that can be used to encrypt or decrypt data.
     */
    interface AuthenticationResult {
        val cipher: Cipher?
    }
}

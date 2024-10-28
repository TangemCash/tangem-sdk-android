package com.tangem.sdk.authentication

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.tangem.Log
import com.tangem.common.authentication.AuthenticationManager
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.sdk.R
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.crypto.Cipher
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import androidx.biometric.BiometricManager as SystemBiometricManager

@RequiresApi(Build.VERSION_CODES.M)
internal class AndroidAuthenticationManager(
    private val activity: FragmentActivity,
) : AuthenticationManager,
    DefaultLifecycleObserver,
    LifecycleOwner by activity {

    private val biometricPromptInfo by lazy {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.biometric_prompt_title))
            .setNegativeButtonText(activity.getString(R.string.common_cancel))
            .setAllowedAuthenticators(SystemBiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setConfirmationRequired(false)
            .build()
    }

    private val biometricsStatus = MutableStateFlow(BiometricsStatus.NOT_INITIALIZED)

    override val isInitialized: Boolean
        get() = biometricsStatus.value != BiometricsStatus.NOT_INITIALIZED

    override val canAuthenticate: Boolean
        get() {
            if (!isInitialized) {
                Log.biometric {
                    "Biometrics status must be initialized before checking if biometrics can authenticate"
                }

                throw TangemSdkError.AuthenticationNotInitialized()
            }

            return biometricsStatus.value == BiometricsStatus.READY
        }

    override val needEnrollBiometrics: Boolean
        get() {
            if (!isInitialized) {
                Log.biometric {
                    "Biometrics status must be initialized before checking if biometrics need to be enrolled"
                }

                throw TangemSdkError.AuthenticationNotInitialized()
            }

            return biometricsStatus.value == BiometricsStatus.NEED_ENROLL
        }

    override fun onResume(owner: LifecycleOwner) {
        owner.lifecycleScope.launch {
            biometricsStatus.value = getBiometricsAvailabilityStatus()

            Log.biometric { "Owner has been resumed, biometrics status: ${biometricsStatus.value}" }
        }
    }

    override suspend fun authenticate(
        params: AuthenticationManager.AuthenticationParams,
    ): AuthenticationManager.AuthenticationResult {
        return when (biometricsStatus.value) {
            BiometricsStatus.READY -> {
                Log.biometric { "Authenticating a user: $params" }

                val timeout = params.timeout
                if (timeout != null) {
                    authenticateWithTimeout(timeout, params.cipher)
                } else {
                    authenticate(params.cipher)
                }
            }
            BiometricsStatus.AUTHENTICATING -> {
                Log.biometric { "A user authentication has already been launched" }

                throw TangemSdkError.AuthenticationAlreadyInProgress()
            }
            BiometricsStatus.NEED_ENROLL -> {
                Log.biometric { "Unable to authenticate the user as the biometrics feature must be enrolled" }

                throw TangemSdkError.AuthenticationUnavailable()
            }
            BiometricsStatus.UNAVAILABLE -> {
                Log.biometric { "Unable to authenticate the user as the biometrics feature is unavailable" }

                throw TangemSdkError.AuthenticationUnavailable()
            }
            BiometricsStatus.NOT_INITIALIZED -> {
                Log.biometric { "Unable to authenticate the user as the biometrics feature is not initialized" }

                throw TangemSdkError.AuthenticationNotInitialized()
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun authenticateWithTimeout(timeout: Duration, cipher: Cipher?): AndroidAuthenticationResult {
        return withTimeoutOrNull(timeout) { authenticate(cipher) }
            ?: throw TangemSdkError.AuthenticationCanceled()
    }

    private suspend fun authenticate(cipher: Cipher?): AndroidAuthenticationResult = withContext(Dispatchers.Main) {
        biometricsStatus.value = BiometricsStatus.AUTHENTICATING

        val promptResult = suspendCancellableCoroutine { continuation ->
            val biometricPrompt = createBiometricPrompt(continuation)

            if (cipher == null) {
                biometricPrompt.authenticate(biometricPromptInfo)
            } else {
                biometricPrompt.authenticate(biometricPromptInfo, BiometricPrompt.CryptoObject(cipher))
            }

            continuation.invokeOnCancellation {
                Log.biometric { "User authentication has been canceled" }

                biometricPrompt.cancelAuthentication()

                biometricsStatus.value = BiometricsStatus.READY
            }
        }

        AndroidAuthenticationResult(promptResult.cryptoObject?.cipher)
    }

    private fun createBiometricPrompt(continuation: CancellableContinuation<BiometricPrompt.AuthenticationResult>) =
        BiometricPrompt(
            activity,
            createAuthenticationCallback { biometricResult ->
                biometricsStatus.value = BiometricsStatus.READY

                when (biometricResult) {
                    is BiometricAuthenticationResult.Failure -> {
                        continuation.resumeWithException(biometricResult.error)
                    }
                    is BiometricAuthenticationResult.Success -> {
                        continuation.resume(biometricResult.result)
                    }
                }
            },
        )

    @Suppress("LongMethod")
    private suspend fun getBiometricsAvailabilityStatus(): BiometricsStatus {
        val biometricManager = SystemBiometricManager.from(activity)

        return suspendCancellableCoroutine { continuation ->
            when (biometricManager.canAuthenticate(AUTHENTICATORS)) {
                SystemBiometricManager.BIOMETRIC_SUCCESS -> {
                    Log.biometric { "Biometric features are available" }
                    continuation.resume(BiometricsStatus.READY)
                }
                SystemBiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                    Log.biometric { "No biometric features enrolled" }
                    continuation.resume(BiometricsStatus.NEED_ENROLL)
                }
                SystemBiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                    Log.biometric { "No biometric features available on this device" }
                    continuation.resume(BiometricsStatus.UNAVAILABLE)
                }
                SystemBiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                    Log.biometric { "Biometric features are currently unavailable" }
                    continuation.resume(BiometricsStatus.UNAVAILABLE)
                }
                SystemBiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                    Log.biometric { "Biometric features are currently unavailable, security update required" }
                    continuation.resume(BiometricsStatus.UNAVAILABLE)
                }
                SystemBiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
                    Log.biometric { "Biometric features are in unknown status" }
                    continuation.resume(BiometricsStatus.UNAVAILABLE)
                }
                SystemBiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
                    Log.biometric { "Biometric features are unsupported" }
                    continuation.resume(BiometricsStatus.UNAVAILABLE)
                }
            }
        }
    }

    private inline fun createAuthenticationCallback(
        crossinline result: (BiometricAuthenticationResult) -> Unit,
    ): BiometricPrompt.AuthenticationCallback {
        return object : BiometricPrompt.AuthenticationCallback() {

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                val error = when (errorCode) {
                    BiometricPrompt.ERROR_LOCKOUT -> TangemSdkError.AuthenticationLockout()
                    BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> TangemSdkError.AuthenticationPermanentLockout()
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    -> TangemSdkError.AuthenticationCanceled()

                    else -> TangemSdkError.AuthenticationFailed(errorCode, errString.toString())
                }

                Log.biometric {
                    """
                        Biometric authentication error
                        |- Code: $errorCode
                        |- Message: $errString
                        |- Cause: $error
                    """.trimIndent()
                }

                result(BiometricAuthenticationResult.Failure(error))
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                Log.biometric { "Biometric authentication succeed" }
                result(BiometricAuthenticationResult.Success(result))
            }

            override fun onAuthenticationFailed() {
                Log.biometric { "Biometric authentication failed" }
            }
        }
    }

    data class AndroidAuthenticationParams(
        override val cipher: Cipher? = null,
        override val timeout: Duration? = null,
    ) : AuthenticationManager.AuthenticationParams

    data class AndroidAuthenticationResult(
        override val cipher: Cipher?,
    ) : AuthenticationManager.AuthenticationResult

    private enum class BiometricsStatus {
        READY,
        AUTHENTICATING,
        UNAVAILABLE,
        NEED_ENROLL,
        NOT_INITIALIZED,
    }

    private sealed interface BiometricAuthenticationResult {
        data class Failure(
            val error: TangemError,
        ) : BiometricAuthenticationResult

        data class Success(
            val result: BiometricPrompt.AuthenticationResult,
        ) : BiometricAuthenticationResult
    }

    private companion object {
        const val AUTHENTICATORS = SystemBiometricManager.Authenticators.BIOMETRIC_STRONG
    }
}

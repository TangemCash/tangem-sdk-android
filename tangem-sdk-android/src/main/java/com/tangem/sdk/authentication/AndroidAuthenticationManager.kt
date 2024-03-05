package com.tangem.sdk.authentication

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.tangem.Log
import com.tangem.common.authentication.AuthenticationManager
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.sdk.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
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
            .build()
    }

    private val authenticationMutex = Mutex()
    private val canAuthenticateInternal: MutableStateFlow<Boolean?> = MutableStateFlow(value = null)
    private val needEnrollBiometricsInternal: MutableStateFlow<Boolean?> = MutableStateFlow(value = null)

    override val canAuthenticate: Boolean
        get() = requireNotNull(canAuthenticateInternal.value) {
            "`canAuthenticate` has not been initialized"
        }
    override val needEnrollBiometrics: Boolean
        get() = requireNotNull(needEnrollBiometricsInternal.value) {
            "`needEnrollBiometrics` has not been initialized"
        }

    override fun onResume(owner: LifecycleOwner) {
        owner.lifecycleScope.launch {
            val (available, canBeEnrolled) = checkBiometricsAvailabilityStatus()

            canAuthenticateInternal.value = available
            needEnrollBiometricsInternal.value = canBeEnrolled
        }
    }

    override suspend fun authenticate() {
        if (lifecycle.currentState != Lifecycle.State.RESUMED) return

        if (authenticationMutex.isLocked) {
            Log.warning { "$TAG - A user authentication has already been launched" }
            Log.biometric { "A user authentication has already been launched" }
            return
        }

        authenticationMutex.withLock {
            Log.debug { "$TAG - Trying to authenticate a user" }
            Log.biometric { "Try to authenticate a user" }

            val canAuthenticate = canAuthenticateInternal.filterNotNull().first()
            if (canAuthenticate) {
                withContext(Dispatchers.Main) {
                    authenticateInternal()
                }
            } else {
                Log.warning { "$TAG - Unable to authenticate the user as the biometrics feature is unavailable" }
                Log.biometric { "Unable to authenticate the user as the biometrics feature is unavailable" }

                throw TangemSdkError.AuthenticationUnavailable()
            }
        }
    }

    private suspend fun authenticateInternal() {
        return suspendCancellableCoroutine { continuation ->
            val biometricPrompt = BiometricPrompt(
                activity,
                createAuthenticationCallback { biometricResult ->
                    when (biometricResult) {
                        is BiometricAuthenticationResult.Failure -> {
                            continuation.resumeWithException(biometricResult.error)
                        }
                        is BiometricAuthenticationResult.Success -> {
                            continuation.resume(Unit)
                        }
                    }
                },
            )

            biometricPrompt.authenticate(biometricPromptInfo)
        }
    }

    @Suppress("LongMethod")
    private suspend fun checkBiometricsAvailabilityStatus(): BiometricsAvailability {
        val biometricManager = SystemBiometricManager.from(activity)

        return suspendCancellableCoroutine { continuation ->
            when (biometricManager.canAuthenticate(AUTHENTICATORS)) {
                SystemBiometricManager.BIOMETRIC_SUCCESS -> {
                    Log.debug { "$TAG - Biometric features are available" }
                    Log.biometric { "Biometric features are available" }
                    continuation.resume(
                        BiometricsAvailability(
                            available = true,
                            canBeEnrolled = false,
                        ),
                    )
                }

                SystemBiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                    Log.debug { "$TAG - No biometric features enrolled" }
                    Log.biometric { "No biometric features enrolled" }
                    continuation.resume(
                        BiometricsAvailability(
                            available = false,
                            canBeEnrolled = true,
                        ),
                    )
                }
                SystemBiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                    Log.debug { "$TAG - No biometric features available on this device" }
                    Log.biometric { "No biometric features available on this device" }
                    continuation.resume(
                        BiometricsAvailability(
                            available = false,
                            canBeEnrolled = false,
                        ),
                    )
                }
                SystemBiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                    Log.debug { "$TAG - Biometric features are currently unavailable" }
                    Log.biometric { "Biometric features are currently unavailable" }
                    continuation.resume(
                        BiometricsAvailability(
                            available = false,
                            canBeEnrolled = false,
                        ),
                    )
                }
                SystemBiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                    Log.debug { "$TAG - Biometric features are currently unavailable, security update required" }
                    Log.biometric { "Biometric features are currently unavailable, security update required" }
                    continuation.resume(
                        BiometricsAvailability(
                            available = false,
                            canBeEnrolled = false,
                        ),
                    )
                }
                SystemBiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
                    Log.debug { "$TAG - Biometric features are in unknown status" }
                    Log.biometric { "Biometric features are in unknown status" }
                    continuation.resume(
                        BiometricsAvailability(
                            available = false,
                            canBeEnrolled = false,
                        ),
                    )
                }
                SystemBiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
                    Log.debug { "$TAG - Biometric features are unsupported" }
                    Log.biometric { "Biometric features are unsupported" }
                    continuation.resume(
                        BiometricsAvailability(
                            available = false,
                            canBeEnrolled = false,
                        ),
                    )
                }
            }
        }
    }

    private fun createAuthenticationCallback(
        result: (BiometricAuthenticationResult) -> Unit,
    ): BiometricPrompt.AuthenticationCallback {
        return object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                val error = when (errorCode) {
                    BiometricPrompt.ERROR_LOCKOUT -> TangemSdkError.AuthenticationLockout()
                    BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> TangemSdkError.AuthenticationPermanentLockout()
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    -> TangemSdkError.UserCanceledAuthentication()

                    else -> TangemSdkError.AuthenticationFailed(errorCode, errString.toString())
                }

                Log.warning {
                    """
                        $TAG - Biometric authentication error
                        |- Code: $errorCode
                        |- Message: $errString
                        |- Cause: $error
                    """.trimIndent()
                }
                Log.biometric { "Biometric authentication error:\n$error" }

                result(BiometricAuthenticationResult.Failure(error))
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                Log.debug { "$TAG - Biometric authentication succeed" }
                Log.biometric { "Biometric authentication succeed" }
                result(BiometricAuthenticationResult.Success(result))
            }

            override fun onAuthenticationFailed() {
                Log.warning { "$TAG - Biometric authentication failed" }
                Log.biometric { "Biometric authentication failed" }
            }
        }
    }

    private data class BiometricsAvailability(
        val available: Boolean,
        val canBeEnrolled: Boolean,
    )

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

        const val TAG = "Android Authentication Manager"
    }
}

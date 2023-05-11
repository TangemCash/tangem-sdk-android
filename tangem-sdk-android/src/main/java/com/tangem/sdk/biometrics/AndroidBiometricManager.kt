package com.tangem.sdk.biometrics

import android.os.Build
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.UserNotAuthenticatedException
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.tangem.Log
import com.tangem.common.CompletionResult
import com.tangem.common.biometric.BiometricManager
import com.tangem.common.catching
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.common.flatMapOnFailure
import com.tangem.common.map
import com.tangem.common.mapFailure
import com.tangem.common.services.secure.SecureStorage
import com.tangem.sdk.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.InvalidKeyException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import androidx.biometric.BiometricManager as SystemBiometricManager

// TODO: Add docs
// TODO: Refactoring needed
@RequiresApi(Build.VERSION_CODES.M)
internal class AndroidBiometricManager(
    secureStorage: SecureStorage,
    private val activity: FragmentActivity,
) : BiometricManager,
    DefaultLifecycleObserver,
    LifecycleOwner by activity {

    private val cryptographyManager by lazy {
        HybridCryptographyManager(secureStorage)
    }

    private val biometricPromptInfo by lazy {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.biometric_prompt_title))
            .setNegativeButtonText(activity.getString(R.string.common_cancel))
            .setAllowedAuthenticators(SystemBiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
    }

    override var canAuthenticate: Boolean = false
        private set
    override var canEnrollBiometrics: Boolean = false
        private set

    override fun onResume(owner: LifecycleOwner) {
        lifecycleScope.launch {
            val (available, canBeEnrolled) = checkBiometricsAvailabilityStatus()
            canAuthenticate = available
            canEnrollBiometrics = canBeEnrolled
        }
    }

    override suspend fun authenticate(mode: BiometricManager.AuthenticationMode): CompletionResult<ByteArray> {
        return if (canAuthenticate) {
            initCrypto(mode)
                .map { cryptographyManager.invoke(mode.keyName, mode.data) }
                .mapFailure { error ->
                    when (val cause = (error as? TangemSdkError.ExceptionError)?.cause) {
                        is KeyPermanentlyInvalidatedException,
                        is UserNotAuthenticatedException,
                        -> {
                            cryptographyManager.deleteMasterKey()
                            TangemSdkError.BiometricCryptographyKeyInvalidated()
                        }

                        is InvalidKeyException -> {
                            TangemSdkError.InvalidBiometricCryptographyKey(cause.localizedMessage.orEmpty(), cause)
                        }

                        is TangemSdkError.UserCanceledBiometricsAuthentication -> {
                            error
                        }

                        else -> {
                            TangemSdkError.BiometricCryptographyOperationFailed(error.customMessage, error)
                        }
                    }
                }
        } else {
            CompletionResult.Failure(TangemSdkError.BiometricsUnavailable())
        }
    }

    private suspend fun authenticateInternal(): CompletionResult<Unit> {
        return suspendCoroutine { continuation ->
            val biometricPrompt = BiometricPrompt(
                activity,
                createAuthenticationCallback { result ->
                    continuation.resume(
                        value = when (result) {
                            is AuthenticationResult.Failure -> CompletionResult.Failure(result.error)
                            is AuthenticationResult.Success -> CompletionResult.Success(Unit)
                        },
                    )
                },
            )

            biometricPrompt.authenticate(biometricPromptInfo)
        }
    }

    private suspend fun initCrypto(mode: BiometricManager.AuthenticationMode): CompletionResult<Unit> {
        return when (mode) {
            is BiometricManager.AuthenticationMode.Decryption -> {
                catching(cryptographyManager::initDecryption).flatMapOnFailure { error ->
                    if ((error as? TangemSdkError.ExceptionError)?.cause is UserNotAuthenticatedException) {
                        withContext(Dispatchers.Main) { authenticateInternal() }
                            .map { cryptographyManager.initDecryption() }
                    } else {
                        CompletionResult.Failure(error)
                    }
                }
            }
            is BiometricManager.AuthenticationMode.Encryption -> {
                catching { cryptographyManager.initEncryption() }
            }
        }
    }

    @Suppress("LongMethod")
    private suspend fun checkBiometricsAvailabilityStatus(): BiometricsAvailability {
        val biometricManager = SystemBiometricManager.from(activity)
        val authenticators = SystemBiometricManager.Authenticators.BIOMETRIC_STRONG

        return suspendCoroutine { continuation ->
            when (biometricManager.canAuthenticate(authenticators)) {
                SystemBiometricManager.BIOMETRIC_SUCCESS -> {
                    Log.debug { "Biometric features are available" }
                    continuation.resume(
                        BiometricsAvailability(
                            available = true,
                            canBeEnrolled = false,
                        ),
                    )
                }
                SystemBiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                    Log.debug { "No biometric features enrolled" }
                    continuation.resume(
                        BiometricsAvailability(
                            available = false,
                            canBeEnrolled = true,
                        ),
                    )
                }
                SystemBiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                    Log.debug { "No biometric features available on this device" }
                    continuation.resume(
                        BiometricsAvailability(
                            available = false,
                            canBeEnrolled = false,
                        ),
                    )
                }
                SystemBiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                    Log.debug { "Biometric features are currently unavailable" }
                    continuation.resume(
                        BiometricsAvailability(
                            available = false,
                            canBeEnrolled = false,
                        ),
                    )
                }
                SystemBiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                    Log.debug { "Biometric features are currently unavailable, security update required" }
                    continuation.resume(
                        BiometricsAvailability(
                            available = false,
                            canBeEnrolled = false,
                        ),
                    )
                }
                SystemBiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
                    Log.debug { "Biometric features are in unknown status" }
                    continuation.resume(
                        BiometricsAvailability(
                            available = false,
                            canBeEnrolled = false,
                        ),
                    )
                }
                SystemBiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
                    Log.debug { "Biometric features are unsupported" }
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
        result: (AuthenticationResult) -> Unit,
    ): BiometricPrompt.AuthenticationCallback {
        return object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                Log.error {
                    """
                        Biometric authentication error
                        |- Code: $errorCode 
                        |- Cause: $errString
                    """.trimIndent()
                }
                val error = when (errorCode) {
                    BiometricPrompt.ERROR_LOCKOUT -> TangemSdkError.BiometricsAuthenticationLockout()
                    BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> TangemSdkError.BiometricsAuthenticationPermanentLockout()
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    -> TangemSdkError.UserCanceledBiometricsAuthentication()
                    else -> TangemSdkError.BiometricsAuthenticationFailed(errorCode, errString.toString())
                }
                result(AuthenticationResult.Failure(error))
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                Log.debug { "Biometric authentication succeed" }
                result(AuthenticationResult.Success(result))
            }

            override fun onAuthenticationFailed() {
                Log.error { "Biometric authentication failed" }
            }
        }
    }

    data class BiometricsAvailability(
        val available: Boolean,
        val canBeEnrolled: Boolean,
    )

    sealed interface AuthenticationResult {
        data class Failure(
            val error: TangemError,
        ) : AuthenticationResult

        data class Success(
            val result: BiometricPrompt.AuthenticationResult,
        ) : AuthenticationResult
    }
}

package com.tangem.tangem_sdk_new.biometrics

import android.os.Build
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
import com.tangem.common.services.secure.SecureStorage
import com.tangem.tangem_sdk_new.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.InvalidKeyException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import androidx.biometric.BiometricManager as SystemBiometricManager

@RequiresApi(Build.VERSION_CODES.M)
internal class AndroidBiometricManager(
    secureStorage: SecureStorage,
    private val activity: FragmentActivity,
) : BiometricManager,
    DefaultLifecycleObserver,
    LifecycleOwner by activity {

    private val encryptionManager by lazy {
        EncryptionManager(secureStorage)
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

    override suspend fun authenticate(
        mode: BiometricManager.AuthenticationMode,
    ): CompletionResult<ByteArray> {
        return if (canAuthenticate) {
            tryToProceedData(mode)
        } else {
            CompletionResult.Failure(TangemSdkError.BiometricsUnavailable())
        }
    }

    private suspend fun tryToProceedData(
        mode: BiometricManager.AuthenticationMode,
    ): CompletionResult<ByteArray> {
        return proceedData(mode)
            .flatMapOnFailure { error ->
                when ((error as? TangemSdkError.ExceptionError)?.cause) {
                    is UserNotAuthenticatedException -> {
                        Log.debug { "The key's validity timed out" }
                        withContext(Dispatchers.Main) {
                            authenticateInternal(mode)
                        }
                    }
                    is InvalidKeyException -> {
                        Log.error { "Key is invalid" }
                        CompletionResult.Failure(error)
                    }
                    else -> CompletionResult.Failure(error)
                }
            }
    }

    private fun proceedData(
        mode: BiometricManager.AuthenticationMode,
    ): CompletionResult<ByteArray> = catching {
        when (mode) {
            is BiometricManager.AuthenticationMode.Decryption -> encryptionManager.decrypt(mode.keyName, mode.data)
            is BiometricManager.AuthenticationMode.Encryption -> encryptionManager.encrypt(mode.keyName, mode.data)
        }
    }

    private suspend fun authenticateInternal(
        mode: BiometricManager.AuthenticationMode,
    ): CompletionResult<ByteArray> = suspendCoroutine { continuation ->
        val biometricPrompt = BiometricPrompt(
            activity,
            createAuthenticationCallback { result ->
                when (result) {
                    is AuthenticationResult.Failure -> {
                        continuation.resume(CompletionResult.Failure(result.error))
                    }
                    is AuthenticationResult.Success -> {
                        continuation.resume(proceedData(mode))
                    }
                }
            }
        )

        biometricPrompt.authenticate(biometricPromptInfo)
    }

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
                        )
                    )
                }
                SystemBiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                    Log.debug { "No biometric features enrolled" }
                    continuation.resume(
                        BiometricsAvailability(
                            available = false,
                            canBeEnrolled = true,
                        )
                    )
                }
                SystemBiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                    Log.debug { "No biometric features available on this device" }
                    continuation.resume(
                        BiometricsAvailability(
                            available = false,
                            canBeEnrolled = false,
                        )
                    )
                }
                SystemBiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                    Log.debug { "Biometric features are currently unavailable" }
                    continuation.resume(
                        BiometricsAvailability(
                            available = false,
                            canBeEnrolled = false,
                        )
                    )
                }
                SystemBiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                    Log.debug { "Biometric features are currently unavailable, security update required" }
                    continuation.resume(
                        BiometricsAvailability(
                            available = false,
                            canBeEnrolled = false,
                        )
                    )
                }
                SystemBiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
                    Log.debug { "Biometric features are in unknown status" }
                    continuation.resume(
                        BiometricsAvailability(
                            available = false,
                            canBeEnrolled = false,
                        )
                    )
                }
                SystemBiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
                    Log.debug { "Biometric features are unsupported" }
                    continuation.resume(
                        BiometricsAvailability(
                            available = false,
                            canBeEnrolled = false,
                        )
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
                        |- Message: $errString
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

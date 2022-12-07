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

    private val encryptionManager = EncryptionManager(secureStorage)

    private val biometricPromptInfo by lazy(mode = LazyThreadSafetyMode.NONE) {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.biometric_auth_title))
            .setNegativeButtonText(activity.getString(R.string.biometric_auth_negative_text))
            .build()
    }

    override var canAuthenticate: Boolean = false
        private set

    override fun onResume(owner: LifecycleOwner) {
        lifecycleScope.launch {
            canAuthenticate = checkBiometricsAvailability()
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

    override fun unauthenticate(keyName: String?) {
        encryptionManager.unauthenticateSecretKey(keyName)
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
        encryptionManager.authenticateSecretKeysIfNot(mode.keys)
        when (mode) {
            is BiometricManager.AuthenticationMode.Decryption -> encryptionManager.decrypt(mode.keyName, mode.data)
            is BiometricManager.AuthenticationMode.Encryption -> encryptionManager.encrypt(mode.keyName, mode.data)
            is BiometricManager.AuthenticationMode.Keys -> byteArrayOf()
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
                        continuation.resume(
                            CompletionResult.Failure(
                                TangemSdkError.BiometricsAuthenticationFailed(
                                    biometricsErrorCode = result.errorCode,
                                    customMessage = result.errorString.orEmpty(),
                                )
                            )
                        )
                    }
                    is AuthenticationResult.Success -> {
                        continuation.resume(proceedData(mode))
                    }
                }
            }
        )

        biometricPrompt.authenticate(biometricPromptInfo)
    }

    private suspend fun checkBiometricsAvailability(): Boolean {
        val biometricManager = SystemBiometricManager.from(activity)
        val authenticators = SystemBiometricManager.Authenticators.BIOMETRIC_STRONG

        return suspendCoroutine { continuation ->
            when (biometricManager.canAuthenticate(authenticators)) {
                SystemBiometricManager.BIOMETRIC_SUCCESS -> {
                    Log.debug { "Biometric features are available" }
                    continuation.resume(true)
                }
                SystemBiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                    Log.debug { "No biometric features enrolled" }
                    continuation.resume(false)
                }
                SystemBiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                    Log.debug { "No biometric features available on this device" }
                    continuation.resume(false)
                }
                SystemBiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                    Log.debug { "Biometric features are currently unavailable" }
                    continuation.resume(false)
                }
                SystemBiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                    Log.debug { "Biometric features are currently unavailable, security update required" }
                    continuation.resume(false)
                }
                SystemBiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
                    Log.debug { "Biometric features are in unknown status" }
                    continuation.resume(false)
                }
                SystemBiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
                    Log.debug { "Biometric features are unsupported" }
                    continuation.resume(false)
                }
            }
        }
    }

    private fun createAuthenticationCallback(
        result: (AuthenticationResult) -> Unit,
    ): BiometricPrompt.AuthenticationCallback {
        return object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                Log.error { "Biometric authentication error: $errString" }
                result(AuthenticationResult.Failure(errorCode, errString.toString()))
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

    sealed interface AuthenticationResult {
        data class Failure(
            val errorCode: Int? = null,
            val errorString: String? = null,
        ) : AuthenticationResult

        data class Success(
            val result: BiometricPrompt.AuthenticationResult,
        ) : AuthenticationResult
    }
}

package com.tangem.tangem_sdk_new.biometrics

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.tangem.Log
import com.tangem.common.auth.AuthManager
import com.tangem.tangem_sdk_new.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

internal class BiometricAuthManager(
    private val activity: FragmentActivity,
) : AuthManager,
    DefaultLifecycleObserver,
    LifecycleOwner by activity {

    private val biometricPrompt by lazy(mode = LazyThreadSafetyMode.NONE) {
        BiometricPrompt(
            activity,
            createAuthenticationCallback()
        )
    }
    private val biometricPromptInfo by lazy(mode = LazyThreadSafetyMode.NONE) {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.biometric_auth_title))
            .setNegativeButtonText(activity.getString(R.string.biometric_auth_negative_text))
            .build()
    }

    private val isAuthenticated = Channel<Boolean>()
    private val isBiometricsAvailable = MutableStateFlow(value = false)
    override val canAuthenticate: Boolean
        get() = isBiometricsAvailable.value

    override fun onResume(owner: LifecycleOwner) {
        checkBiometricsAvailability()
    }

    override fun authenticate(
        block: (isAuthenticated: Boolean) -> Unit
    ) {
        if (canAuthenticate) {
            lifecycleScope.launch(Dispatchers.Main) {
                biometricPrompt.authenticate(biometricPromptInfo)
                block(isAuthenticated.receive())
            }
        } else block(false)
    }

    private fun checkBiometricsAvailability(): Boolean {
        val biometricManager = BiometricManager.from(activity)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG

        when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                Log.debug { "Biometric features are available" }
                isBiometricsAvailable.value = true
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Log.error { "No biometric features enrolled" }
                isBiometricsAvailable.value = false
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Log.error { "No biometric features available on this device" }
                isBiometricsAvailable.value = false
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Log.error { "Biometric features are currently unavailable" }
                isBiometricsAvailable.value = false
            }
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                Log.error {
                    "Biometric features are currently unavailable, security update required"
                }
                isBiometricsAvailable.value = false
            }
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
                Log.error { "Biometric features are in unknown status" }
                isBiometricsAvailable.value = false
            }
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
                Log.error { "Biometric features are unsupported" }
                isBiometricsAvailable.value = false
            }
        }

        return isBiometricsAvailable.value
    }

    private fun createAuthenticationCallback() = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            Log.error { "Biometric authentication error: $errString" }
            lifecycleScope.launch { isAuthenticated.send(element = false) }
        }

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            Log.debug { "Biometric authentication succeed" }
            lifecycleScope.launch { isAuthenticated.send(element = true) }
        }

        override fun onAuthenticationFailed() {
            Log.error { "Biometric authentication failed" }
        }
    }
}

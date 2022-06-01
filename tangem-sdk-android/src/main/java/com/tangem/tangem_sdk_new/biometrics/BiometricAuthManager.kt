package com.tangem.tangem_sdk_new.biometrics

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.tangem.Log
import com.tangem.SessionViewDelegate
import com.tangem.common.biomteric.AuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

internal class BiometricAuthManager(
    private val activity: FragmentActivity,
    private val viewDelegate: SessionViewDelegate,
) : DefaultLifecycleObserver,
    AuthManager {
    private val scope get() = activity.lifecycleScope

    private val biometricPrompt by lazy(mode = LazyThreadSafetyMode.NONE) {
        BiometricPrompt(
            activity,
            createAuthenticationCallback()
        )
    }
    private val biometricPromptInfo by lazy(mode = LazyThreadSafetyMode.NONE) {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login")
            .setSubtitle("Proceed using your biometric credential")
            .setNegativeButtonText("Use card access code")
            .build()
    }

    private val isAuthenticated = Channel<Boolean>()
    private val _isBiometricsAvailable = MutableStateFlow(value = false)
    override val canAuthenticate: Boolean
        get() = _isBiometricsAvailable.value

    private var enrollBiometricsLauncher: ActivityResultLauncher<Intent>? = null

    init {
        scope.launchWhenResumed {
            checkBiometricsAvailability()
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        enrollBiometricsLauncher = createEnrollBiometricsLauncher()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        enrollBiometricsLauncher = null
    }

    override fun authenticate(
        block: (isAuthenticated: Boolean) -> Unit
    ) {
        if (canAuthenticate) {
            scope.launch(Dispatchers.Main) {
                biometricPrompt.authenticate(biometricPromptInfo)
                block(isAuthenticated.receive())
            }
        } else block(false)
    }

    private fun checkBiometricsAvailability(): Boolean {
        val biometricManager = BiometricManager.from(activity)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL

        when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                Log.debug { "Biometric features are available" }
                _isBiometricsAvailable.value = true
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Log.error { "No biometric features enrolled, attempt to enroll" }
                tryToEnrollBiometricFeatures()
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Log.error { "No biometric features available on this device" }
                _isBiometricsAvailable.value = false
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Log.error { "Biometric features are currently unavailable" }
                _isBiometricsAvailable.value = false
            }
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                Log.error {
                    "Biometric features are currently unavailable, security update required"
                }
                _isBiometricsAvailable.value = false
            }
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
                Log.error { "Biometric features are in unknown status" }
                _isBiometricsAvailable.value = false
            }
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
                Log.error { "Biometric features are unsupported" }
                _isBiometricsAvailable.value = false
            }
        }

        return _isBiometricsAvailable.value
    }

    // Prompts the user to create credentials that your app accepts.
    private fun tryToEnrollBiometricFeatures() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                putExtra(
                    Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                            BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
            }
            if (enrollBiometricsLauncher != null) {
                enrollBiometricsLauncher?.launch(enrollIntent)
            } else {
                Log.error { "Attempt to enroll biometrics features, not enrolled" }
                _isBiometricsAvailable.value = false
            }
        } else {
            Log.error { "Attempt to enroll biometrics features, low SDK version" }
            _isBiometricsAvailable.value = false
        }
    }

    private fun createAuthenticationCallback() = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            Log.error { "Biometric authentication error: $errString" }
            scope.launch { isAuthenticated.send(element = false) }
        }

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            Log.debug { "Biometric authentication succeed" }
            scope.launch { isAuthenticated.send(element = true) }
        }

        override fun onAuthenticationFailed() {
            Log.error { "Biometric authentication failed" }
            scope.launch { isAuthenticated.send(element = false) }
        }
    }

    private fun createEnrollBiometricsLauncher(): ActivityResultLauncher<Intent> {
        return activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val enrolled = result.resultCode == Activity.RESULT_OK
            if (enrolled) {
                Log.debug { "Attempt to enroll biometrics features succeed" }
            } else {
                Log.error { "Attempt to enroll biometrics features, not enrolled" }
            }
            _isBiometricsAvailable.value = enrolled
        }
    }
}

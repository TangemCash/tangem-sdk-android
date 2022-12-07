@file:Suppress("unused")

package com.tangem.tangem_sdk_new.extensions

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.fragment.app.FragmentActivity
import com.tangem.Log
import com.tangem.LogFormat
import com.tangem.SessionViewDelegate
import com.tangem.TangemSdk
import com.tangem.TangemSdkLogger
import com.tangem.common.biometric.BiometricManager
import com.tangem.common.biometric.DummyBiometricManager
import com.tangem.common.core.Config
import com.tangem.common.services.secure.SecureStorage
import com.tangem.tangem_sdk_new.DefaultSessionViewDelegate
import com.tangem.tangem_sdk_new.NfcLifecycleObserver
import com.tangem.tangem_sdk_new.biometrics.AndroidBiometricManager
import com.tangem.tangem_sdk_new.nfc.NfcManager
import com.tangem.tangem_sdk_new.storage.create
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun TangemSdk.init(activity: FragmentActivity, config: Config = Config()): TangemSdk {
    val secureStorage = SecureStorage.create(activity)
    val nfcManager = TangemSdk.initNfcManager(activity)
    val authManager = TangemSdk.initBiometricAuthManager(activity, secureStorage)

    val viewDelegate = DefaultSessionViewDelegate(nfcManager, nfcManager.reader, activity)
    viewDelegate.sdkConfig = config

    return TangemSdk(
        reader = nfcManager.reader,
        viewDelegate = viewDelegate,
        biometricManager = authManager,
        secureStorage = secureStorage,
        config = config,
    )
}

fun TangemSdk.customDelegate(
    activity: FragmentActivity,
    viewDelegate: SessionViewDelegate? = null,
    config: Config = Config(),
): TangemSdk {
    val secureStorage = SecureStorage.create(activity)
    val nfcManager = TangemSdk.initNfcManager(activity)
    val authManager = TangemSdk.initBiometricAuthManager(activity, secureStorage)

    val safeViewDelegate = viewDelegate
        ?: DefaultSessionViewDelegate(nfcManager, nfcManager.reader, activity).apply {
            this.sdkConfig = config
        }


    return TangemSdk(
        nfcManager.reader,
        safeViewDelegate,
        secureStorage = secureStorage,
        config = config,
        biometricManager = authManager,
    )
}

fun TangemSdk.initNfcManager(activity: ComponentActivity): NfcManager {
    val nfcManager = NfcManager()
    nfcManager.setCurrentActivity(activity)
    activity.lifecycle.addObserver(NfcLifecycleObserver(nfcManager))
    return nfcManager
}

fun TangemSdk.createLogger(
    formatter: LogFormat? = null
): TangemSdkLogger = object : TangemSdkLogger {

    val dateFormatter: DateFormat = SimpleDateFormat("HH:mm:ss:SSS", Locale.getDefault())

    private val tag = "TangemSdkLogger"

    override fun log(message: () -> String, level: Log.Level) {
        if (!Log.Config.Verbose.levels.contains(level)) return

        val formatMessage = formatter?.format(message, level) ?: "${level.prefix}${message()}"
        val logMessage = "${dateFormatter.format(Date())}: $formatMessage"

        when (level) {
            Log.Level.Debug -> android.util.Log.d(tag, logMessage)
            Log.Level.Info -> android.util.Log.i(tag, logMessage)
            Log.Level.Warning -> android.util.Log.w(tag, logMessage)
            Log.Level.Error -> android.util.Log.e(tag, logMessage)
            else -> android.util.Log.v(tag, logMessage)
        }
    }
}
fun TangemSdk.initBiometricAuthManager(
    activity: FragmentActivity,
    secureStorage: SecureStorage,
): BiometricManager {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        AndroidBiometricManager(secureStorage, activity)
            .also { activity.lifecycle.addObserver(it) }
    } else {
        DummyBiometricManager()
    }
}

@file:Suppress("unused")

package com.tangem.sdk.extensions

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.fragment.app.FragmentActivity
import com.tangem.Log
import com.tangem.LogFormat
import com.tangem.SessionViewDelegate
import com.tangem.TangemSdk
import com.tangem.TangemSdkLogger
import com.tangem.common.authentication.AuthenticationManager
import com.tangem.common.authentication.DummyAuthenticationManager
import com.tangem.common.authentication.keystore.DummyKeystoreManager
import com.tangem.common.authentication.keystore.KeystoreManager
import com.tangem.common.core.Config
import com.tangem.common.services.secure.SecureStorage
import com.tangem.crypto.bip39.Wordlist
import com.tangem.sdk.DefaultSessionViewDelegate
import com.tangem.sdk.authentication.AndroidAuthenticationManager
import com.tangem.sdk.authentication.AndroidKeystoreManager
import com.tangem.sdk.nfc.NfcManager
import com.tangem.sdk.storage.create
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun TangemSdk.Companion.init(activity: ComponentActivity, config: Config = Config()): TangemSdk {
    val secureStorage = SecureStorage.create(activity)
    val nfcManager = TangemSdk.initNfcManager(activity)

    val viewDelegate = DefaultSessionViewDelegate(nfcManager, activity)
    viewDelegate.sdkConfig = config

    return TangemSdk(
        reader = nfcManager.reader,
        viewDelegate = viewDelegate,
        secureStorage = secureStorage,
        wordlist = Wordlist.getWordlist(activity),
        config = config,
    )
}

fun TangemSdk.Companion.initWithBiometrics(activity: FragmentActivity, config: Config = Config()): TangemSdk {
    val secureStorage = SecureStorage.create(activity)
    val nfcManager = TangemSdk.initNfcManager(activity)
    val authenticationManager = initAuthenticationManager(activity)
    val keystoreManager = initKeystoreManager(authenticationManager, secureStorage)

    val viewDelegate = DefaultSessionViewDelegate(nfcManager, activity)
    viewDelegate.sdkConfig = config

    return TangemSdk(
        reader = nfcManager.reader,
        viewDelegate = viewDelegate,
        secureStorage = secureStorage,
        authenticationManager = authenticationManager,
        keystoreManager = keystoreManager,
        wordlist = Wordlist.getWordlist(activity),
        config = config,
    )
}

fun TangemSdk.Companion.customDelegate(
    activity: ComponentActivity,
    viewDelegate: SessionViewDelegate? = null,
    config: Config = Config(),
): TangemSdk {
    val secureStorage = SecureStorage.create(activity)
    val nfcManager = TangemSdk.initNfcManager(activity)

    val safeViewDelegate = viewDelegate ?: DefaultSessionViewDelegate(nfcManager, activity).apply {
        this.sdkConfig = config
    }

    return TangemSdk(
        reader = nfcManager.reader,
        viewDelegate = safeViewDelegate,
        secureStorage = secureStorage,
        wordlist = Wordlist.getWordlist(activity),
        config = config,
    )
}

fun TangemSdk.Companion.initNfcManager(activity: ComponentActivity): NfcManager {
    val nfcManager = NfcManager()
    Log.info { "initNfcManager" }
    nfcManager.setCurrentActivity(activity)
    activity.lifecycle.addObserver(nfcManager)
    return nfcManager
}

fun TangemSdk.Companion.createLogger(formatter: LogFormat? = null): TangemSdkLogger = object : TangemSdkLogger {

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

fun TangemSdk.Companion.initAuthenticationManager(activity: FragmentActivity): AuthenticationManager {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        AndroidAuthenticationManager(activity)
            .also { activity.lifecycle.addObserver(it) }
    } else {
        DummyAuthenticationManager()
    }
}

fun TangemSdk.Companion.initKeystoreManager(
    authenticationManager: AuthenticationManager,
    secureStorage: SecureStorage,
): KeystoreManager {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        AndroidKeystoreManager(authenticationManager, secureStorage)
    } else {
        DummyKeystoreManager()
    }
}

fun AuthenticationManager.unsubscribe(activity: FragmentActivity) {
    if (this is AndroidAuthenticationManager) {
        activity.lifecycle.removeObserver(this)
    }
}

fun NfcManager.unsubscribe(activity: FragmentActivity) {
    activity.lifecycle.removeObserver(this)
}

package com.tangem.tangem_sdk_new.extensions

import androidx.activity.ComponentActivity
import com.tangem.Log
import com.tangem.SessionViewDelegate
import com.tangem.TangemSdk
import com.tangem.TangemSdkLogger
import com.tangem.common.core.Config
import com.tangem.common.services.secure.SecureStorage
import com.tangem.tangem_sdk_new.DefaultSessionViewDelegate
import com.tangem.tangem_sdk_new.NfcLifecycleObserver
import com.tangem.tangem_sdk_new.nfc.NfcManager
import com.tangem.tangem_sdk_new.storage.create
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

fun TangemSdk.Companion.init(activity: ComponentActivity, config: Config = Config()): TangemSdk {
    val nfcManager = TangemSdk.initNfcManager(activity)

    val viewDelegate = DefaultSessionViewDelegate(nfcManager, nfcManager.reader, activity)
    viewDelegate.sdkConfig = config

    return TangemSdk(
        reader = nfcManager.reader,
        viewDelegate = viewDelegate,
        secureStorage = SecureStorage.create(activity),
        config = config,
    )
}

fun TangemSdk.Companion.customDelegate(
    activity: ComponentActivity,
    viewDelegate: SessionViewDelegate? = null,
    config: Config = Config()
): TangemSdk {
    val nfcManager = TangemSdk.initNfcManager(activity)

    val viewDelegate =
        viewDelegate ?: DefaultSessionViewDelegate(nfcManager, nfcManager.reader, activity).apply {
            this.sdkConfig = config
        }


    return TangemSdk(
        nfcManager.reader,
        viewDelegate,
        secureStorage = SecureStorage.create(activity),
        config = config,
    )
}

fun TangemSdk.Companion.initNfcManager(activity: ComponentActivity): NfcManager {
    val nfcManager = NfcManager()
    nfcManager.setCurrentActivity(activity)
    activity.lifecycle.addObserver(NfcLifecycleObserver(nfcManager))
    return nfcManager
}

fun TangemSdk.Companion.createLogger(): TangemSdkLogger {
    return object : TangemSdkLogger {
        private val tag = "TangemSdkLogger"
        private val dateFormatter: DateFormat =
            SimpleDateFormat("HH:mm:ss:SSS", Locale.getDefault())

        override fun log(message: () -> String, level: Log.Level) {
            if (!Log.Config.Verbose.levels.contains(level)) return

            val prefixDelimiter = if (level.prefix.isEmpty()) "" else ": "
            val logMessage =
                "${dateFormatter.format(Date())}: ${level.prefix}$prefixDelimiter${message()}"
            when (level) {
                Log.Level.Debug -> android.util.Log.d(tag, logMessage)
                Log.Level.Info -> android.util.Log.i(tag, logMessage)
                Log.Level.Warning -> android.util.Log.w(tag, logMessage)
                Log.Level.Error -> android.util.Log.e(tag, logMessage)
                else -> android.util.Log.v(tag, logMessage)
            }
        }
    }
}

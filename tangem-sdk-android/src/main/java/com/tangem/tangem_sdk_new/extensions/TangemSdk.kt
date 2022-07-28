package com.tangem.tangem_sdk_new.extensions

import androidx.activity.ComponentActivity
import com.tangem.*
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

    return DefaultTangemSdk(
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


    return DefaultTangemSdk(
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

fun TangemSdk.Companion.createLogger(
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

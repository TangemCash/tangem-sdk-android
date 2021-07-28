package com.tangem.tangem_sdk_new.extensions

import androidx.activity.ComponentActivity
import com.tangem.SessionViewDelegate
import com.tangem.TangemSdk
import com.tangem.common.core.Config
import com.tangem.common.services.secure.SecureStorage
import com.tangem.tangem_sdk_new.DefaultSessionViewDelegate
import com.tangem.tangem_sdk_new.NfcLifecycleObserver
import com.tangem.tangem_sdk_new.nfc.NfcManager
import com.tangem.tangem_sdk_new.storage.create

fun TangemSdk.Companion.init(activity: ComponentActivity, config: Config = Config()): TangemSdk {
    val nfcManager = TangemSdk.initNfcManager(activity)

    val viewDelegate = DefaultSessionViewDelegate(nfcManager, nfcManager.reader)
    viewDelegate.sdkConfig = config
    viewDelegate.activity = activity

    return TangemSdk(
            nfcManager.reader,
            viewDelegate,
            SecureStorage.create(activity),
            config,
    )
}

fun TangemSdk.Companion.customDelegate(
    activity: ComponentActivity,
    viewDelegate: SessionViewDelegate? = null,
    config: Config = Config()
): TangemSdk {
    val nfcManager = TangemSdk.initNfcManager(activity)

    val viewDelegate = viewDelegate ?: DefaultSessionViewDelegate(nfcManager, nfcManager.reader).apply {
        this.sdkConfig = config
        this.activity = activity
    }

    return TangemSdk(
            nfcManager.reader,
            viewDelegate,
            SecureStorage.create(activity),
            config,
    )
}

fun TangemSdk.Companion.initNfcManager(activity: ComponentActivity): NfcManager {
    val nfcManager = NfcManager()
    nfcManager.setCurrentActivity(activity)
    activity.lifecycle.addObserver(NfcLifecycleObserver(nfcManager))
    return nfcManager
}

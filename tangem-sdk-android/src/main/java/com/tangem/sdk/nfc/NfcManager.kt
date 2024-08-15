package com.tangem.sdk.nfc

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.tangem.Log
import com.tangem.common.extensions.VoidCallback
import com.tangem.common.nfc.ReadingActiveListener

/**
 * Helps use NFC, leveraging Android NFC functionality.
 * Launches [NfcAdapter], manages it with [Activity] lifecycle,
 * enables and disables Nfc Reading Mode, receives NFC [Tag].
 */
class NfcManager : NfcAdapter.ReaderCallback, ReadingActiveListener, DefaultLifecycleObserver {

    override var readingIsActive: Boolean = false
        set(value) {
            Log.nfc { "set readingIsActive $value" }
            if (value) {
                disableReaderMode()
                // delay before enableReaderMode because some devices catch ANR or smth without it
                Thread.sleep(200)
                enableReaderMode()
            }
            field = value
        }

    val reader = NfcReader()

    val isNfcEnabled: Boolean
        get() = nfcAdapter?.isEnabled == true

    private val onTagDiscoveredListeners: MutableList<VoidCallback> = mutableListOf()
    private var activity: Activity? = null
    private var nfcAdapter: NfcAdapter? = null

    private val mBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return
            if (action == NfcAdapter.ACTION_ADAPTER_STATE_CHANGED &&
                intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE, NfcAdapter.STATE_ON) == NfcAdapter.STATE_ON
            ) {
                enableReaderMode()
            }
        }
    }

    override fun onTagDiscovered(tag: Tag?) {
        Log.info { "NFC tag is discovered readingIsActive $readingIsActive" }
        onTagDiscoveredListeners.forEach { it.invoke() }
        if (readingIsActive) reader.onTagDiscovered(tag) else ignoreTag(tag)
    }

    fun addTagDiscoveredListener(listener: VoidCallback) {
        onTagDiscoveredListeners.add(listener)
    }

    fun removeTagDiscoveredListener(listener: VoidCallback) {
        onTagDiscoveredListeners.remove(listener)
    }

    fun setCurrentActivity(activity: Activity) {
        this.activity = activity
        nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        val filter = IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
        activity?.registerReceiver(mBroadcastReceiver, filter)
        enableReaderModeIfEnabled()
    }

    override fun onStart(owner: LifecycleOwner) {
        reader.listener = this
    }

    override fun onStop(owner: LifecycleOwner) {
        reader.stopSession(true)
        disableReaderMode()
        reader.listener = null
    }

    override fun onDestroy(owner: LifecycleOwner) {
        activity?.unregisterReceiver(mBroadcastReceiver)
        activity = null
        nfcAdapter = null
    }

    private fun enableReaderModeIfEnabled() {
        val nfcEnabled = nfcAdapter?.isEnabled == true

        if (nfcEnabled) {
            enableReaderMode()
        }
    }

    private fun enableReaderMode() {
        Log.nfc { "enableReaderMode $nfcAdapter" }
        if (activity?.isDestroyed == false) {
            nfcAdapter?.enableReaderMode(activity, this, READER_FLAGS, Bundle())
        }
    }

    private fun disableReaderMode() {
        Log.nfc { "disableReaderMode $nfcAdapter" }
        if (activity?.isDestroyed == false) {
            nfcAdapter?.disableReaderMode(activity)
        }
    }

    private fun ignoreTag(tag: Tag?) {
        Log.nfc { "NFC tag is ignored" }
        nfcAdapter?.ignore(tag, IGNORE_DEBOUNCE_MS, null, null)
        IsoDep.get(tag)?.closeInternal()
    }

    private companion object {
        // reader mode flags: listen for type A (not B), skipping ndef check
        const val READER_FLAGS = NfcAdapter.FLAG_READER_NFC_A or
            NfcAdapter.FLAG_READER_NFC_V or
            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK or
            NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS

        const val IGNORE_DEBOUNCE_MS = 1_500
    }
}

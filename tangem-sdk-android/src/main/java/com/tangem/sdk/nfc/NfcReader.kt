package com.tangem.sdk.nfc

import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.IsoDep
import android.nfc.tech.NfcV
import com.tangem.Log
import com.tangem.common.CompletionResult
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TagType
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.toHexString
import com.tangem.common.nfc.CardReader
import com.tangem.common.nfc.ReadingActiveListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume

data class NfcTag(val type: TagType, val isoDep: IsoDep?, val nfcV: NfcV? = null)

/**
 * Provides NFC communication between an Android application and Tangem card.
 */
class NfcReader : CardReader {
    override val tag = ConflatedBroadcastChannel<TagType?>()
    override var scope: CoroutineScope? = null

    var listener: ReadingActiveListener? = null

    private val readerMutex = Mutex()
    private var nfcTag: NfcTag? = null
        set(value) {
            field = value
            Log.nfc { "received tag: ${value?.type?.name?.uppercase()}" }
            scope?.launchWithLock(readerMutex) { tag.send(value?.type) }
        }

    override fun startSession() {
        scope?.launchWithLock(readerMutex) {
            Log.nfc { "start NFC session, thread ${Thread.currentThread().id}" }
            nfcTag = null
            listener?.readingIsActive = true
        }
    }

    override fun pauseSession() {
        scope?.launchWithLock(readerMutex) {
            Log.nfc { "pause NFC session, thread ${Thread.currentThread().id}" }
            listener?.readingIsActive = false
        }
    }

    override fun resumeSession() {
        scope?.launchWithLock(readerMutex) {
            Log.nfc { "resume NFC session, thread ${Thread.currentThread().id}" }
            listener?.readingIsActive = true
        }
    }

    fun onTagDiscovered(tag: Tag?) {
        scope?.launchWithLock(readerMutex) {
            NfcV.get(tag)?.let {
                nfcTag = NfcTag(TagType.Slix, null, NfcV.get(tag))
                return@launchWithLock
            }
            IsoDep.get(tag)?.let { isoDep ->
                connect(isoDep)
                nfcTag = NfcTag(TagType.Nfc, isoDep)
            }
        }
    }

    private suspend fun connect(isoDep: IsoDep) {
        Log.nfc { "connect" }
        if (isoDep.isConnected) {
            Log.nfc { "already connected close and reconnect" }
            isoDep.closeInternal()
            delay(CONNECTION_DELAY)
            isoDep.connectInternal()
        } else {
            isoDep.connectInternal()
            Log.nfc { "connected" }
        }
        isoDep.timeout = ISO_DEP_TIMEOUT_MS
    }

    override fun stopSession(cancelled: Boolean) {
        scope?.launchWithLock(readerMutex) {
            Log.nfc { "stop NFC session, thread ${Thread.currentThread().id}" }
            listener?.readingIsActive = false
            if (cancelled) {
                val userCancelledException = TangemSdkError.UserCancelled()
                scope?.cancel(
                    CancellationException(
                        message = userCancelledException.customMessage,
                        cause = userCancelledException,
                    ),
                )
            } else {
                nfcTag = null
            }
        }
    }

    override suspend fun transceiveApdu(apdu: CommandApdu): CompletionResult<ResponseApdu> =
        suspendCancellableCoroutine { continuation ->
            transceiveApdu(apdu) { result ->
                if (continuation.isActive) continuation.resume(result)
            }
        }

    override fun transceiveApdu(apdu: CommandApdu, callback: CompletionCallback<ResponseApdu>) {
        transceiveRaw(apdu.apduData) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    result.data?.let {
                        val rApdu = ResponseApdu(it)
                        Log.nfc { rApdu.toString() }
                        callback.invoke(CompletionResult.Success(rApdu))
                    }
                }

                is CompletionResult.Failure -> callback.invoke(CompletionResult.Failure(result.error))
            }
        }
    }

    override suspend fun transceiveRaw(apduData: ByteArray): CompletionResult<ByteArray?> =
        suspendCancellableCoroutine { continuation ->
            transceiveRaw(apduData) { result ->
                if (continuation.isActive) continuation.resume(result)
            }
        }

    override fun transceiveRaw(apduData: ByteArray, callback: CompletionCallback<ByteArray?>) {
        val rawResponse: ByteArray? = try {
            Log.nfc { "transceiveRaw: " + apduData.toHexString() }
            transcieveAndLog(apduData)
        } catch (exception: TagLostException) {
            Log.nfc { "ERROR transceiving data TagLostException: $exception" }
            callback.invoke(CompletionResult.Failure(TangemSdkError.TagLost()))
            nfcTag = null
            return
        } catch (exception: Exception) {
            Log.nfc { "ERROR transceiving data Exception: $exception" }
            tryHandleNfcError(exception, callback)
            nfcTag = null
            return
        }
        callback.invoke(CompletionResult.Success(rawResponse))
    }

    private fun transcieveAndLog(apduData: ByteArray): ByteArray? {
        Log.nfc { "transcieve..." }
        val startTime = System.currentTimeMillis()
        val isExtendedLengthApduSupported = nfcTag?.isoDep?.isExtendedLengthApduSupported
        Log.nfc { "isExtendedLengthApduSupported $isExtendedLengthApduSupported" }
        Log.nfc { "transcieveAndLog: isoDep isConnected = " + nfcTag?.isoDep?.isConnected }
        val rawResponse = nfcTag?.isoDep?.transceive(apduData)
        val finishTime = System.currentTimeMillis()
        Log.nfc { "transcieve success: [${finishTime - startTime}] ms" }
        return rawResponse
    }

    private fun tryHandleNfcError(exception: Exception, callback: (response: CompletionResult<ByteArray?>) -> Unit) {
        // The messages of errors can vary on different Android devices,
        // but we try to identify it by parsing the message.
        if (exception.message?.contains("length") == true) {
            callback.invoke(CompletionResult.Failure(TangemSdkError.ExtendedLengthNotSupported()))
        }
    }

    override fun readSlixTag(callback: CompletionCallback<ResponseApdu>) {
        val nfcV = nfcTag?.nfcV
        if (nfcV == null) {
            callback.invoke(CompletionResult.Failure(TangemSdkError.ErrorProcessingCommand()))
            return
        }
        val response = SlixTagReader().transceive(nfcV)
        when (response) {
            is SlixReadResult.Failure -> {
                Log.nfc { "read Slix tag error: ${response.exception.message}" }
                callback.invoke(CompletionResult.Failure(TangemSdkError.ErrorProcessingCommand()))
            }

            is SlixReadResult.Success -> {
                Log.nfc { "read Slix tag succeed" }
                callback.invoke(CompletionResult.Success(ResponseApdu(response.data)))
            }
        }
    }

    private fun CoroutineScope.launchWithLock(mutex: Mutex, action: suspend () -> Unit) {
        this.launch {
            mutex.withLock(null) {
                action()
            }
        }
    }

    private companion object {
        const val ISO_DEP_TIMEOUT_MS = 240_000
        const val CONNECTION_DELAY = 100L
    }
}

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
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.CancellationException
import kotlin.coroutines.resume

data class NfcTag(val type: TagType, val isoDep: IsoDep?, val nfcV: NfcV? = null)

/**
 * Provides NFC communication between an Android application and Tangem card.
 */
class NfcReader : CardReader {
    override val tag = ConflatedBroadcastChannel<TagType?>()
    override var scope: CoroutineScope? = null

    var listener: ReadingActiveListener? = null

    private var nfcTag: NfcTag? = null
        set(value) {
            field = value
            Log.nfc { "received tag: ${value?.type?.name?.uppercase()}" }
            scope?.launch { tag.send(value?.type) }
        }

    override fun startSession() {
        Log.nfc { "start NFC session" }
        nfcTag = null
        listener?.readingIsActive = true
    }

    override fun pauseSession() {
        Log.nfc { "pause NFC session" }
        listener?.readingIsActive = false
    }

    override fun resumeSession() {
        Log.nfc { "resume NFC session" }
        listener?.readingIsActive = true
    }

    fun onTagDiscovered(tag: Tag?) {
        NfcV.get(tag)?.let {
            nfcTag = NfcTag(TagType.Slix, null, NfcV.get(tag))
            return
        }
        IsoDep.get(tag)?.let { isoDep ->
            connect(isoDep)
            nfcTag = NfcTag(TagType.Nfc, isoDep)
        }
    }

    private fun connect(isoDep: IsoDep) {
        Log.nfc { "connect" }
        isoDep.connect()
        isoDep.close()
        isoDep.connect()
        isoDep.timeout = ISO_DEP_TIMEOUT_MS
    }

    override fun stopSession(cancelled: Boolean) {
        Log.nfc { "stop NFC session" }
        listener?.readingIsActive = false
        if (cancelled) {
            scope?.cancel(CancellationException(TangemSdkError.UserCancelled().customMessage))
        } else {
            nfcTag = null
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
            Log.nfc { apduData.toHexString() }
            transcieveAndLog(apduData)
        } catch (exception: TagLostException) {
            Log.nfc { "ERROR transceiving data: ${exception.localizedMessage}" }
            callback.invoke(CompletionResult.Failure(TangemSdkError.TagLost()))
            nfcTag = null
            return
        } catch (exception: Exception) {
            Log.nfc { "ERROR transceiving data: ${exception.localizedMessage}" }
            tryHandleNfcError(exception, callback)
            nfcTag = null
            return
        }
        callback.invoke(CompletionResult.Success(rawResponse))
    }

    private fun transcieveAndLog(apduData: ByteArray): ByteArray? {
        Log.nfc { "transcieve..." }
        val startTime = System.currentTimeMillis()
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

    private companion object {
        const val ISO_DEP_TIMEOUT_MS = 240_000
    }
}

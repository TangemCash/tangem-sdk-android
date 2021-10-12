package com.tangem.tangem_sdk_new.nfc

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
            Log.nfc { "Received tag: ${value?.type}" }
            scope?.launch { tag.send(value?.type) }
        }

    override fun startSession() {
        Log.nfc { "Start NFC session" }
        nfcTag = null
        listener?.readingIsActive = true
    }

    override fun pauseSession() {
        Log.nfc { "Pause NFC session" }
        listener?.readingIsActive = false
    }

    override fun resumeSession() {
        Log.nfc { "Resume NFC session" }
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
        Log.nfc { "Connect" }
        isoDep.connect()
        isoDep.close()
        isoDep.connect()
        isoDep.timeout = 240000
    }

    override fun stopSession(cancelled: Boolean) {
        Log.nfc { "Stop NFC session" }
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
        val rawResponse: ByteArray? = try {
            Log.apdu { apdu.toString() }
            transcieveAndLog(apdu.apduData)
        } catch (exception: TagLostException) {
            Log.nfc { "Error transceiving data: ${exception.localizedMessage}" }
            callback.invoke(CompletionResult.Failure(TangemSdkError.TagLost()))
            nfcTag = null
            return
        } catch (exception: Exception) {
            Log.nfc { "Error transceiving data: ${exception.localizedMessage}" }
            tryHandleNfcError(exception, callback)
            nfcTag = null
            return
        }
        rawResponse?.let {
            val rApdu = ResponseApdu(it)
            Log.apdu { rApdu.toString() }
            callback.invoke(CompletionResult.Success(rApdu))
        }
    }

    private fun transcieveAndLog(data: ByteArray): ByteArray? {
        Log.nfc { "Transcieve invoked" }
        val startTime = System.currentTimeMillis()
        val rawResponse = nfcTag?.isoDep?.transceive(data)
        val finishTime = System.currentTimeMillis()
        Log.nfc { "Success response from card received. Execution time is: ${finishTime - startTime} ms" }
        return rawResponse
    }

    private fun tryHandleNfcError(exception: Exception, callback: (response: CompletionResult<ResponseApdu>) -> Unit) {
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
                Log.nfc { "Read Slix tag error: ${response.exception.message}" }
                callback.invoke(CompletionResult.Failure(TangemSdkError.ErrorProcessingCommand()))
            }
            is SlixReadResult.Success -> {
                Log.nfc { "Read Slix tag succeed" }
                callback.invoke(CompletionResult.Success(ResponseApdu(response.data)))
            }
        }
    }
}
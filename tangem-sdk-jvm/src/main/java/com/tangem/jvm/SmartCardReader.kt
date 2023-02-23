package com.tangem.jvm

import com.tangem.Log
import com.tangem.common.CompletionResult
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TagType
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import com.tangem.common.nfc.CardReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import javax.smartcardio.Card
import javax.smartcardio.CardChannel
import javax.smartcardio.CardException
import javax.smartcardio.CardTerminal
import javax.smartcardio.CommandAPDU

class SmartCardReader(private var terminal: CardTerminal?) : CardReader {
    private var card: Card? = null
    private var channel: CardChannel? = null
    var logException = false

    override var scope: CoroutineScope? = null
    override val tag = ConflatedBroadcastChannel<TagType?>()

    private var connectedTag: TagType? = null
        set(value) {
            field = value
            scope?.launch { tag.send(value) }
        }

    override fun stopSession(cancelled: Boolean) {
        if (card != null) {
            try {
                channel = null
                try {
                    card!!.disconnect(false)
                } catch (e: CardException) {
                    e.message?.let { Log.nfc { it } }
                    if (logException) e.printStackTrace()
                }
                card = null
            } catch (e: CardException) {
                throw IOException(e)
            }
        } else if (terminal?.isCardPresent != false) {
            card = terminal!!.connect("*")
            card?.disconnect(false)
        }
        terminal = null
        card = null
        channel = null
    }

    fun isCardPresent(): Boolean {
        return terminal?.isCardPresent ?: false
    }

    @Throws(CardException::class)
    fun getUID(): ByteArray? {
        val rsp = channel!!.transmit(CommandAPDU("FFCA000000".hexToBytes()))
        return rsp.data
    }

    @Throws(CardException::class)
    override fun startSession() {
        val terminal = terminal ?: throw CardException("No terminal specified!")

        if (terminal.waitForCardPresent(30000)) {
            card = terminal.connect("*")
            channel = card?.basicChannel
            getUID()?.let { Log.nfc { "UID: " + it.toHexString() } }
            connectedTag = TagType.Nfc
        } else {
            throw CardException("Timeout waiting card present!")
        }
    }

    override fun pauseSession() {
    }

    override fun readSlixTag(callback: CompletionCallback<ResponseApdu>) {
    }

    override fun resumeSession() {
    }

    override suspend fun transceiveApdu(apdu: CommandApdu): CompletionResult<ResponseApdu> =
        suspendCancellableCoroutine { continuation ->
            transceiveApdu(apdu) { result ->
                if (continuation.isActive) continuation.resume(result) {}
            }
        }

    override fun transceiveApdu(apdu: CommandApdu, callback: (response: CompletionResult<ResponseApdu>) -> Unit) {
        transceiveRaw(apdu.apduData) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    result.data?.let {
                        Log.nfc { "data from the card was received" }
                        Log.nfc { "raw data that was received from the card: ${it.toHexString()}" }
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
                if (continuation.isActive) continuation.resume(result) {}
            }
        }

    override fun transceiveRaw(apduData: ByteArray, callback: CompletionCallback<ByteArray?>) {
        val channel = channel ?: throw IOException()

        Log.nfc { "sending data to the card, size is ${apduData.size}" }
        Log.nfc { "raw data that is to be sent to the card: ${apduData.toHexString()}" }
        try {
            val rspAPDU = channel.transmit(CommandAPDU(apduData))
            callback.invoke(CompletionResult.Success(rspAPDU.bytes))
        } catch (e: Exception) {
            callback.invoke(CompletionResult.Failure(TangemSdkError.TagLost()))
            if (terminal?.waitForCardAbsent(30000) == true) {
                connectedTag = null
                startSession()
            } else {
                Log.error { e.localizedMessage }
                stopSession()
            }
        }
    }
}
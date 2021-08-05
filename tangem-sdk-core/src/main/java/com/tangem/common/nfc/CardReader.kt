package com.tangem.common.nfc

import com.tangem.common.CompletionResult
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TagType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BroadcastChannel

/**
 * Allows interaction between the phone or any other terminal and Tangem card.
 *
 * Its default implementation, NfcCardReader, is in our tangem-sdk module.
 */
interface CardReader {

    val tag: BroadcastChannel<TagType?>
    var scope: CoroutineScope?

    /**
     * Sends data to the card and receives the reply in an asynchronous way using coroutines.
     *
     * @param apdu Data to be sent. [CommandApdu] serializes it to a [ByteArray]
     * @param callback Returns response from the card,
     * [ResponseApdu] Allows to convert raw data to [Tlv]
     */
    suspend fun transceiveApdu(apdu: CommandApdu): CompletionResult<ResponseApdu>

    /**
     * Sends data to the card and receives the reply.
     *
     * @param apdu Data to be sent. [CommandApdu] serializes it to a [ByteArray]
     * @param callback Returns response from the card,
     * [ResponseApdu] Allows to convert raw data to [Tlv]
     */
    fun transceiveApdu(apdu: CommandApdu, callback: CompletionCallback<ResponseApdu>)

    /**
     * Signals to [CardReader] to become ready to transceive data.
     */
    fun startSession()

    /**
     * Signals to [CardReader] that no further NFC transition is expected.
     */
    fun stopSession(cancelled: Boolean = false)

    fun pauseSession()
    fun resumeSession()

    fun readSlixTag(callback: CompletionCallback<ResponseApdu>)

}

interface ReadingActiveListener {
    var readingIsActive: Boolean
}
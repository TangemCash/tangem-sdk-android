package com.tangem.commands

import com.squareup.moshi.JsonClass
import com.tangem.CardSession
import com.tangem.SessionEnvironment
import com.tangem.TangemSdkError
import com.tangem.common.CompletionResult
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.apdu.StatusWord
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag

@JsonClass(generateAdapter = true)
class SetPinResponse(
        /**
         * CID, Unique Tangem card ID number.
         */
        val cardId: String,
        /**
         *
         */
        val status: SetPinStatus
) : CommandResponse

class SetPinCommand(
        private val pinType: PinType,
        private var newPin1: ByteArray? = null,
        private var newPin2: ByteArray? = null
) : Command<SetPinResponse>() {

    override fun requiresPin2(): Boolean = true

    override fun prepare(session: CardSession, callback: (result: CompletionResult<Unit>) -> Unit) {
        if ((pinType == PinType.Pin1 && newPin1 == null) ||
                (pinType == PinType.Pin2 && newPin2 == null)) {
            requestNewPin(session, callback)
        } else {
            callback(CompletionResult.Success(Unit))
        }
    }

    override fun run(session: CardSession, callback: (result: CompletionResult<SetPinResponse>) -> Unit) {
        if (newPin1 != null || newPin2 != null) {
            transceive(session, callback)
            return
        }

        session.pause()
        requestNewPin(session) {
            session.resume()
            transceive(session, callback)
        }
    }

    private fun requestNewPin(
            session: CardSession, callback: (result: CompletionResult<Unit>) -> Unit
    ) {
        session.viewDelegate.onPinChangeRequested(pinType) { pinString ->
            val newPin = pinString.calculateSha256()
            when (pinType) {
                PinType.Pin1 -> newPin1 = newPin
                PinType.Pin2 -> newPin2 = newPin
            }
            callback(CompletionResult.Success(Unit))
        }
    }

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.Pin, environment.pin1?.value)
        tlvBuilder.append(TlvTag.CardId, environment.card?.cardId)
        tlvBuilder.append(TlvTag.Pin2, environment.pin2?.value)
        tlvBuilder.append(TlvTag.Cvc, environment.cvc)
        tlvBuilder.append(TlvTag.NewPin, newPin1 ?: environment.pin1?.value)
        tlvBuilder.append(TlvTag.NewPin2, newPin2 ?: environment.pin2?.value)
        return CommandApdu(Instruction.SetPin, tlvBuilder.serialize())
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): SetPinResponse {
        val tlvData = apdu.getTlvData() ?: throw TangemSdkError.DeserializeApduFailed()

        val status = SetPinStatus.fromStatusWord(apdu.statusWord)
                ?: throw TangemSdkError.DecodingFailed("Failed to parse set pin status")

        val decoder = TlvDecoder(tlvData)
        return SetPinResponse(
                cardId = decoder.decode(TlvTag.CardId),
                status = status
        )
    }

    companion object {
        fun setPin1(newPin1: ByteArray?): SetPinCommand = SetPinCommand(newPin1 = newPin1, pinType = PinType.Pin1)
        fun setPin2(newPin2: ByteArray?): SetPinCommand = SetPinCommand(newPin2 = newPin2, pinType = PinType.Pin2)
    }
}

enum class PinType {
    Pin1,
    Pin2,
    ;
}

enum class SetPinStatus {
    PinsNotChanged,
    Pin1Changed,
    Pin2Changed,
    Pin3Changed,
    Pins12Changed,
    Pins13Changed,
    Pins23Changed,
    Pins123Changed,
    ;

    companion object {
        fun fromStatusWord(statusWord: StatusWord): SetPinStatus? {
            return when (statusWord) {
                StatusWord.ProcessCompleted -> PinsNotChanged
                StatusWord.Pin1Changed -> Pin1Changed
                StatusWord.Pin2Changed -> Pin2Changed
                StatusWord.Pins12Changed -> Pins12Changed
                StatusWord.Pin3Changed -> Pin3Changed
                StatusWord.Pins13Changed -> Pins13Changed
                StatusWord.Pins23Changed -> Pins23Changed
                StatusWord.Pins123Changed -> Pins123Changed
                else -> null
            }
        }
    }
}

fun PinType.isWrongPinEntered(environment: SessionEnvironment?): Boolean {
    return when (this) {
        PinType.Pin1 -> environment?.pin1?.isDefault == true
        PinType.Pin2 -> environment?.pin2?.isDefault == true
    }
}
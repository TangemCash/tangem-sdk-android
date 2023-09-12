package com.tangem.operations.read

import com.tangem.common.UserCodeType
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.card.Card
import com.tangem.common.core.CardSession
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.SessionEnvironment
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.common.deserialization.CardDeserializer
import com.tangem.common.deserialization.WalletDataDeserializer
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvTag
import com.tangem.operations.Command
import com.tangem.operations.PreflightReadMode

/**
 * This command receives from the Tangem Card all the data about the card and the wallet,
 * including unique card number (CID or cardId) that has to be submitted while calling all other commands.
 */
class DiscoverCommand(private val onReadPerform: () -> Unit) : Command<ReadResponse>() {

    override fun preflightReadMode(): PreflightReadMode = PreflightReadMode.None

    override fun run(
        session: CardSession,
        callback: CompletionCallback<ReadResponse>,
    ) {
        super.run(session) {
            onReadPerform.invoke()
            run(session, callback)
        }
    }

    override fun mapError(card: Card?, error: TangemError): TangemError {
        if (error is TangemSdkError.InvalidParams) {
            return TangemSdkError.AccessCodeRequired()
        }

        return error
    }

    /**
     *  [SessionEnvironment] stores the pin1 value. If no pin1 value was set, it will contain
     *  default value of ‘000000’.
     *  In order to obtain card’s data, [ReadCommand] should use the correct pin 1 value.
     *  The card will not respond if wrong pin1 has been submitted.
     */
    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.Pin, environment.accessCode.value)
        tlvBuilder.append(TlvTag.InteractionMode, ReadMode.Card)
        tlvBuilder.append(TlvTag.TerminalPublicKey, environment.terminalKeys?.publicKey)

        return CommandApdu(Instruction.Read, tlvBuilder.serialize())
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): ReadResponse {
        val decoder = CardDeserializer.getDecoder(apdu)
        val cardDataDecoder = CardDeserializer.getCardDataDecoder(decoder.tlvList)

        val isAccessCodeSetLegacy = environment.isUserCodeSet(UserCodeType.AccessCode)
        val card = CardDeserializer.deserialize(isAccessCodeSetLegacy, decoder, cardDataDecoder)
        val walletData = cardDataDecoder?.let { WalletDataDeserializer.deserialize(it) }

        return ReadResponse(card, walletData)
    }
}

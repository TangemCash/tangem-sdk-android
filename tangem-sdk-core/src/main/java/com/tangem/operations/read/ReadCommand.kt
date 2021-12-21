package com.tangem.operations.read

import com.squareup.moshi.JsonClass
import com.tangem.common.CompletionResult
import com.tangem.common.UserCodeType
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.card.Card
import com.tangem.common.card.WalletData
import com.tangem.common.core.*
import com.tangem.common.deserialization.CardDeserializer
import com.tangem.common.deserialization.WalletDataDeserializer
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvTag
import com.tangem.operations.Command
import com.tangem.operations.CommandResponse
import com.tangem.operations.PreflightReadMode

@JsonClass(generateAdapter = true)
data class ReadResponse(
    val card: Card,
    val walletData: WalletData?
) : CommandResponse

/**
 * This command receives from the Tangem Card all the data about the card and the wallet,
 * including unique card number (CID or cardId) that has to be submitted while calling all other commands.
 */
class ReadCommand : Command<ReadResponse>() {

    override fun preflightReadMode(): PreflightReadMode = PreflightReadMode.None

    override fun run(session: CardSession, callback: CompletionCallback<ReadResponse>) {
        super.run(session) {
            when (it) {
                is CompletionResult.Success -> {
                    session.environment.card = it.data.card
                    session.environment.walletData = it.data.walletData
                    callback(it)
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(it.error))
            }
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
        val decoder = CardDeserializer.getDecoder(environment, apdu)
        val cardDataDecoder = CardDeserializer.getCardDataDecoder(environment, decoder.tlvList)

        val isAccessCodeSetLegacy = environment.isUserCodeSet(UserCodeType.AccessCode)
        val card = CardDeserializer.deserialize(isAccessCodeSetLegacy, decoder, cardDataDecoder)
        val walletData = cardDataDecoder?.let { WalletDataDeserializer.deserialize(it) }

        return ReadResponse(card, walletData)
    }
}
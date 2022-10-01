package com.tangem.operations

import com.squareup.moshi.JsonClass
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.card.Card
import com.tangem.common.core.SessionEnvironment
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag

/**
 * Deserialized response from the Tangem card after `GenerateOTPCommand`.
 * @param cardId: Unique Tangem card ID number.
 * @param rootOTP: Generated  root OTP.
 * @param rootOTPCounter: Generated root OTP's counter.
 * @param walletPublicKey: Wallet's public key.
 */
@JsonClass(generateAdapter = true)
class GenerateOTPResponse(
    val cardId: String,
    val rootOTP: ByteArray,
    val rootOTPCounter: Int,
    val walletPublicKey: ByteArray,
) : CommandResponse

class GenerateOTPCommand : Command<GenerateOTPResponse>() {

    override fun performPreCheck(card: Card): TangemError? {
        if (card.wallets.isEmpty()) return TangemSdkError.WalletNotFound()

        return null
    }

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.Pin, environment.accessCode.value)
        tlvBuilder.append(TlvTag.Pin2, environment.passcode.value)
        tlvBuilder.append(TlvTag.CardId, environment.card?.cardId)

        return CommandApdu(Instruction.GenerateOTP, tlvBuilder.serialize())
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): GenerateOTPResponse {
        val tlvData = apdu.getTlvData(environment.encryptionKey) ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)
        return GenerateOTPResponse(
            decoder.decode(TlvTag.CardId),
            decoder.decode(TlvTag.CodeHash),
            decoder.decode(TlvTag.FileIndex),
            decoder.decode(TlvTag.WalletPublicKey),
        )
    }
}

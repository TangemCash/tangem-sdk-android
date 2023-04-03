package com.tangem.operations

import com.squareup.moshi.JsonClass
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.card.Card
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.SessionEnvironment
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag

@JsonClass(generateAdapter = true)
class GetEntropyResponse(
    val cardId: String,
    val data: ByteArray,
) : CommandResponse

class GetEntropyCommand : Command<GetEntropyResponse>() {

    override fun preflightReadMode(): PreflightReadMode = PreflightReadMode.ReadCardOnly

    override fun performPreCheck(card: Card): TangemError? {
        if (card.firmwareVersion < FirmwareVersion.KeysImportAvailable) {
            return TangemSdkError.WalletNotFound()
        }
        return null
    }

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(tag = TlvTag.Pin, value = environment.accessCode.value)
        tlvBuilder.append(tag = TlvTag.CardId, value = environment.card?.cardId)

        return CommandApdu(Instruction.GetEntropy, tlvBuilder.serialize())
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): GetEntropyResponse {
        val tlvData = apdu.getTlvData() ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)
        return GetEntropyResponse(cardId = decoder.decode(TlvTag.CardId), data = decoder.decode(TlvTag.IssuerData))
    }
}

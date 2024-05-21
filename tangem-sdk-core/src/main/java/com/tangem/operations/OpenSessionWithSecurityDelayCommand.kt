package com.tangem.operations

import com.squareup.moshi.JsonClass
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.card.Card
import com.tangem.common.card.EncryptionMode
import com.tangem.common.core.SessionEnvironment
import com.tangem.common.core.TangemSdkError
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag

@JsonClass(generateAdapter = true)
class OpenSessionWithSecurityDelayResponse(
    val accessLevel: Card.AccessLevel,
) : CommandResponse

/**
 * In case of encrypted communication, App should setup a session before calling any further command.
 * [OpenSessionCommand] generates secret session_key that is used by both host and card
 * to encrypt and decrypt commands’ payload.
 */
class OpenSessionWithSecurityDelayCommand(
    val sessionKeyB: ByteArray,
) : Command<OpenSessionWithSecurityDelayResponse>() {

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        val p2 = EncryptionMode.CcmWithSecurityDelay.byteValue
        tlvBuilder.append(TlvTag.CardId, environment.card?.cardId)
        tlvBuilder.append(TlvTag.SessionKeyB, sessionKeyB)

        return CommandApdu(Instruction.OpenSession.code, tlvBuilder.serialize(), 0, p2)
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): OpenSessionWithSecurityDelayResponse {
        val tlvData = apdu.getTlvData() ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)
        return OpenSessionWithSecurityDelayResponse(decoder.decode(TlvTag.AccessLevel))
    }
}

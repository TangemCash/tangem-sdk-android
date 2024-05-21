package com.tangem.operations

import com.squareup.moshi.JsonClass
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.card.Card
import com.tangem.common.core.SessionEnvironment
import com.tangem.common.core.TangemSdkError
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag
import com.tangem.operations.resetcode.AuthorizeMode

@JsonClass(generateAdapter = true)
class AuthorizeWithPinResponse(
    val accessLevel: Card.AccessLevel,
) : CommandResponse

/**
 * In case of encrypted communication, App should setup a session before calling any further command.
 * [OpenSessionCommand] generates secret session_key that is used by both host and card
 * to encrypt and decrypt commands’ payload.
 */
class AuthorizeWithPinCommand(
) : Command<AuthorizeWithPinResponse> (){

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.InteractionMode, AuthorizeMode.AuthorizeWithPIN)
        tlvBuilder.append(TlvTag.Pin, environment.accessCode.value)

        return CommandApdu(Instruction.Authorize.code, tlvBuilder.serialize(), 0, 0)
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): AuthorizeWithPinResponse {
        val tlvData = apdu.getTlvData() ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)
        return AuthorizeWithPinResponse(decoder.decode(TlvTag.AccessLevel))
    }
}

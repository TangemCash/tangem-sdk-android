package com.tangem.operations

import com.squareup.moshi.JsonClass
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.core.SessionEnvironment
import com.tangem.common.core.TangemSdkError
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag
import com.tangem.operations.resetcode.AuthorizeMode

@JsonClass(generateAdapter = true)
class AuthorizeWithSecurityDelayResponse(
    val pubSessionKeyA: ByteArray,
    val signAttestA: ByteArray,
) : CommandResponse

/**
 * In case of encrypted communication, App should setup a session before calling any further command.
 * [OpenSessionCommand] generates secret session_key that is used by both host and card
 * to encrypt and decrypt commands’ payload.
 */
class AuthorizeWithSecurityDelayCommand(

) : Command<AuthorizeWithSecurityDelayResponse>() {

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.CardId, environment.card?.cardId)
        tlvBuilder.append(TlvTag.InteractionMode, AuthorizeMode.AuthorizeWithSecureDelay)

        return CommandApdu(Instruction.Authorize.code, tlvBuilder.serialize(), 0, 0)
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): AuthorizeWithSecurityDelayResponse {
        val tlvData = apdu.getTlvData() ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)
        return AuthorizeWithSecurityDelayResponse(decoder.decode(TlvTag.SessionKeyA), decoder.decode(TlvTag.CardSignature))
    }
}

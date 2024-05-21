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

@JsonClass(generateAdapter = true)
class ManageAccessTokensResponse(
    val accessToken: ByteArray,
    val identifyToken: ByteArray
) : CommandResponse

enum class ManageAccessTokensMode(val rawValue: Int) {
    Get(0x00),
    Renew(0x01),
    Reset(0x02);

    companion object {
        private val values = values()
        fun byRawValue(rawValue: Int): ManageAccessTokensMode? = values.find { it.rawValue == rawValue }
    }
}

/**
 * In case of encrypted communication, App should setup a session before calling any further command.
 * [OpenSessionCommand] generates secret session_key that is used by both host and card
 * to encrypt and decrypt commands’ payload.
 */
class ManageAccessTokensCommand(
    val mode: ManageAccessTokensMode
) : Command<ManageAccessTokensResponse>() {

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.InteractionMode, mode)

        return CommandApdu(Instruction.ManageAccessTokens.code, tlvBuilder.serialize(), 0, 0)
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): ManageAccessTokensResponse {
        val tlvData = apdu.getTlvData() ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)
        return ManageAccessTokensResponse(decoder.decode(TlvTag.AccessToken), decoder.decode(TlvTag.IdentifyToken))
    }
}

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
class OpenSessionResponse(
    val sessionKeyB: ByteArray,
    val uid: ByteArray
) : CommandResponse

/**
 * In case of encrypted communication, App should setup a session before calling any further command.
 * [OpenSessionCommand] generates secret session_key that is used by both host and card
 * to encrypt and decrypt commands’ payload.
 */
class OpenSessionCommand(
    private val sessionKeyA: ByteArray
) : ApduSerializable<OpenSessionResponse> {

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.SessionKeyA, sessionKeyA)
        val p2 = environment.encryptionMode.byteValue

        return CommandApdu(Instruction.OpenSession.code, tlvBuilder.serialize(), 0, p2)
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): OpenSessionResponse {
        val tlvData = apdu.getTlvData() ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)
        return OpenSessionResponse(decoder.decode(TlvTag.SessionKeyB), decoder.decode(TlvTag.Uid))
    }
}
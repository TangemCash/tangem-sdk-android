package com.tangem.operations.personalization

import com.squareup.moshi.JsonClass
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.core.SessionEnvironment
import com.tangem.operations.Command
import com.tangem.operations.CommandResponse
import com.tangem.operations.PreflightReadMode

@JsonClass(generateAdapter = true)
data class DepersonalizeResponse(
    val success: Boolean,
) : CommandResponse

/**
 * Command available on SDK cards only
 *
 * This command resets card to initial state,
 * erasing all data written during personalization and usage.
 */
class DepersonalizeCommand : Command<DepersonalizeResponse>() {

    override fun preflightReadMode(): PreflightReadMode = PreflightReadMode.None

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        return CommandApdu(Instruction.Depersonalize, byteArrayOf())
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): DepersonalizeResponse {
        return DepersonalizeResponse(true)
    }
}

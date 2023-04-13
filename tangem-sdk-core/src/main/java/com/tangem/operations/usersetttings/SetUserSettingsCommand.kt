package com.tangem.operations.usersetttings

import com.squareup.moshi.JsonClass
import com.tangem.common.CompletionResult
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.card.Card
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.card.UserSettings
import com.tangem.common.card.UserSettingsMask
import com.tangem.common.core.CardSession
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.SessionEnvironment
import com.tangem.common.core.TangemSdkError
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag
import com.tangem.operations.Command
import com.tangem.operations.CommandResponse
import com.tangem.operations.PreflightReadMode

/**
 * Deserialized response from the Tangem card after [SetUserSettingsCommand]. COS v.6.11+
 */
@JsonClass(generateAdapter = true)
class SetUserSettingsCommandResponse(
    /**
     * Unique Tangem card ID number
     */
    val cardId: String,
    /**
     * The settings mask as it was set
     */
    val settings: UserSettings,
) : CommandResponse

class SetUserSettingsCommand(private val settings: UserSettings) : Command<SetUserSettingsCommandResponse>() {

    override fun preflightReadMode(): PreflightReadMode = PreflightReadMode.ReadCardOnly

    override fun performPreCheck(card: Card): TangemSdkError? {
        if (card.firmwareVersion < FirmwareVersion.KeysImportAvailable) {
            return TangemSdkError.UnsupportedCurve()
        }
        return null
    }

    override fun run(session: CardSession, callback: CompletionCallback<SetUserSettingsCommandResponse>) {
        super.run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    session.environment.card = session.environment.card?.copy(userSettings = result.data.settings)
                    callback(CompletionResult.Success(result.data))
                }
                is CompletionResult.Failure -> callback(result)
            }
        }
    }

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.Pin, environment.accessCode.value)
        tlvBuilder.append(TlvTag.Pin2, environment.passcode.value)
        tlvBuilder.append(TlvTag.CardId, environment.card?.cardId)
        tlvBuilder.append(TlvTag.UserSettingsMask, settings.mask)
        return CommandApdu(Instruction.SetUserSettings, tlvBuilder.serialize())
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): SetUserSettingsCommandResponse {
        val tlvData = apdu.getTlvData() ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)
        val mask: UserSettingsMask = decoder.decode(TlvTag.UserSettingsMask)
        return SetUserSettingsCommandResponse(
            cardId = decoder.decode(TlvTag.CardId),
            settings = UserSettings(mask),
        )
    }
}

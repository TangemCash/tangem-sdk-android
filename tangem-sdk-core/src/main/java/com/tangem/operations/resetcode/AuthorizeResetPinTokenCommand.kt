package com.tangem.operations.resetcode

import com.tangem.common.SuccessResponse
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.card.Card
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.SessionEnvironment
import com.tangem.common.core.TangemSdkError
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag
import com.tangem.operations.Command
import com.tangem.operations.PreflightReadMode

class AuthorizeResetPinTokenCommand(
    private val confirmationCard: ConfirmationCard,
) : Command<SuccessResponse>() {

    override fun requiresPasscode(): Boolean = false
    override fun preflightReadMode(): PreflightReadMode = PreflightReadMode.ReadCardOnly

    override fun performPreCheck(card: Card): TangemSdkError? {
        if (card.firmwareVersion < FirmwareVersion.BackupAvailable) {
            return TangemSdkError.NotSupportedFirmwareVersion()
        }
        if (card.backupStatus !is Card.BackupStatus.Active) {
            return TangemSdkError.NoActiveBackup()
        }
        return null
    }

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder().apply {
            append(TlvTag.CardId, environment.card?.cardId)
            append(TlvTag.InteractionMode, AuthorizeMode.TokenAuthenticate)
            append(TlvTag.Salt, confirmationCard.salt)
            append(TlvTag.BackupCardLinkingKey, confirmationCard.backupKey)
            append(TlvTag.BackupAttestSignature, confirmationCard.authorizeSignature)
        }
        return CommandApdu(Instruction.Authorize, tlvBuilder.serialize())
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): SuccessResponse {
        val tlvData = apdu.getTlvData()
            ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)
        return SuccessResponse(
            cardId = decoder.decode(TlvTag.CardId),
        )
    }
}

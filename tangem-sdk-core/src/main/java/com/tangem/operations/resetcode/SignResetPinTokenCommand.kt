package com.tangem.operations.resetcode

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
import com.tangem.operations.Command
import com.tangem.operations.PreflightReadMode

class SignResetPinTokenCommand(
    private val resetPinCard: ResetPinCard,
) : Command<ConfirmationCard>() {

    override fun requiresPasscode(): Boolean = false
    override fun preflightReadMode(): PreflightReadMode = PreflightReadMode.ReadCardOnly

    override fun performPreCheck(card: Card): TangemSdkError? {
        if (card.firmwareVersion < FirmwareVersion.BackupAvailable) {
            return TangemSdkError.ResetPinWrongCard(
                internalCode = TangemSdkError.NotSupportedFirmwareVersion().code,
            )
        }
        if (card.backupStatus !is Card.BackupStatus.Active) {
            return TangemSdkError.ResetPinWrongCard(
                internalCode = TangemSdkError.NoActiveBackup().code,
            )
        }
        if (card.cardId == resetPinCard.cardId) {
            return TangemSdkError.ResetPinWrongCard()
        }
        return null
    }

    override fun mapError(card: Card?, error: TangemError): TangemError {
        if (error is TangemSdkError.InvalidParams) {
            return TangemSdkError.ResetPinWrongCard()
        }
        return error
    }

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder().apply {
            append(TlvTag.CardId, environment.card?.cardId)
            append(TlvTag.InteractionMode, AuthorizeMode.TokenSign)
            append(TlvTag.Challenge, resetPinCard.token)
            append(TlvTag.PrimaryCardLinkingKey, resetPinCard.backupKey)
            append(TlvTag.BackupAttestSignature, resetPinCard.attestSignature)
        }
        return CommandApdu(Instruction.Authorize, tlvBuilder.serialize())
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): ConfirmationCard {
        val tlvData = apdu.getTlvData()
            ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)

        val isAccessCodeSet = environment.card?.isAccessCodeSet
        val isPasscodeSet = environment.card?.isPasscodeSet
        if (isPasscodeSet == null || isAccessCodeSet == null) {
            throw TangemSdkError.MissingPreflightRead()
        }

        return ConfirmationCard(
            cardId = decoder.decode(TlvTag.CardId),
            backupKey = decoder.decode(TlvTag.BackupCardLinkingKey),
            salt = decoder.decode(TlvTag.Salt),
            authorizeSignature = decoder.decode(TlvTag.BackupAttestSignature),
        )
    }
}

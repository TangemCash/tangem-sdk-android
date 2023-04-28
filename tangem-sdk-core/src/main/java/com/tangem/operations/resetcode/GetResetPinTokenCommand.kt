package com.tangem.operations.resetcode

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

class GetResetPinTokenCommand : Command<ResetPinCard>() {

    override fun requiresPasscode(): Boolean = false
    override fun preflightReadMode(): PreflightReadMode = PreflightReadMode.ReadCardOnly

    override fun performPreCheck(card: Card): TangemSdkError? {
        if (card.firmwareVersion < FirmwareVersion.BackupAvailable) {
            return TangemSdkError.NotSupportedFirmwareVersion()
        }
        if (card.backupStatus !is Card.BackupStatus.Active) {
            return TangemSdkError.NoActiveBackup()
        }
        if (!card.userSettings.isUserCodeRecoveryAllowed) {
            return TangemSdkError.UserCodeRecoveryDisabled()
        }
        return null
    }

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder().apply {
            append(TlvTag.CardId, environment.card?.cardId)
            append(TlvTag.InteractionMode, AuthorizeMode.TokenGet)
        }
        return CommandApdu(Instruction.Authorize, tlvBuilder.serialize())
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): ResetPinCard {
        val tlvData = apdu.getTlvData()
            ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)

        val isAccessCodeSet = environment.card?.isAccessCodeSet
        val isPasscodeSet = environment.card?.isPasscodeSet
        if (isPasscodeSet == null || isAccessCodeSet == null) {
            throw TangemSdkError.MissingPreflightRead()
        }

        return ResetPinCard(
            cardId = decoder.decode(TlvTag.CardId),
            backupKey = decoder.decode(TlvTag.PrimaryCardLinkingKey),
            attestSignature = decoder.decode(TlvTag.BackupAttestSignature),
            token = decoder.decode(TlvTag.Challenge),
            isAccessCodeSet = isAccessCodeSet,
            isPasscodeSet = isPasscodeSet,
        )
    }
}

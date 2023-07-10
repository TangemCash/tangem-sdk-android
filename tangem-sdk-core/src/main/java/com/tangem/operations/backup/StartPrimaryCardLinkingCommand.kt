package com.tangem.operations.backup

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

/**
 */
class StartPrimaryCardLinkingCommand : Command<RawPrimaryCard>() {

    override fun requiresPasscode(): Boolean = false

    override fun performPreCheck(card: Card): TangemSdkError? {
        if (card.firmwareVersion < FirmwareVersion.BackupAvailable) {
            return TangemSdkError.BackupFailedFirmware()
        }
        if (!card.settings.isBackupAllowed) {
            return TangemSdkError.BackupNotAllowed()
        }
        if (card.backupStatus?.canBackup != true) {
            return TangemSdkError.BackupFailedAlreadyCreated()
        }
        if (card.wallets.isEmpty()) {
            return TangemSdkError.BackupFailedEmptyWallets()
        }
        return null
    }

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.Pin, environment.accessCode.value)
        tlvBuilder.append(TlvTag.CardId, environment.card?.cardId)

        return CommandApdu(Instruction.StartPrimaryCardLinking, tlvBuilder.serialize())
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): RawPrimaryCard {
        val tlvData = apdu.getTlvData()
            ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)

        val cardPublicKey = environment.card?.cardPublicKey ?: throw TangemSdkError.UnknownError()

        val card = environment.card ?: throw TangemSdkError.MissingPreflightRead()

        return RawPrimaryCard(
            cardId = decoder.decode(TlvTag.CardId),
            cardPublicKey = cardPublicKey,
            linkingKey = decoder.decode(TlvTag.PrimaryCardLinkingKey),
            existingWalletsCount = card.wallets.size,
            isHDWalletAllowed = card.settings.isHDWalletAllowed,
            issuer = card.issuer,
            walletCurves = card.wallets.map { it.curve },
            batchId = card.batchId,
            firmwareVersion = card.firmwareVersion,
            isKeysImportAllowed = card.settings.isKeysImportAllowed,
        )
    }
}

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
class StartBackupCardLinkingCommand(
    private val primaryCardLinkingKey: ByteArray,
) : Command<RawBackupCard>() {

    override fun requiresPasscode(): Boolean = false

    override fun performPreCheck(card: Card): TangemSdkError? {
        if (card.firmwareVersion < FirmwareVersion.BackupAvailable) {
            return TangemSdkError.BackupFailedFirmware()
        }
        if (card.wallets.isNotEmpty()) {
            return TangemSdkError.BackupFailedNotEmptyWallets()
        }
        if (!card.settings.isBackupAllowed) {
            return TangemSdkError.BackupNotAllowed()
        }
        return null
    }

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.Pin, environment.accessCode.value)
        tlvBuilder.append(TlvTag.CardId, environment.card?.cardId)
        tlvBuilder.append(TlvTag.PrimaryCardLinkingKey, primaryCardLinkingKey)

        return CommandApdu(Instruction.StartBackupCardLinking, tlvBuilder.serialize())
    }

    override fun deserialize(
        environment: SessionEnvironment,
        apdu: ResponseApdu,
    ): RawBackupCard {
        val tlvData = apdu.getTlvData(environment.encryptionKey)
            ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)
        return RawBackupCard(
            cardId = decoder.decode(TlvTag.CardId),
            cardPublicKey = environment.card?.cardPublicKey ?: throw TangemSdkError.UnknownError(),
            linkingKey = decoder.decode(TlvTag.BackupCardLinkingKey),
            attestSignature = decoder.decode(TlvTag.CardSignature)
        )
    }
}


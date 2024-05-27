package com.tangem.operations.backup

import com.tangem.common.SuccessResponse
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.card.Card
import com.tangem.common.card.FirmwareVersion.Companion.KeysImportAvailable
import com.tangem.common.core.SessionEnvironment
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag
import com.tangem.operations.Command

class FinalizeReadBackupDataCommand(private val accessCode: ByteArray) : Command<SuccessResponse>() {

    override fun performPreCheck(card: Card): TangemError? {
        if (card.firmwareVersion < KeysImportAvailable) {
            return TangemSdkError.BackupFailedFirmware()
        }
        if (!card.settings.isBackupAllowed) {
            return TangemSdkError.BackupNotAllowed()
        }
        if (card.backupStatus == Card.BackupStatus.NoBackup) {
            return TangemSdkError.BackupFailedCardNotLinked()
        }
        if (card.wallets.isEmpty()) {
            return TangemSdkError.BackupFailedEmptyWallets()
        }
        return null
    }

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.CardId, environment.card?.cardId)
        tlvBuilder.append(TlvTag.Pin, accessCode)
        return CommandApdu(Instruction.FinalizeReadBackupData, tlvBuilder.serialize())
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): SuccessResponse {
        val tlvData = apdu.getTlvData() ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)
        return SuccessResponse(cardId = decoder.decode(TlvTag.CardId))
    }
}

package com.tangem.operations.backup

import com.squareup.moshi.JsonClass
import com.tangem.common.CompletionResult
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.card.Card
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.CardSession
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.SessionEnvironment
import com.tangem.common.core.TangemSdkError
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag
import com.tangem.operations.Command
import com.tangem.operations.CommandResponse

/**
 */
@JsonClass(generateAdapter = true)
class WriteBackupDataResponse(
    /**
     * Unique Tangem card ID number
     */
    val cardId: String,
    val backupStatus: Card.BackupRawStatus,
) : CommandResponse

/**
 */
class WriteBackupDataCommand(
    private val backupData: List<EncryptedBackupData>,
    private val accessCode: ByteArray,
    private val passcode: ByteArray,
) : Command<WriteBackupDataResponse>() {

    private var index: Int = 0

    override fun requiresPasscode(): Boolean = false

    override fun performPreCheck(card: Card): TangemSdkError? {
        if (card.firmwareVersion < FirmwareVersion.BackupAvailable) {
            return TangemSdkError.BackupFailedFirmware()
        }
        if (!card.settings.isBackupAllowed) {
            return TangemSdkError.BackupNotAllowed()
        }
        if (card.backupStatus == Card.BackupStatus.NoBackup) {
            return TangemSdkError.BackupFailedCardNotLinked()
        }
        return null
    }

    override fun run(session: CardSession, callback: CompletionCallback<WriteBackupDataResponse>) {
        writeData(session, callback)
    }

    private fun writeData(session: CardSession, callback: CompletionCallback<WriteBackupDataResponse>) {
        transceive(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    if (index == backupData.lastIndex) {
                        val backupStatus = session.environment.card?.backupStatus
                        if (backupStatus is Card.BackupStatus.CardLinked) {
                            session.environment.card = session.environment.card?.copy(
                                backupStatus = Card.BackupStatus.from(
                                    rawStatus = result.data.backupStatus,
                                    cardsCount = backupStatus.cardsCount,
                                ),
                            )
                        }
                        callback(CompletionResult.Success(result.data))
                    } else {
                        index += 1
                        writeData(session, callback)
                    }
                }

                is CompletionResult.Failure -> callback(result)
            }
        }
    }

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.CardId, environment.card?.cardId)
        tlvBuilder.append(TlvTag.Pin, accessCode)
        tlvBuilder.append(TlvTag.Pin2, passcode)
        tlvBuilder.append(TlvTag.Salt, backupData[index].salt)
        tlvBuilder.append(TlvTag.IssuerData, backupData[index].data)

        return CommandApdu(Instruction.WriteBackupData, tlvBuilder.serialize())
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): WriteBackupDataResponse {
        val tlvData = apdu.getTlvData()
            ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)

        return WriteBackupDataResponse(
            cardId = decoder.decode(TlvTag.CardId),
            backupStatus = decoder.decode(TlvTag.BackupStatus),
        )
    }
}

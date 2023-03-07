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
import com.tangem.common.extensions.guard
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag
import com.tangem.operations.Command
import com.tangem.operations.CommandResponse

/**
 * Response from the Tangem card after `ReadBackupDataCommand`.
 */
@JsonClass(generateAdapter = true)
class ReadBackupDataResponse(
    /**
     * Unique Tangem card ID number
     */
    val cardId: String,
    val data: List<EncryptedBackupData>,
    internal val index: Int,
) : CommandResponse

private class InternalReadBackupDataResponse(
    /**
     * Unique Tangem card ID number
     */
    var cardId: String = "",
    val data: MutableList<EncryptedBackupData> = mutableListOf(),
) : CommandResponse {

    fun update(response: ReadBackupDataResponse) {
        cardId = response.cardId
        data.addAll(response.data)
    }
}

/**
 */
class ReadBackupDataCommand(
    private val backupCardLinkingKey: ByteArray,
    private val accessCode: ByteArray,
) : Command<ReadBackupDataResponse>() {

    private var aggregatedResponse: InternalReadBackupDataResponse = InternalReadBackupDataResponse()
    private var readIndex: Int = 0

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
        if (card.wallets.isEmpty()) {
            return TangemSdkError.BackupFailedEmptyWallets()
        }
        return null
    }

    override fun run(session: CardSession, callback: CompletionCallback<ReadBackupDataResponse>) {
        readData(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    val status = session.environment.card?.backupStatus
                    if (status is Card.BackupStatus.CardLinked) {
                        session.environment.card = session.environment.card?.copy(
                            backupStatus = Card.BackupStatus.Active(status.cardCount)
                        )
                        val wallets = session.environment.card?.wallets
                            ?.map { it.copy(hasBackup = true) }
                            ?: emptyList()
                        session.environment.card?.setWallets(wallets)
                    }
                    callback(CompletionResult.Success(
                        ReadBackupDataResponse(
                            aggregatedResponse.cardId,
                            aggregatedResponse.data,
                            session.environment.card?.wallets?.size ?: 0
                        )
                    ))
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }

    private fun readData(session: CardSession, callback: CompletionCallback<Unit>) {
        val card = session.environment.card.guard {
            callback(CompletionResult.Failure(TangemSdkError.MissingPreflightRead()))
            return
        }
        transceive(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    aggregatedResponse.update(result.data)
                    if (result.data.index == card.settings.maxWalletsCount - 1) {
                        callback(CompletionResult.Success(Unit))
                    } else {
                        readIndex = result.data.index + 1
                        readData(session, callback)
                    }
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.CardId, environment.card?.cardId)
        tlvBuilder.append(TlvTag.Pin, accessCode)
        tlvBuilder.append(TlvTag.BackupCardLinkingKey, backupCardLinkingKey)

        return CommandApdu(Instruction.ReadBackupData, tlvBuilder.serialize())
    }

    override fun deserialize(
        environment: SessionEnvironment,
        apdu: ResponseApdu,
    ): ReadBackupDataResponse {
        val tlvData = apdu.getTlvData()
            ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)

        return ReadBackupDataResponse(
            cardId = decoder.decode(TlvTag.CardId),
            data = listOf(EncryptedBackupData(
                data = decoder.decode(TlvTag.IssuerData),
                salt = decoder.decode(TlvTag.Salt)
            )),
            index = decoder.decode(TlvTag.WalletIndex)
        )
    }
}

package com.tangem.operations.backup

import com.squareup.moshi.JsonClass
import com.tangem.common.CompletionResult
import com.tangem.common.UserCode
import com.tangem.common.UserCodeType
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
 * Response from the Tangem card after `LinkOriginCardCommand`.
 */
@JsonClass(generateAdapter = true)
class LinkPrimaryCardResponse(
    /**
     * Unique Tangem card ID number
     */
    val cardId: String,
    val backupStatus: Card.BackupRawStatus,
) : CommandResponse

/**
 */
class LinkPrimaryCardCommand(
    private val primaryCard: PrimaryCard,
    private val backupCards: List<BackupCard>,
    private val attestSignature: ByteArray,
    private val accessCode: ByteArray,
    private val passcode: ByteArray,
) : Command<LinkPrimaryCardResponse>() {

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

    override fun run(session: CardSession, callback: CompletionCallback<LinkPrimaryCardResponse>) {
        transceive(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    session.environment.accessCode =
                        UserCode(UserCodeType.AccessCode, value = accessCode)
                    session.environment.passcode =
                        UserCode(UserCodeType.Passcode, value = passcode)
                    complete(result.data, session, callback)
                }
                is CompletionResult.Failure -> {
                    when (result.error) {
                        is TangemSdkError.AccessCodeRequired, is TangemSdkError.PasscodeRequired -> {
                            val cardId = session.environment.card?.cardId.guard {
                                callback(result)
                                return@transceive
                            }
                            complete(
                                LinkPrimaryCardResponse(cardId, Card.BackupRawStatus.CardLinked),
                                session, callback
                            )
                        }
                        else -> callback(result)
                    }

                }
            }
        }
    }

    private fun complete(
        response: LinkPrimaryCardResponse,
        session: CardSession,
        callback: CompletionCallback<LinkPrimaryCardResponse>,
    ) {
        val card = session.environment.card
        session.environment.card = card?.copy(
            backupStatus = Card.BackupStatus.from(response.backupStatus, backupCards.size),
            settings = card.settings.copy(
                isSettingAccessCodeAllowed = true,
                isSettingPasscodeAllowed = true,
                isResettingUserCodesAllowed = false
            )
        )
        callback(CompletionResult.Success(response))
    }

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.CardId, environment.card?.cardId)
        tlvBuilder.append(TlvTag.Pin, environment.accessCode.value)
        tlvBuilder.append(TlvTag.Pin2, environment.passcode.value)
        tlvBuilder.append(TlvTag.PrimaryCardLinkingKey, primaryCard.linkingKey)
        tlvBuilder.append(TlvTag.Certificate, primaryCard.generateCertificate())
        tlvBuilder.append(TlvTag.BackupAttestSignature, attestSignature)
        tlvBuilder.append(TlvTag.NewPin, accessCode)
        tlvBuilder.append(TlvTag.NewPin2, passcode)

        backupCards
            .mapIndexed { index, card ->
                TlvBuilder().apply {
                    append(TlvTag.FileIndex, index)
                    append(TlvTag.BackupCardLinkingKey, card.linkingKey)
                }.serialize()
            }
            .forEach { tlvBuilder.append(TlvTag.BackupCardLink, it) }

        return CommandApdu(Instruction.LinkPrimaryCard, tlvBuilder.serialize())
    }

    override fun deserialize(
        environment: SessionEnvironment,
        apdu: ResponseApdu,
    ): LinkPrimaryCardResponse {
        val tlvData = apdu.getTlvData(environment.encryptionKey)
            ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)

        return LinkPrimaryCardResponse(
            cardId = decoder.decode(TlvTag.CardId),
            backupStatus = decoder.decode(TlvTag.BackupStatus)
        )
    }
}


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
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag
import com.tangem.operations.Command
import com.tangem.operations.CommandResponse

/**
 * Response from the Tangem card after `LinkBackupCardsCommand`.
 */
@JsonClass(generateAdapter = true)
class LinkBackupCardsResponse(
    /**
     * Unique Tangem card ID number
     */
    val cardId: String,
    val attestSignature: ByteArray,
) : CommandResponse

/**
 */
class LinkBackupCardsCommand(
    private val backupCards: List<BackupCard>,
    private val accessCode: ByteArray,
    private val passcode: ByteArray,
) : Command<LinkBackupCardsResponse>() {

    override fun requiresPasscode(): Boolean = true

    override fun performPreCheck(card: Card): TangemSdkError? {
        if (card.firmwareVersion < FirmwareVersion.BackupAvailable) {
            return TangemSdkError.BackupFailedFirmware()
        }
        if (!card.settings.isBackupAllowed) {
            return TangemSdkError.BackupNotAllowed()
        }
        if (card.wallets.isEmpty()) {
            return TangemSdkError.BackupFailedEmptyWallets()
        }
        return null
    }

    override fun run(session: CardSession, callback: CompletionCallback<LinkBackupCardsResponse>) {
        transceive(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    val card = session.environment.card
                    session.environment.card = card?.copy(
                        backupStatus = Card.BackupStatus.CardLinked(backupCards.size),
                        settings = card.settings.copy(
                            isSettingAccessCodeAllowed = true,
                            isSettingPasscodeAllowed = true,
                            isRemovingUserCodesAllowed = true,
                        ),
                    )
                    session.environment.accessCode =
                        UserCode(UserCodeType.AccessCode, value = accessCode)
                    session.environment.passcode =
                        UserCode(UserCodeType.Passcode, value = passcode)

                    callback(result)
                }
                is CompletionResult.Failure -> {
                    when (result.error) {
                        is TangemSdkError.AccessCodeRequired, is TangemSdkError.PasscodeRequired -> {
                            session.environment.accessCode =
                                UserCode(UserCodeType.AccessCode, accessCode)
                            session.environment.passcode = UserCode(UserCodeType.Passcode, passcode)
                            run(session, callback)
                        }
                        else -> callback(result)
                    }
                }
            }
        }
    }

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.CardId, environment.card?.cardId)
        tlvBuilder.append(TlvTag.Pin, environment.accessCode.value)
        tlvBuilder.append(TlvTag.Pin2, environment.passcode.value)
        tlvBuilder.append(TlvTag.BackupCount, backupCards.size)
        tlvBuilder.append(TlvTag.NewPin, accessCode)
        tlvBuilder.append(TlvTag.NewPin2, passcode)

        backupCards
            .mapIndexed { index, card ->
                TlvBuilder().apply {
                    append(TlvTag.FileIndex, index)
                    append(TlvTag.BackupCardLinkingKey, card.linkingKey)
                    append(TlvTag.Certificate, card.certificate)
                    append(TlvTag.CardSignature, card.attestSignature)
                }.serialize()
            }
            .forEach { tlvBuilder.append(TlvTag.BackupCardLink, it) }

        return CommandApdu(Instruction.LinkBackupCards, tlvBuilder.serialize())
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): LinkBackupCardsResponse {
        val tlvData = apdu.getTlvData()
            ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)
        return LinkBackupCardsResponse(
            decoder.decode(TlvTag.CardId),
            decoder.decode(TlvTag.BackupAttestSignature),
        )
    }
}

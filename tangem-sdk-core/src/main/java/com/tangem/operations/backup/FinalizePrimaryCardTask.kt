package com.tangem.operations.backup

import com.tangem.common.CompletionResult
import com.tangem.common.UserCode
import com.tangem.common.UserCodeType
import com.tangem.common.card.Card
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.guard

class FinalizePrimaryCardTask(
    private val backupCards: List<BackupCard>,
    private val accessCode: ByteArray,
    private val passcode: ByteArray,
    private val attestSignature: ByteArray?, //We already have attestSignature
    private val onLink: (ByteArray) -> Unit,
    private val onRead: (String, List<EncryptedBackupData>) -> Unit,
    private val readBackupStartIndex: Int,
) : CardSessionRunnable<Card> {

    private lateinit var card: Card

    override fun run(
        session: CardSession,
        callback: CompletionCallback<Card>,
    ) {
        card = session.environment.card.guard {
            callback(CompletionResult.Failure(TangemSdkError.MissingPreflightRead()))
            return
        }
        val backupStatus = card.backupStatus.guard {
            callback(CompletionResult.Failure(TangemSdkError.NotSupportedFirmwareVersion()))
            return
        }

        val linkAction = getLinkAction(backupStatus)

        if (linkAction == LinkAction.RETRY) { //We should swap codes only if they were set on the card.
            if (card.isAccessCodeSet) {
                session.environment.accessCode = UserCode(UserCodeType.AccessCode, accessCode)
            }
            if (card.isPasscodeSet == true) {
                session.environment.passcode = UserCode(UserCodeType.Passcode, passcode)
            }
        }

        if (linkAction != LinkAction.SKIP) {
            val command = LinkBackupCardsCommand(
                backupCards = backupCards, accessCode = accessCode, passcode = passcode
            )
            command.run(session) { linkResult ->
                when (linkResult) {
                    is CompletionResult.Success -> {
                        onLink(linkResult.data.attestSignature)
                        readBackupData(session = session, index = 0, callback = callback)
                    }
                    is CompletionResult.Failure -> callback(CompletionResult.Failure(linkResult.error))
                }
            }
        } else {
            readBackupData(session = session, index = readBackupStartIndex, callback = callback)
        }
    }

    private fun readBackupData(
        session: CardSession,
        index: Int,
        callback: CompletionCallback<Card>,
    ) {
        if (index > backupCards.lastIndex) {
            callback(CompletionResult.Success(session.environment.card!!))
            return
        }
        val currentBackupCard = backupCards[index]
        ReadBackupDataCommand(
            backupCardLinkingKey = currentBackupCard.linkingKey, accessCode = accessCode
        ).run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    onRead(currentBackupCard.cardId, result.data.data)
                    readBackupData(session, index + 1, callback)
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }

        }
    }

    private fun getLinkAction(status: Card.BackupStatus): LinkAction {
        return when (status) {
            is Card.BackupStatus.Active, is Card.BackupStatus.CardLinked -> {
                if (attestSignature != null) {
                    //We already have attest signature and card already linked. Can skip linking
                    LinkAction.SKIP
                } else {
                    //We don't have attest signature, but card already linked.
                    // Force retry with new user codes
                    LinkAction.RETRY
                }
            }
            Card.BackupStatus.NoBackup -> {
                LinkAction.LINK
            }
        }
    }

    enum class LinkAction {
        LINK,
        SKIP,
        RETRY
    }
}


package com.tangem.operations.backup

import com.tangem.common.CompletionResult
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.guard

class StartBackupCardLinkingTask(
    private val primaryCard: PrimaryCard,
    private val addedBackupCards: List<String>,
) : CardSessionRunnable<BackupCard> {

    private var command: StartBackupCardLinkingCommand? = null

    override fun run(session: CardSession, callback: CompletionCallback<BackupCard>) {
        val card = session.environment.card.guard {
            callback(CompletionResult.Failure(TangemSdkError.MissingPreflightRead()))
            return
        }

        val primaryWalletCurves = primaryCard.walletCurves
        val backupCardSupportedCurves = card.supportedCurves

        if (!card.issuer.publicKey.contentEquals(primaryCard.issuer.publicKey)) {
            callback(CompletionResult.Failure(TangemSdkError.BackupFailedWrongIssuer()))
            return
        }

        if (card.settings.isHDWalletAllowed != primaryCard.isHDWalletAllowed) {
            callback(CompletionResult.Failure(TangemSdkError.BackupFailedHDWalletSettings()))
            return
        }

        if (!backupCardSupportedCurves.containsAll(primaryWalletCurves)) {
            callback(CompletionResult.Failure(TangemSdkError.BackupFailedNotEnoughCurves()))
            return
        }

        if (primaryCard.existingWalletsCount > card.settings.maxWalletsCount) {
            callback(CompletionResult.Failure(TangemSdkError.BackupFailedNotEnoughWallets()))
            return
        }

        if (card.cardId.lowercase() == primaryCard.cardId.lowercase()) {
            callback(CompletionResult.Failure(TangemSdkError.BackupCardRequired()))
            return
        }

        if (addedBackupCards.contains(card.cardId)) {
            callback(CompletionResult.Failure(TangemSdkError.BackupCardAlreadyAdded()))
            return
        }

        command = StartBackupCardLinkingCommand(primaryCard.linkingKey)
        command?.run(session, callback)
    }
}
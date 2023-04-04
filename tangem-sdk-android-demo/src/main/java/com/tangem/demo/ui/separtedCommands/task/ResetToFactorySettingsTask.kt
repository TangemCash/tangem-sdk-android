package com.tangem.demo.ui.separtedCommands.task

import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.extensions.guard
import com.tangem.operations.backup.ResetBackupCommand
import com.tangem.operations.wallet.PurgeWalletCommand

class ResetToFactorySettingsTask : CardSessionRunnable<Card> {

    override fun run(session: CardSession, callback: (result: CompletionResult<Card>) -> Unit) {
        deleteWallets(session, callback)
    }

    private fun deleteWallets(session: CardSession, callback: (result: CompletionResult<Card>) -> Unit) {
        val wallet = session.environment.card?.wallets?.lastOrNull().guard {
            resetBackup(session, callback)
            return
        }

        PurgeWalletCommand(wallet.publicKey).run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    deleteWallets(session, callback)
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }

    private fun resetBackup(session: CardSession, callback: (result: CompletionResult<Card>) -> Unit) {
        val backupStatus = session.environment.card?.backupStatus
        if (backupStatus == null || backupStatus == Card.BackupStatus.NoBackup) {
            callback(CompletionResult.Success(session.environment.card!!))
            return
        }

        ResetBackupCommand().run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    callback(CompletionResult.Success(session.environment.card!!))
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }
}

package com.tangem.operations.backup

import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.guard
import com.tangem.operations.read.ReadWalletsListCommand

class FinalizeBackupCardTask(
    private val primaryCard: PrimaryCard,
    private val backupCards: List<BackupCard>,
    private val backupData: List<EncryptedBackupData>,
    private val attestSignature: ByteArray,
    private val accessCode: ByteArray,
    private val passcode: ByteArray,
) : CardSessionRunnable<Card> {

    override val allowsRequestAccessCodeFromRepository: Boolean
        get() = false

    override fun run(session: CardSession, callback: CompletionCallback<Card>) {
        val card = session.environment.card.guard {
            callback(CompletionResult.Failure(TangemSdkError.MissingPreflightRead()))
            return
        }
        if (card.backupStatus == Card.BackupStatus.NoBackup) {
            val command = LinkPrimaryCardCommand(
                primaryCard = primaryCard,
                backupCards = backupCards,
                attestSignature = attestSignature,
                accessCode = accessCode,
                passcode = passcode
            )

            command.run(session) { linkResult ->
                when (linkResult) {
                    is CompletionResult.Success -> {
                        writeBackupData(session, callback)
                    }
                    is CompletionResult.Failure -> callback(CompletionResult.Failure(linkResult.error))
                }
            }
        } else {
            writeBackupData(session, callback)
        }
    }

    private fun writeBackupData(
        session: CardSession,
        callback: CompletionCallback<Card>,
    ) {
        val writeCommand = WriteBackupDataCommand(backupData, accessCode, passcode)

        writeCommand.run(session) { writeResult ->
            when (writeResult) {
                is CompletionResult.Success -> {
                    if (writeResult.data.backupStatus == Card.BackupRawStatus.Active) {
                        readWallets(session, callback)
                    } else {
                        callback(CompletionResult.Failure(TangemSdkError.UnknownError()))
                    }
                }
                is CompletionResult.Failure ->
                    callback(CompletionResult.Failure(writeResult.error))
            }
        }
    }

    private fun readWallets(
        session: CardSession,
        callback: CompletionCallback<Card>,
    ) {
        ReadWalletsListCommand().run(session) { result ->
            when (result) {
                is CompletionResult.Success ->
                    callback(CompletionResult.Success(session.environment.card!!))
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }
}

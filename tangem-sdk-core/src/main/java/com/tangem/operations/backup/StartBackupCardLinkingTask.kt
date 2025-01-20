package com.tangem.operations.backup

import com.squareup.moshi.JsonClass
import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.guard
import com.tangem.operations.attestation.AttestationTask

@JsonClass(generateAdapter = true)
data class StartBackupCardLinkingTaskResponse(
    val backupCard: BackupCard,
    val card: Card,
)

class StartBackupCardLinkingTask(
    private val primaryCard: PrimaryCard,
    private val addedBackupCards: List<String>,
    private val skipCompatibilityChecks: Boolean = false,
) : CardSessionRunnable<StartBackupCardLinkingTaskResponse> {

    override val allowsRequestAccessCodeFromRepository: Boolean
        get() = false

    @Suppress("CyclomaticComplexMethod")
    override fun run(session: CardSession, callback: CompletionCallback<StartBackupCardLinkingTaskResponse>) {
        val card = session.environment.card.guard {
            callback(CompletionResult.Failure(TangemSdkError.MissingPreflightRead()))
            return
        }

        val primaryWalletCurves = primaryCard.walletCurves
        val backupCardSupportedCurves = card.supportedCurves

        if (!skipCompatibilityChecks) {
            if (!card.issuer.publicKey.contentEquals(primaryCard.issuer.publicKey)) {
                callback(CompletionResult.Failure(TangemSdkError.BackupFailedWrongIssuer()))
                return
            }

            if (card.settings.isHDWalletAllowed != primaryCard.isHDWalletAllowed) {
                callback(CompletionResult.Failure(TangemSdkError.BackupFailedHDWalletSettings()))
                return
            }

            if (!isBatchIdCompatible(card.batchId)) {
                callback(CompletionResult.Failure(TangemSdkError.BackupFailedIncompatibleBatch()))
                return
            }

            if (primaryCard.firmwareVersion != null && primaryCard.firmwareVersion != card.firmwareVersion) {
                callback(CompletionResult.Failure(TangemSdkError.BackupFailedIncompatibleFirmware()))
                return
            }
        }

        if (primaryCard.isKeysImportAllowed != null &&
            primaryCard.isKeysImportAllowed != card.settings.isKeysImportAllowed
        ) {
            callback(CompletionResult.Failure(TangemSdkError.BackupFailedKeysImportSettings()))
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

        StartBackupCardLinkingCommand(primaryCard.linkingKey)
            .run(session) { result ->
                when (result) {
                    is CompletionResult.Success -> {
                        runAttestation(
                            session = session,
                            response = StartBackupCardLinkingTaskResponse(backupCard = result.data, card = card),
                            callback = callback,
                        )
                    }

                    is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
                }
            }
    }

    private fun runAttestation(
        session: CardSession,
        response: StartBackupCardLinkingTaskResponse,
        callback: CompletionCallback<StartBackupCardLinkingTaskResponse>,
    ) {
        val mode = session.environment.config.attestationMode
        val secureStorage = session.environment.secureStorage
        val attestationTask = AttestationTask(mode, secureStorage)

        attestationTask.run(session) { result ->
            when (result) {
                is CompletionResult.Success -> callback(CompletionResult.Success(response))
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }

    private fun isBatchIdCompatible(batchId: String): Boolean {
        val primaryCardBatchId = primaryCard.batchId?.uppercase()
            ?: return true // We found the old interrupted backup. Skip this check.

        val backupCardBatchId = batchId.uppercase()

        if (backupCardBatchId == primaryCardBatchId) return true

        if (isBatchDetached(backupCardBatchId) || isBatchDetached(primaryCardBatchId)) return false

        return true
    }

    companion object {
        private val detachedBatches = listOf("AC01", "AC02", "CB95")

        private fun isBatchDetached(batchId: String): Boolean {
            return detachedBatches.contains(batchId)
        }
    }
}

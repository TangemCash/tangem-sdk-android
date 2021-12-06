package com.tangem.operations.backup

import com.tangem.common.CompletionResult
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.guard
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.services.Result
import com.tangem.crypto.sign
import com.tangem.operations.attestation.OnlineCardVerifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StartBackupCardLinkingTask(
    private val primaryCard: PrimaryCard,
    private val addedBackupCards: List<String>,
) : CardSessionRunnable<BackupCard> {

    private val onlineCardVerifier: OnlineCardVerifier = OnlineCardVerifier()

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

        StartBackupCardLinkingCommand(primaryCard.linkingKey)
            .run(session) { result ->
                when (result) {
                    is CompletionResult.Success -> {
                        loadIssuerSignature(result.data, session, callback)
                    }
                    is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
                }
            }
    }

    private fun loadIssuerSignature(
        rawCard: RawBackupCard,
        session: CardSession, callback: CompletionCallback<BackupCard>,
    ) {
        if (session.environment.card?.firmwareVersion?.type == FirmwareVersion.FirmwareType.Sdk) {
            val issuerPrivateKey =
                "11121314151617184771ED81F2BACF57479E4735EB1405083927372D40DA9E92".hexToBytes()
            val issuerSignature = rawCard.cardPublicKey.sign(issuerPrivateKey)
            callback(CompletionResult.Success(BackupCard(rawCard, issuerSignature)))
            return
        }

        session.scope.launch(Dispatchers.IO) {
            when (val result =
                onlineCardVerifier.getCardData(rawCard.cardId, rawCard.cardPublicKey)) {
                is Result.Success -> {
                    val signature = result.data.issuerSignature.guard {
                        callback(CompletionResult.Failure(TangemSdkError.IssuerSignatureLoadingFailed()))
                        return@launch
                    }
                    val backupCard = BackupCard(rawCard, signature.hexToBytes())
                    callback(CompletionResult.Success(backupCard))
                }
                is Result.Failure ->
                    callback(CompletionResult.Failure(TangemSdkError.IssuerSignatureLoadingFailed()))
            }
        }
    }
}
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

class StartPrimaryCardLinkingTask : CardSessionRunnable<PrimaryCard> {

    private val onlineCardVerifier: OnlineCardVerifier = OnlineCardVerifier()

    override fun run(session: CardSession, callback: CompletionCallback<PrimaryCard>) {
        StartPrimaryCardLinkingCommand()
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
        rawCard: RawPrimaryCard,
        session: CardSession,
        callback: CompletionCallback<PrimaryCard>,
    ) {
        if (session.environment.card?.firmwareVersion?.type == FirmwareVersion.FirmwareType.Sdk) {
            val issuerPrivateKey =
                "11121314151617184771ED81F2BACF57479E4735EB1405083927372D40DA9E92".hexToBytes()
            val issuerSignature = rawCard.cardPublicKey.sign(issuerPrivateKey)
            callback(CompletionResult.Success(PrimaryCard(rawCard, issuerSignature)))
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
                    val primaryCard = PrimaryCard(rawCard, issuerSignature = signature.hexToBytes())
                    callback(CompletionResult.Success(primaryCard))
                }
                is Result.Failure ->
                    callback(CompletionResult.Failure(TangemSdkError.IssuerSignatureLoadingFailed()))
            }
        }
    }
}
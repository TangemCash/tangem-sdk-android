package com.tangem.operations.backup

import com.tangem.common.CompletionResult
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.guard
import com.tangem.operations.attestation.AttestationTask

class StartPrimaryCardLinkingTask : CardSessionRunnable<PrimaryCard> {

    private var attestationTask: AttestationTask? = null

    override fun run(session: CardSession, callback: CompletionCallback<PrimaryCard>) {
        StartPrimaryCardLinkingCommand()
            .run(session) { result ->
                when (result) {
                    is CompletionResult.Success -> {
                        runAttestation(result.data, session, callback)
                    }
                    is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
                }
            }
    }

    private fun runAttestation(
        rawCard: RawPrimaryCard,
        session: CardSession, callback: CompletionCallback<PrimaryCard>,
    ) {
        attestationTask = AttestationTask(
            mode = AttestationTask.Mode.Full,
            secureStorage = session.environment.secureStorage
        )
        attestationTask?.run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    val signature = session.environment.card?.issuerSignature.guard {
                        callback(CompletionResult.Failure(TangemSdkError.CertificateSignatureRequired()))
                        return@run
                    }
                    val primaryCard = PrimaryCard(rawCard, issuerSignature = signature)
                    callback(CompletionResult.Success(primaryCard))
                }
                is CompletionResult.Failure -> {
                    callback(CompletionResult.Failure(result.error))
                }
            }

        }
    }
}
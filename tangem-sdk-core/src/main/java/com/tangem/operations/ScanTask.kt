package com.tangem.operations

import com.tangem.*
import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.*
import com.tangem.operations.*
import com.tangem.operations.attestation.Attestation
import com.tangem.operations.attestation.AttestationTask

/**
 * Task that allows to read Tangem card and verify its private key.
 * Returns data from a Tangem card after successful completion of [ReadCommand]
 * and [AttestWalletKeyCommand], subsequently.
 */
class ScanTask : CardSessionRunnable<Card> {

    override fun run(session: CardSession, callback: CompletionCallback<Card>) {
        if (session.environment.card == null) {
            callback(CompletionResult.Failure(TangemSdkError.MissingPreflightRead()))
            return
        }
        runAttestation(session, callback)
    }

    private fun runAttestation(session: CardSession, callback: CompletionCallback<Card>) {
        val mode = session.environment.config.attestationMode
        val secureStorage = session.environment.secureStorage
        val attestationTask = AttestationTask(mode, secureStorage)

        attestationTask.run(session) { result ->
            when (result) {
                is CompletionResult.Success -> processAttestationReport(result.data, attestationTask, session, callback)
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }

    private fun processAttestationReport(
        report: Attestation,
        attestationTask: AttestationTask,
        session: CardSession,
        callback: CompletionCallback<Card>
    ) {
        when (report.status) {
            Attestation.Status.Failed, Attestation.Status.Skipped -> {
                val isDevelopmentCard = session.environment.card!!.firmwareVersion.type ==
                        FirmwareVersion.FirmwareType.Sdk

                //Possible production sample or development card
                if (isDevelopmentCard || session.environment.config.allowUntrustedCards) {
                    session.viewDelegate.attestationDidFail(isDevelopmentCard, {
                        callback(CompletionResult.Success(session.environment.card!!))
                    }) {
                        callback(CompletionResult.Failure(TangemSdkError.UserCancelled()))
                    }
                } else {
                    callback(CompletionResult.Failure(TangemSdkError.CardVerificationFailed()))
                }
            }
            Attestation.Status.Verified -> {
                callback(CompletionResult.Success(session.environment.card!!))
            }
            Attestation.Status.VerifiedOffline -> {
                session.viewDelegate.attestationCompletedOffline({
                    callback(CompletionResult.Success(session.environment.card!!))
                }, {
                    callback(CompletionResult.Failure(TangemSdkError.UserCancelled()))
                }, {
                    attestationTask.retryOnline(session) { result ->
                        when (result) {
                            is CompletionResult.Success -> {
                                processAttestationReport(result.data, attestationTask, session, callback)
                            }
                            is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
                        }
                    }
                })
            }
            Attestation.Status.Warning -> {
                session.viewDelegate.attestationCompletedWithWarnings {
                    callback(CompletionResult.Success(session.environment.card!!))
                }
            }
        }
    }
}
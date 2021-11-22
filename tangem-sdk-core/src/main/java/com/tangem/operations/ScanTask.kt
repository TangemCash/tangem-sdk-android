package com.tangem.operations

import com.tangem.*
import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.*
import com.tangem.operations.*
import com.tangem.operations.attestation.Attestation
import com.tangem.operations.attestation.AttestationTask
import com.tangem.operations.pins.CheckUserCodesCommand

/**
 * Task that allows to read Tangem card and verify its private key.
 * Returns data from a Tangem card after successful completion of [ReadCommand]
 * and [AttestWalletKeyCommand], subsequently.
 */
class ScanTask : CardSessionRunnable<Card> {

    override fun run(session: CardSession, callback: CompletionCallback<Card>) {
        val card = session.environment.card
        if (card == null) {
            callback(CompletionResult.Failure(TangemSdkError.MissingPreflightRead()))
            return
        }

        // We have to retrieve passcode status information for cards with COS before v4.01 with checkUserCodes
        // command for backward compatibility.
        // checkUserCodes command for cards with COS <=1.19 not supported because of persistent SD.
        // We cannot run checkUserCodes command for cards whose `isResettingUserCodesAllowed` is set to false
        // because of an error
        if (card.firmwareVersion < FirmwareVersion.IsPasscodeStatusAvailable
                && card.firmwareVersion.doubleValue > 1.19
                && card.settings.isResettingUserCodesAllowed
        ) {
            checkUserCodes(session, callback)
        } else {
            runAttestation(session, callback)
        }
    }

    private fun checkUserCodes(session: CardSession, callback: CompletionCallback<Card>) {
        CheckUserCodesCommand().run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    session.environment.card = session.environment.card?.copy(isPasscodeSet = result.data.isPasscodeSet)
                    runAttestation(session, callback)
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
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
                if (session.environment.config.attestationMode == AttestationTask.Mode.Offline){
                    callback(CompletionResult.Success(session.environment.card!!))
                    return
                }

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
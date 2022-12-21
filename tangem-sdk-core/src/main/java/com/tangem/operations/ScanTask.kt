package com.tangem.operations

import com.tangem.*
import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.*
import com.tangem.common.extensions.guard
import com.tangem.common.extensions.toMapKey
import com.tangem.operations.*
import com.tangem.operations.attestation.AttestationTask
import com.tangem.operations.derivation.DeriveMultipleWalletPublicKeysTask
import com.tangem.operations.pins.CheckUserCodesCommand

/**
 * Task that allows to read Tangem card and verify its private key.
 * Returns data from a Tangem card after successful completion of [ReadCommand]
 * and [AttestWalletKeyCommand], subsequently.
 */
class ScanTask(
    override val allowsRequestAccessCodeFromRepository: Boolean = false,
) : CardSessionRunnable<Card> {

    override fun run(session: CardSession, callback: CompletionCallback<Card>) {
        val card = session.environment.card.guard {
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
            deriveKeysIfNeeded(session, callback)
        }
    }

    private fun checkUserCodes(session: CardSession, callback: CompletionCallback<Card>) {
        CheckUserCodesCommand().run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    session.environment.card =
                        session.environment.card?.copy(isPasscodeSet = result.data.isPasscodeSet)
                    runAttestation(session, callback)
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }

    private fun deriveKeysIfNeeded(session: CardSession, callback: CompletionCallback<Card>) {
        val card = session.environment.card.guard {
            callback(CompletionResult.Failure(TangemSdkError.MissingPreflightRead()))
            return
        }

        val defaultPaths = session.environment.config.defaultDerivationPaths

        if (card.firmwareVersion < FirmwareVersion.HDWalletAvailable
            || !card.settings.isHDWalletAllowed || defaultPaths.isEmpty()
        ) {
            runAttestation(session, callback)
            return
        }

        val derivations = card.wallets.mapNotNull { wallet ->
            val paths = defaultPaths[wallet.curve]
            if (!paths.isNullOrEmpty()) wallet.publicKey.toMapKey() to paths else null
        }.toMap()

        if (derivations.isEmpty()) {
            runAttestation(session, callback)
            return
        }

        DeriveMultipleWalletPublicKeysTask(derivations).run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
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
                is CompletionResult.Success -> {
                    val card = session.environment.card.guard {
                        callback(CompletionResult.Failure(TangemSdkError.MissingPreflightRead()))
                        return@run
                    }
                    callback(CompletionResult.Success(card))
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }
}

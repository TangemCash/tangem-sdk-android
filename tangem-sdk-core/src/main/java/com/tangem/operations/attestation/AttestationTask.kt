package com.tangem.operations.attestation

import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.guard
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.common.services.Result
import com.tangem.common.services.TrustedCardsRepo
import com.tangem.common.services.secure.SecureStorage
import com.tangem.common.services.toTangemSdkError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import java.util.*

class AttestationTask(
    private val mode: Mode,
    secureStorage: SecureStorage
) : CardSessionRunnable<Attestation> {

    /**
     * If `true`, AttestationTask will not pause nfc session after all card operations complete. Useful
     * for chaining  tasks after AttestationTask. False by default
     */
    var shouldKeepSessionOpened = false

    /**
     * Attest wallet count or sign command count greater this value is looks suspicious.
     */
    private val maxCounter = 100000
    private val trustedCardsRepo = TrustedCardsRepo(secureStorage, MoshiJsonConverter.INSTANCE)
    private val onlineCardVerifier: OnlineCardVerifier = OnlineCardVerifier()

    private var currentAttestationStatus: Attestation = Attestation.empty

    private var onlineAttestationChannel = ConflatedBroadcastChannel<CompletionResult<Unit>>()
    private var onlineAttestationSubscription: ReceiveChannel<CompletionResult<*>>? = null

    override fun run(session: CardSession, callback: CompletionCallback<Attestation>) {
        session.environment.card.guard {
            callback(CompletionResult.Failure(TangemSdkError.MissingPreflightRead()))
            return
        }
        attestCard(session, callback)
    }

    fun retryOnline(session: CardSession, callback: CompletionCallback<Attestation>) {
        onlineAttestationSubscription = null
        onlineAttestationChannel.cancel()
        onlineAttestationChannel = ConflatedBroadcastChannel()

        val card = session.environment.card.guard {
            callback(CompletionResult.Failure(TangemSdkError.MissingPreflightRead()))
            return
        }

        runOnlineAttestation(session.scope, card)
        waitForOnlineAndComplete(session, callback)
    }

    private fun attestCard(session: CardSession, callback: CompletionCallback<Attestation>) {
        AttestCardKeyCommand().run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    //This card already attested with the current or more secured mode
                    val attestation = trustedCardsRepo.attestation(session.environment.card!!.cardPublicKey)
                    if (attestation != null && attestation.mode.ordinal >= mode.ordinal) {
                        currentAttestationStatus = attestation
                        complete(session, callback)
                    } else {
                        //Continue attestation
                        currentAttestationStatus = currentAttestationStatus.copy(
                                cardKeyAttestation = Attestation.Status.VerifiedOffline
                        )
                        continueAttestation(session, callback)
                    }
                }
                is CompletionResult.Failure -> {
                    //Card attestation failed. Update status and continue attestation
                    if (result.error is TangemSdkError.CardVerificationFailed) {
                        currentAttestationStatus = currentAttestationStatus.copy(
                                cardKeyAttestation = Attestation.Status.Failed
                        )
                        continueAttestation(session, callback)
                    } else {
                        callback(CompletionResult.Failure(result.error))
                    }
                }
            }
        }
    }

    private fun continueAttestation(session: CardSession, callback: CompletionCallback<Attestation>) {
        runOnlineAttestation(session.scope, session.environment.card!!)

        when (mode) {
            Mode.Normal -> waitForOnlineAndComplete(session, callback)
            Mode.Full -> runWalletsAttestation(session, callback)
        }
    }

    private fun runWalletsAttestation(session: CardSession, callback: CompletionCallback<Attestation>) {
        attestWallets(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    //Wallets attestation completed. Update status and continue attestation
                    val hasWarnings = result.data
                    val status = if (hasWarnings) Attestation.Status.Warning else Attestation.Status.Verified
                    currentAttestationStatus = currentAttestationStatus.copy(walletKeysAttestation = status)
                    runExtraAttestation(session, callback)
                }
                is CompletionResult.Failure -> {
                    //Wallets attestation failed. Update status and continue attestation
                    if (result.error is TangemSdkError.CardVerificationFailed) {
                        currentAttestationStatus = currentAttestationStatus.copy(
                                walletKeysAttestation = Attestation.Status.Failed
                        )
                        runExtraAttestation(session, callback)
                    } else {
                        callback(CompletionResult.Failure(result.error))
                    }
                }
            }
        }
    }

    private fun runExtraAttestation(session: CardSession, callback: CompletionCallback<Attestation>) {
        //TODO: ATTEST_CARD_FIRMWARE, ATTEST_CARD_UNIQUENESS
        waitForOnlineAndComplete(session, callback)
    }

    private fun attestWallets(session: CardSession, callback: CompletionCallback<Boolean>) {
        session.scope.launch {
            val card = session.environment.card!!
            val walletsKeys = card.wallets.map { it.publicKey }
            val attestationCommands = walletsKeys.map { AttestWalletKeyCommand(it) }

            //check for hacking attempts with signs
            var hasWarnings = card.wallets.mapNotNull { it.totalSignedHashes }.any { it > maxCounter }
            var shouldReturn = false
            var flowIsCompleted = false

            flow {
                attestationCommands.forEach { emit(it) }
            }.onCompletion {
                flowIsCompleted = true
            }.collect {
                if (shouldReturn) return@collect

                it.run(session) { result ->
                    when (result) {
                        is CompletionResult.Success -> {
                            //check for hacking attempts with attestWallet
                            if (result.data.counter != null && result.data.counter > maxCounter) {
                                hasWarnings = true
                            }
                            if (flowIsCompleted) callback(CompletionResult.Success(hasWarnings))
                        }
                        is CompletionResult.Failure -> {
                            shouldReturn = true
                            callback(CompletionResult.Failure(result.error))
                        }
                    }
                }
            }
        }
    }

    /**
     * Dev card will not pass online attestation. Or, if the card already failed offline attestation,
     * we can skip online part. So, we can send the error to the publisher immediately
     */
    private fun runOnlineAttestation(scope: CoroutineScope, card: Card) {
        scope.launch(Dispatchers.IO) {
            val isSdkCard = card.firmwareVersion.type == FirmwareVersion.FirmwareType.Sdk
            val isAttestationFailed = card.attestation.cardKeyAttestation == Attestation.Status.Failed
            if (isSdkCard || isAttestationFailed) {
                onlineAttestationChannel.send(CompletionResult.Failure(TangemSdkError.CardVerificationFailed()))
                return@launch
            }

            when (val result = onlineCardVerifier.getCardInfo(card.cardId, card.cardPublicKey)) {
                is Result.Success -> onlineAttestationChannel.send(CompletionResult.Success(Unit))
                is Result.Failure -> onlineAttestationChannel.send(CompletionResult.Failure(result.toTangemSdkError()))
            }
        }
    }

    private fun waitForOnlineAndComplete(session: CardSession, callback: CompletionCallback<Attestation>) {
        if (onlineAttestationSubscription != null) return

        if (!shouldKeepSessionOpened) {
            session.pause()
        }

        session.scope.launch {
            onlineAttestationSubscription = onlineAttestationChannel.openSubscription()
            onlineAttestationSubscription?.consumeEach { result ->
                when (result) {
                    is CompletionResult.Success -> {
                        //We assume, that card verified, because we skip online attestation for dev cards and cards that failed keys attestation
                        currentAttestationStatus = currentAttestationStatus.copy(
                                cardKeyAttestation = Attestation.Status.Verified
                        )
                        trustedCardsRepo.append(session.environment.card!!.cardPublicKey, currentAttestationStatus)
                        complete(session, callback)
                    }
                    is CompletionResult.Failure -> {
                        //We interest only in cardVerificationFailed error, ignore network errors
                        if (result.error is TangemSdkError.CardVerificationFailed) {
                            currentAttestationStatus = currentAttestationStatus.copy(
                                    cardKeyAttestation = Attestation.Status.Failed
                            )
                        }
                        complete(session, callback)
                    }
                }
            }
        }
    }

    private fun complete(session: CardSession, callback: CompletionCallback<Attestation>) {
        session.environment.card = session.environment.card?.copy(attestation = currentAttestationStatus)
        callback(CompletionResult.Success(currentAttestationStatus))

        onlineAttestationChannel.cancel()
        onlineAttestationSubscription = null
    }

    enum class Mode {
        Normal,
        Full
    }
}
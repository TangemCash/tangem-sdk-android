package com.tangem.tasks

import com.tangem.*
import com.tangem.commands.common.card.Card
import com.tangem.commands.read.ReadCommand
import com.tangem.commands.read.ReadWalletCommand
import com.tangem.commands.read.ReadWalletListCommand
import com.tangem.commands.wallet.CardWallet
import com.tangem.commands.wallet.WalletIndex
import com.tangem.common.CompletionResult

/**
 * Created by Anton Zhilenkov on 26/03/2021.
 */
interface PreflightReadCapable {
    fun needPreflightRead(): Boolean = true
    fun preflightReadSettings(): PreflightReadSettings = PreflightReadSettings.ReadCardOnly
}

sealed class PreflightReadSettings {

    object ReadCardOnly : PreflightReadSettings() {
        override fun toString(): String = this::class.java.simpleName
    }

    data class ReadWallet(val walletIndex: WalletIndex) : PreflightReadSettings() {
        override fun toString(): String = walletIndex.toString()
    }

    object FullCardRead : PreflightReadSettings() {
        override fun toString(): String = this::class.java.simpleName
    }
}

class PreflightReadTask(
    private val readSettings: PreflightReadSettings,
    private val cardId: String? = null,
) : CardSessionRunnable<Card> {

    override val requiresPin2: Boolean = false

    override fun run(session: CardSession, callback: (result: CompletionResult<Card>) -> Unit) {
        Log.debug { "================ Perform preflight check with settings: $readSettings) ================" }
        ReadCommand().run(session) readCommandRun@{ result ->
            when (result) {
                is CompletionResult.Success -> {
                    if (cardId != null && cardId != result.data.cardId) {
                        callback(CompletionResult.Failure(TangemSdkError.WrongCardNumber()))
                        return@readCommandRun
                    }
                    if (!session.environment.cardFilter.allowedCardTypes
                            .contains(result.data.firmwareVersion.type)
                    ) {
                        callback(CompletionResult.Failure(TangemSdkError.WrongCardType()))
                        return@readCommandRun
                    }
                    finalizeRead(session, result.data, callback)
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }

    private fun finalizeRead(session: CardSession, card: Card, callback: (result: CompletionResult<Card>) -> Unit) {
        if (card.firmwareVersion < FirmwareConstraints.AvailabilityVersions.walletData ||
            readSettings == PreflightReadSettings.ReadCardOnly) {
            callback(CompletionResult.Success(card))
            return
        }

        fun handleSuccess(card: Card, wallets: List<CardWallet>, callback: (result: CompletionResult<Card>) -> Unit) {
            session.environment.card = card.setWallets(wallets)
            callback(CompletionResult.Success(session.environment.card!!))
        }

        when (readSettings) {
            is PreflightReadSettings.ReadWallet -> {
                ReadWalletCommand(readSettings.walletIndex).run(session) {
                    when (it) {
                        is CompletionResult.Success -> handleSuccess(card, listOf(it.data.wallet), callback)
                        is CompletionResult.Failure -> callback(CompletionResult.Failure(it.error))
                    }
                }
            }
            PreflightReadSettings.FullCardRead -> {
                ReadWalletListCommand().run(session) {
                    when (it) {
                        is CompletionResult.Success -> handleSuccess(card, it.data.wallets, callback)
                        is CompletionResult.Failure -> callback(CompletionResult.Failure(it.error))
                    }
                }
            }
        }
    }
}
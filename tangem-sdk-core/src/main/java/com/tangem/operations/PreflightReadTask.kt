package com.tangem.operations

import com.tangem.Log
import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.CardIdDisplayFormat
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.guard
import com.tangem.operations.preflightread.PreflightReadFilter
import com.tangem.operations.read.ReadCommand
import com.tangem.operations.read.ReadWalletsListCommand

/**
 * Mode for preflight read task
 * Note: Valid for cards with COS v. 4.0 and higher. Older card will always read the card and the wallet info.
 * `fullCardRead` will be used by default
 */
sealed class PreflightReadMode {

    /**
     * No card will be read at session start. `SessionEnvironment.card` will be empty
     */
    object None : PreflightReadMode()

    /**
     * Read only card info without wallet info. Valid for cards with COS v. 4.0 and higher.
     * Older card will always read card and wallet info
     */
    object ReadCardOnly : PreflightReadMode()

    /**
     * Read card info and all wallets. Used by default
     */
    object FullCardRead : PreflightReadMode()

    override fun toString(): String = this::class.java.simpleName
}

class PreflightReadTask(
    private val readMode: PreflightReadMode,
    private val filter: PreflightReadFilter? = null,
) : CardSessionRunnable<Card> {

    override fun run(session: CardSession, callback: CompletionCallback<Card>) {
        Log.command(this) { " [mode - $readMode]" }

        ReadCommand().run(session) readCommand@{ result ->
            when (result) {
                is CompletionResult.Success -> {
                    val card = result.data.card

                    try {
                        session.environment.config.filter.verifyCard(result.data.card)

                        if (session.environment.config.handleErrors) {
                            filter?.onCardRead(card = card, environment = session.environment)
                        }
                    } catch (error: TangemSdkError) {
                        callback(CompletionResult.Failure(error))
                        return@readCommand
                    }

                    updateEnvironmentIfNeeded(card = card, session = session)
                    session.updateUserCodeIfNeeded()
                    finalizeRead(session = session, card = card, callback = callback)
                }

                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }

    private fun finalizeRead(session: CardSession, card: Card, callback: CompletionCallback<Card>) {
        if (card.firmwareVersion < FirmwareVersion.MultiWalletAvailable) {
            val result = try {
                filterOnReadWalletsList(card, session)
                CompletionResult.Success(card)
            } catch (error: TangemSdkError) {
                CompletionResult.Failure(error)
            }

            callback(result)
            return
        }

        when (readMode) {
            PreflightReadMode.FullCardRead -> readWalletsList(session, callback)
            PreflightReadMode.ReadCardOnly, PreflightReadMode.None -> callback(CompletionResult.Success(card))
        }
    }

    private fun readWalletsList(session: CardSession, callback: CompletionCallback<Card>) {
        ReadWalletsListCommand().run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    val card = session.environment.card.guard {
                        callback(CompletionResult.Failure(TangemSdkError.MissingPreflightRead()))
                        return@run
                    }

                    val callbackResult = try {
                        filterOnReadWalletsList(card, session)
                        CompletionResult.Success(card)
                    } catch (error: TangemSdkError) {
                        CompletionResult.Failure(error)
                    }

                    callback(callbackResult)
                }

                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }

    private fun filterOnReadWalletsList(card: Card, session: CardSession) {
        if (!session.environment.config.handleErrors) return

        filter?.onFullCardRead(card = card, environment = session.environment)
    }

    private fun updateEnvironmentIfNeeded(card: Card, session: CardSession) {
        if (FirmwareVersion.visaRange.contains(card.firmwareVersion.doubleValue)) {
            session.environment.config.cardIdDisplayFormat = CardIdDisplayFormat.None
        }
    }
}

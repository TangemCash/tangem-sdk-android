package com.tangem.operations.wallet

import com.tangem.Log
import com.tangem.common.CompletionResult
import com.tangem.common.card.EllipticCurve
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.guard
import com.tangem.operations.read.ReadCommand
import com.tangem.operations.read.ReadWalletsListCommand

class CreateWalletTask(
    private val curve: EllipticCurve
) : CardSessionRunnable<CreateWalletResponse> {

    override fun run(session: CardSession, callback: CompletionCallback<CreateWalletResponse>) {
        val command = CreateWalletCommand(curve)
        command.run(session) { result ->
            when (result) {
                is CompletionResult.Success -> callback(CompletionResult.Success(result.data))
                is CompletionResult.Failure -> {
                    //Wallet already created but we didn't get the proper response from the card. Rescan and retrieve the wallet
                    if (result.error is TangemSdkError.InvalidState) {
                        Log.debug { "Received wallet creation error. Try rescan and retrieve created wallet" }
                        scanAndRetrieveCreatedWallet(command.walletIndex, session, callback)
                    } else {
                        callback(CompletionResult.Failure(result.error))
                    }
                }
            }
        }
    }

    private fun scanAndRetrieveCreatedWallet(
        index: Int,
        session: CardSession,
        callback: CompletionCallback<CreateWalletResponse>
    ) {
        val card = session.environment.card.guard {
            callback(CompletionResult.Failure(TangemSdkError.MissingPreflightRead()))
            return
        }

        if (card.firmwareVersion < FirmwareVersion.MultiWalletAvailable) {
            ReadCommand().run(session) { result ->
                when (result) {
                    is CompletionResult.Success -> mapWallet(index, session, callback)
                    is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
                }
            }

        } else {
            ReadWalletsListCommand().run(session) { result ->
                when (result) {
                    is CompletionResult.Success -> mapWallet(index, session, callback)
                    is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
                }
            }
        }
    }

    private fun mapWallet(index: Int, session: CardSession, callback: CompletionCallback<CreateWalletResponse>) {
        val card = session.environment.card.guard {
            callback(CompletionResult.Failure(TangemSdkError.MissingPreflightRead()))
            return
        }

        val createdWallet = card.wallets.firstOrNull { it.index == index }
        if (createdWallet == null) {
            Log.debug { "Wallet not found after rescan." }
            callback(CompletionResult.Failure(TangemSdkError.UnknownError()))
        } else {
            callback(CompletionResult.Success(CreateWalletResponse(card.cardId, createdWallet)))
        }
    }
}


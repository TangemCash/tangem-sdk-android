package com.tangem.demo

import com.tangem.Message
import com.tangem.common.CompletionResult
import com.tangem.common.SuccessResponse
import com.tangem.common.card.CardWallet
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.guard
import com.tangem.operations.wallet.PurgeWalletCommand
import java.util.ArrayDeque

class PurgeAllWalletsTask : CardSessionRunnable<SuccessResponse> {

    private val copyOfWallets = ArrayDeque<CardWallet>()

    override fun run(session: CardSession, callback: CompletionCallback<SuccessResponse>) {
        val card = session.environment.card.guard {
            callback(CompletionResult.Failure(TangemSdkError.CardError()))
            return
        }
        if (card.wallets.isEmpty()) {
            callback(CompletionResult.Failure(TangemSdkError.WalletNotFound()))
            return
        }

        copyOfWallets.addAll(card.wallets)
        purgeWallet(copyOfWallets.pollLast(), session, callback)
    }

    private fun purgeWallet(wallet: CardWallet?, session: CardSession, callback: CompletionCallback<SuccessResponse>) {
        if (wallet == null) {
            callback(CompletionResult.Success(SuccessResponse(session.environment.card?.cardId ?: "")))
            return
        }

        val walletIndex = session.environment.card?.wallet(wallet.publicKey)?.index ?: -1
        session.setMessage(Message(
            "Deleting the #$walletIndex wallet index",
            "Wait until all wallets are deleted"
        ))

        PurgeWalletCommand(wallet.publicKey).run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    session.environment.card = session.environment.card?.removeWallet(wallet.publicKey)
                    purgeWallet(copyOfWallets.pollLast(), session, callback)
                }
                is CompletionResult.Failure -> {
                    callback(CompletionResult.Failure(result.error))
                }
            }
        }
    }
}
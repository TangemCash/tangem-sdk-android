package com.tangem.operations

import com.tangem.common.CompletionResult
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.guard
import com.tangem.common.hdWallet.DerivationPath
import com.tangem.common.hdWallet.ExtendedPublicKey
import com.tangem.operations.read.ReadWalletCommand

/**
 * Created by Anton Zhilenkov on 06/08/2021.
 */
class DerivePublicKeyCommand(
    private val walletPublicKey: ByteArray,
    private val hdPath: DerivationPath,
) : CardSessionRunnable<ExtendedPublicKey> {

    override fun preflightReadMode(): PreflightReadMode = PreflightReadMode.ReadCardOnly

    override fun run(session: CardSession, callback: CompletionCallback<ExtendedPublicKey>) {
        val readWallet = ReadWalletCommand(walletPublicKey, hdPath)
        readWallet.run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    val chainCode = result.data.wallet.chainCode.guard {
                        callback(CompletionResult.Failure(TangemSdkError.CardError()))
                        return@run
                    }

                    val childKey = ExtendedPublicKey(result.data.wallet.publicKey, chainCode)
                    callback(CompletionResult.Success(childKey))
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }
}
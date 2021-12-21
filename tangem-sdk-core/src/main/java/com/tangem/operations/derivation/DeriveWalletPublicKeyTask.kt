package com.tangem.operations.derivation

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
 * Derive wallet  public key according to BIP32 (Private parent key → public child key)
 * Warning: Only `secp256k1` and `ed25519` (BIP32-Ed25519 scheme) curves supported
 * @property walletPublicKey seed public key.
 * @property derivationPath derivation path.
 */
class DeriveWalletPublicKeyTask(
    private val walletPublicKey: ByteArray,
    private val derivationPath: DerivationPath,
) : CardSessionRunnable<ExtendedPublicKey> {

    override fun run(session: CardSession, callback: CompletionCallback<ExtendedPublicKey>) {
        val walletIndex = session.environment.card?.wallet(walletPublicKey)?.index.guard {
            callback(CompletionResult.Failure(TangemSdkError.WalletNotFound()))
            return
        }

        val readWallet = ReadWalletCommand(walletIndex, derivationPath)
        readWallet.run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    val chainCode = result.data.wallet.chainCode.guard {
                        callback(CompletionResult.Failure(TangemSdkError.CardError()))
                        return@run
                    }

                    val childKey = ExtendedPublicKey(result.data.wallet.publicKey, chainCode, derivationPath)
                    callback(CompletionResult.Success(childKey))
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }
}
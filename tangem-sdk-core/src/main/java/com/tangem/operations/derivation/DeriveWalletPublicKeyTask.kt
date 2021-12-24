package com.tangem.operations.derivation

import com.tangem.common.CompletionResult
import com.tangem.common.card.EllipticCurve
import com.tangem.common.card.WalletIndex
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.get
import com.tangem.common.extensions.guard
import com.tangem.common.extensions.plusIfNotContains
import com.tangem.common.hdWallet.DerivationPath
import com.tangem.common.hdWallet.ExtendedPublicKey
import com.tangem.operations.read.ReadWalletCommand

/**
 * Derive wallet  public key according to BIP32 (Private parent key → public child key)
 * Warning: Only `secp256k1` and `ed25519` (BIP32-Ed25519 scheme) curves supported
 * @property walletIndex index of the wallet.
 * @property derivationPath derivation path.
 */
class DeriveWalletPublicKeyTask(
    private val walletIndex: WalletIndex,
    private val derivationPath: DerivationPath,
) : CardSessionRunnable<ExtendedPublicKey> {

    override fun run(session: CardSession, callback: CompletionCallback<ExtendedPublicKey>) {
        val wallet = session.environment.card?.wallet(walletIndex).guard {
             callback(CompletionResult.Failure(TangemSdkError.WalletNotFound()))
            return
        }

        if (wallet.curve != EllipticCurve.Secp256k1 && wallet.curve != EllipticCurve.Ed25519) {
            callback(CompletionResult.Failure(TangemSdkError.UnsupportedCurve()))
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

                    val childKey = ExtendedPublicKey(
                        compressedPublicKey = result.data.wallet.publicKey,
                        chainCode = chainCode,
                        derivationPath = derivationPath
                    )
                    val wallet = session.environment.card?.wallets?.get(walletIndex)
                    if (wallet != null) {
                        session.environment.card = session.environment.card?.updateWallet(
                            wallet.copy(derivedKeys = wallet.derivedKeys.plusIfNotContains(childKey))
                        )
                    }
                    callback(CompletionResult.Success(childKey))
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }
}
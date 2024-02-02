package com.tangem.operations.derivation

import com.tangem.common.CompletionResult
import com.tangem.common.card.EllipticCurve
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.get
import com.tangem.common.extensions.guard
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
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
        val wallet = session.environment.card?.wallet(walletPublicKey).guard {
            callback(CompletionResult.Failure(TangemSdkError.WalletNotFound()))
            return
        }
        if (!wallet.curve.supportsDerivation()) {
            callback(CompletionResult.Failure(TangemSdkError.UnsupportedCurve()))
            return
        }
        if (wallet.curve == EllipticCurve.Ed25519Slip0010 && derivationPath.nodes.any { !it.isHardened }) {
            callback(CompletionResult.Failure(TangemSdkError.NonHardenedDerivationNotSupported()))
        }

        val readWallet = ReadWalletCommand(wallet.index, derivationPath)
        readWallet.run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    val chainCode = result.data.wallet.chainCode.guard {
                        callback(CompletionResult.Failure(TangemSdkError.CardError()))
                        return@run
                    }

                    val childKey = ExtendedPublicKey(
                        publicKey = result.data.wallet.publicKey,
                        chainCode = chainCode,
                    )
                    updateKeys(childKey, session)
                    callback(CompletionResult.Success(childKey))
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }

    private fun updateKeys(childKey: ExtendedPublicKey, session: CardSession) {
        val wallet = session.environment.card?.wallets?.get(walletPublicKey)
        val updatedKeys = wallet?.derivedKeys?.toMutableMap()
            ?.apply { this[derivationPath] = childKey }
        if (wallet != null && updatedKeys != null) {
            session.environment.card = session.environment.card?.updateWallet(
                wallet.copy(derivedKeys = updatedKeys),
            )
        }
    }
}

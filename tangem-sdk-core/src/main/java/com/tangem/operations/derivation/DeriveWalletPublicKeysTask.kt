package com.tangem.operations.derivation

import com.tangem.Log
import com.tangem.common.CompletionResult
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.operations.CommandResponse

class ExtendedPublicKeysMap(
    map: Map<DerivationPath, ExtendedPublicKey>,
) : HashMap<DerivationPath, ExtendedPublicKey>(map), CommandResponse

/**
 * Derive wallet public keys according to BIP32 (Private parent key → public child key)
 * Warning: Only `secp256k1` and `ed25519` (BIP32-Ed25519 scheme) curves supported
 * @property walletPublicKey seed public key.
 * @property derivationPaths multiple derivation paths. Repeated items will be ignored.
 */
class DeriveWalletPublicKeysTask(
    private val walletPublicKey: ByteArray,
    derivationPaths: List<DerivationPath>,
) : CardSessionRunnable<ExtendedPublicKeysMap> {

    private val derivationPaths = derivationPaths.distinct()

    override fun run(session: CardSession, callback: CompletionCallback<ExtendedPublicKeysMap>) {
        runDerivation(0, emptyMap(), session, callback)
    }

    private fun runDerivation(
        index: Int,
        keys: Map<DerivationPath, ExtendedPublicKey>,
        session: CardSession,
        callback: CompletionCallback<ExtendedPublicKeysMap>,
    ) {
        if (index >= derivationPaths.size) {
            callback(CompletionResult.Success(ExtendedPublicKeysMap(keys)))
            return
        }
        val path = derivationPaths[index]
        val task = DeriveWalletPublicKeyTask(walletPublicKey, path)
        task.run(session) { result ->
            val updatedKeys = keys.toMutableMap()
            when (result) {
                is CompletionResult.Success -> {
                    updatedKeys[path] = result.data
                }
                is CompletionResult.Failure -> {
                    when (result.error) {
                        is TangemSdkError.NonHardenedDerivationNotSupported,
                        is TangemSdkError.WalletNotFound,
                        is TangemSdkError.UnsupportedCurve,
                        -> {
                            // continue derivation
                            Log.error { "Error: ${result.error}" }
                        }
                        else -> {
                            if (keys.keys.isEmpty()) {
                                callback(CompletionResult.Failure(result.error))
                            } else {
                                Log.error { "Error: ${result.error}" }
                                // return partial response
                                callback(CompletionResult.Success(ExtendedPublicKeysMap(keys)))
                            }
                        }
                    }
                }
            }
            runDerivation(index + 1, updatedKeys, session, callback)
        }
    }
}

package com.tangem.operations.derivation

import com.tangem.common.CompletionResult
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.hdWallet.DerivationPath
import com.tangem.common.hdWallet.ExtendedPublicKey
import com.tangem.operations.CommandResponse

class ExtendedPublicKeyList(
    items: Collection<ExtendedPublicKey>
): ArrayList<ExtendedPublicKey>(items), CommandResponse

/**
 * Derive wallet public keys according to BIP32 (Private parent key → public child key)
 * @property walletPublicKey seed public key.
 * @property derivationPaths multiple derivation paths.
 */
class DeriveWalletPublicKeysTask(
    private val walletPublicKey: ByteArray,
    private val derivationPaths: List<DerivationPath>,
) : CardSessionRunnable<ExtendedPublicKeyList> {

    override fun run(session: CardSession, callback: CompletionCallback<ExtendedPublicKeyList>) {
        runDerivation(0, listOf(), session, callback)
    }

    private fun runDerivation(
        index: Int,
        keys: List<ExtendedPublicKey>,
        session: CardSession,
        callback: CompletionCallback<ExtendedPublicKeyList>
    ) {
        if (index >= derivationPaths.size) {
            callback(CompletionResult.Success(ExtendedPublicKeyList(keys)))
            return
        }

        val task = DeriveWalletPublicKeyTask(walletPublicKey, derivationPaths[index])
        task.run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    runDerivation(index + 1, keys + listOf(result.data), session, callback)
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }
}

package com.tangem.operations.derivation

import com.tangem.common.CompletionResult
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.hdWallet.DerivationPath
import com.tangem.operations.CommandResponse

class DerivationTaskResponse(
    val entries: Map<ByteArrayKey, ExtendedPublicKeysMap>,
) : CommandResponse

class DeriveMultipleWalletPublicKeysTask(
    private val derivations: Map<ByteArrayKey, List<DerivationPath>>,
) : CardSessionRunnable<DerivationTaskResponse> {

    val response: MutableMap<ByteArrayKey, ExtendedPublicKeysMap> = mutableMapOf()

    override fun run(session: CardSession, callback: CompletionCallback<DerivationTaskResponse>) {
        derive(keys = derivations.keys.toList(), index = 0, session = session, callback = callback)
    }

    private fun derive(
        keys: List<ByteArrayKey>,
        index: Int,
        session: CardSession,
        callback: CompletionCallback<DerivationTaskResponse>,
    ) {
        if (index == keys.count()) {
            callback(CompletionResult.Success(DerivationTaskResponse(response.toMap())))
            return
        }

        val key = keys[index]
        val paths = derivations[key]!!
        DeriveWalletPublicKeysTask(key.bytes, paths).run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    response[key] = result.data
                    derive(keys = keys, index = index + 1, session = session, callback = callback)
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }
}
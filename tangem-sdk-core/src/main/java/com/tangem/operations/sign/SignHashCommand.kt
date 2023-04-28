package com.tangem.operations.sign

import com.squareup.moshi.JsonClass
import com.tangem.common.CompletionResult
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.operations.CommandResponse

/**
 * Response for [SignHashCommand].
 */
@JsonClass(generateAdapter = true)
data class SignHashResponse(
    /**
     * CID, Unique Tangem card ID number
     */
    val cardId: String,

    /**
     * Signed hash
     */
    val signature: ByteArray,

    /**
     * Total number of signed  hashes returned by the wallet since its creation. COS: 1.16+
     */
    val totalSignedHashes: Int?,
) : CommandResponse

/**
 * Signs transaction hash using a wallet private key, stored on the card.
 * @property hash: Transaction hash for sign by card.
 * @property walletPublicKey: Public key of the wallet, using for sign.
 * @property derivationPath: Derivation path of the wallet. Optional. COS v. 4.28 and higher,
 */
class SignHashCommand(
    private val hash: ByteArray,
    private val walletPublicKey: ByteArray,
    private val derivationPath: DerivationPath? = null,
) : CardSessionRunnable<SignHashResponse> {

    override fun run(session: CardSession, callback: CompletionCallback<SignHashResponse>) {
        val signCommand = SignCommand(arrayOf(hash), walletPublicKey, derivationPath)
        signCommand.run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    val response = SignHashResponse(
                        result.data.cardId,
                        result.data.signatures[0],
                        result.data.totalSignedHashes,
                    )
                    callback(CompletionResult.Success(response))
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }
}

package com.tangem.operations.sign

import com.squareup.moshi.JsonClass
import com.tangem.common.CompletionResult
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.operations.CommandResponse
import com.tangem.operations.PreflightReadMode
import com.tangem.operations.read.WalletPointer

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
//    val walletHdPath: ByteArray?,
//    val walletHdChain: ByteArray?,
) : CommandResponse

/**
 * Signs transaction hash using a wallet private key, stored on the card.
 * @property hash: Transaction hash for sign by card.
 * @property walletPublicKey: Public key of the wallet, using for sign.
 */
class SignHashCommand(
    private val hash: ByteArray,
    private val walletPointer: WalletPointer,
) : CardSessionRunnable<SignHashResponse> {

    override fun preflightReadMode(): PreflightReadMode = PreflightReadMode.FullCardRead

    override fun run(session: CardSession, callback: CompletionCallback<SignHashResponse>) {

        val wallet = session.environment.card?.wallets?.firstOrNull()
        if (wallet == null) {
            callback(CompletionResult.Failure(TangemSdkError.WalletNotFound()))
            return
        }

        val signCommand = SignCommand(arrayOf(hash), walletPointer)
        signCommand.run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    val response = SignHashResponse(
                        result.data.cardId,
                        result.data.signatures[0],
                        result.data.totalSignedHashes,
//                        result.data.walletHdPath,
//                        result.data.walletHdChain
                    )
                    callback(CompletionResult.Success(response))
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }
}
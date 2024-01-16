package com.tangem.operations.backup

import com.tangem.common.CompletionResult
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.guard
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.services.Result
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvTag
import com.tangem.crypto.sign
import com.tangem.operations.attestation.OnlineCardVerifier

class BackupCertificateProvider {

    private val onlineCardVerifier: OnlineCardVerifier = OnlineCardVerifier()

    suspend fun getCertificate(
        cardId: String,
        cardPublicKey: ByteArray,
        developmentMode: Boolean,
        callback: CompletionCallback<ByteArray>,
    ) {
        if (developmentMode) {
            val issuerPrivateKey =
                "11121314151617184771ED81F2BACF57479E4735EB1405083927372D40DA9E92".hexToBytes()
            val issuerSignature = cardPublicKey.sign(issuerPrivateKey)
            callback(CompletionResult.Success(generateCertificate(cardPublicKey, issuerSignature)))
            return
        }

        when (
            val result =
                onlineCardVerifier.getCardData(cardId, cardPublicKey)
        ) {
            is Result.Success -> {
                val signature = result.data.issuerSignature.guard {
                    callback(CompletionResult.Failure(TangemSdkError.IssuerSignatureLoadingFailed()))
                    return
                }
                callback(
                    CompletionResult.Success(
                        generateCertificate(cardPublicKey, signature.hexToBytes()),
                    ),
                )
            }

            is Result.Failure ->
                callback(CompletionResult.Failure(TangemSdkError.IssuerSignatureLoadingFailed()))
        }
    }

    private fun generateCertificate(cardPublicKey: ByteArray, issuerSignature: ByteArray): ByteArray {
        return TlvBuilder().run {
            append(TlvTag.CardPublicKey, cardPublicKey)
            append(TlvTag.IssuerDataSignature, issuerSignature)
            serialize()
        }
    }
}

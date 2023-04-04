package com.tangem.crypto

import com.squareup.moshi.JsonClass
import com.tangem.common.tlv.TlvEncoder
import com.tangem.common.tlv.TlvTag
import java.io.ByteArrayOutputStream

interface IssuerDataVerifier {
    fun verify(issuerPublicKey: ByteArray, signature: ByteArray, issuerDataToVerify: IssuerDataToVerify): Boolean
}

@JsonClass(generateAdapter = true)
class IssuerDataToVerify(
    val cardId: String,
    val issuerData: ByteArray?,
    val issuerDataCounter: Int? = null,
    val issuerExtraDataSize: Int? = null,
)

class DefaultIssuerDataVerifier : IssuerDataVerifier {
    override fun verify(
        issuerPublicKey: ByteArray,
        signature: ByteArray,
        issuerDataToVerify: IssuerDataToVerify,
    ): Boolean {
        val tlvEncoder = TlvEncoder()
        val dataToVerify = ByteArrayOutputStream()
        dataToVerify.write(tlvEncoder.encodeValue(TlvTag.CardId, issuerDataToVerify.cardId))
        issuerDataToVerify.issuerData?.let { dataToVerify.write(it) }
        issuerDataToVerify.issuerDataCounter?.let { counter ->
            dataToVerify.write(tlvEncoder.encodeValue(TlvTag.IssuerDataCounter, counter))
        }
        issuerDataToVerify.issuerExtraDataSize?.let {
            dataToVerify.write(tlvEncoder.encodeValue(TlvTag.Size, it))
        }
        return CryptoUtils.verify(issuerPublicKey, dataToVerify.toByteArray(), signature)
    }
}

package com.tangem.operations.personalization.entities

import java.util.*

/**
 * Detailed information about card contents.
 */
internal class CardData(
    /**
     * Tangem internal manufacturing batch ID.
     */
    val batchId: String,
    /**
     * Timestamp of manufacturing.
     */
    val manufactureDateTime: Date,
    /**
     * Name of the issuer.
     */
    val issuerName: String?,
    /**
     * Name of the blockchain.
     */
    val blockchainName: String,
    /**
     * Signature of CardId with manufacturer’s private key. COS 1.21+
     */
    val manufacturerSignature: ByteArray?,
    /**
     * Mask of products enabled on card. COS 2.30+
     */
    val productMask: ProductMask?,
    /**
     * Name of the token.
     */
    val tokenSymbol: String?,
    /**
     * Smart contract address.
     */
    val tokenContractAddress: String?,
    /**
     * Number of decimals in token value.
     */
    val tokenDecimal: Int?,
)
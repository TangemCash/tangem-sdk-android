package com.tangem.operations.personalization.entities

import java.util.Date

/**
 * Detailed information about card contents
 *
 * @property batchId              Tangem internal manufacturing batch ID
 * @property manufactureDateTime  timestamp of manufacturing
 * @property blockchainName       name of the blockchain
 * @property productMask          mask of products enabled on card. COS 2.30+
 * @property tokenSymbol          name of the token
 * @property tokenContractAddress smart contract address
 * @property tokenDecimal         number of decimals in token value
 */
@Suppress("LongParameterList")
internal class CardData(
    val batchId: String,
    val manufactureDateTime: Date,
    val blockchainName: String,
    val productMask: ProductMask?,
    val tokenSymbol: String?,
    val tokenContractAddress: String?,
    val tokenDecimal: Int?,
)
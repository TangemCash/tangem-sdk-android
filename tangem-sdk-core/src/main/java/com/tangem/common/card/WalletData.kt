package com.tangem.common.card

/**
 * Created by Anton Zhilenkov on 20/07/2021.
 */
data class WalletData(
    /**
     * Name of the blockchain.
     */
    val blockchain: String,
    /**
     * Token of the specified blockchain.
     */
    val token: Token?,
) {

    data class Token(
        /**
         * Display name of the token.
         */
        val name: String,
        /**
         * Token symbol
         */
        val symbol: String,
        /**
         * Smart contract address.
         */
        val contractAddress: String,
        /**
         * Number of decimals in token value.
         */
        val decimals: Int,
    )
}
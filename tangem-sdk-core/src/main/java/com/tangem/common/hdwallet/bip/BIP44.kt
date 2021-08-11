package com.tangem.common.hdWallet.bip

import com.tangem.common.hdwallet.DerivationNode
import com.tangem.common.hdwallet.DerivationPath

/**
 * Created by Anton Zhilenkov on 06/08/2021.
 */
class BIP44(
    val coinType: Long,
    val account: Long,
    val change: Chain,
    val addressIndex: Long,
) {

    /**
     * Build path
     * @return Path according BIP32
     */
    fun buildPath(): DerivationPath {
        val nodes = listOf(
                DerivationNode.Hardened(purpose),
                DerivationNode.Hardened(coinType),
                DerivationNode.Hardened(account),
                DerivationNode.NotHardened(change.index),
                DerivationNode.NotHardened(addressIndex),
        )
        return DerivationPath(nodes)
    }

    companion object {
        const val purpose: Long = 44

        /**
         * Build path m/44/coinType
         * @param coinType: UInt32 index of the coin
         * @return DerivationPath m/44/coinType
         */
        fun buildPath(coinType: Long): DerivationPath {
            val nodes = listOf(DerivationNode.NotHardened(purpose), DerivationNode.NotHardened(coinType))
            return DerivationPath(nodes)
        }
    }

    enum class Chain(val index: Long) {
        External(0),
        Internal(1);
    }
}
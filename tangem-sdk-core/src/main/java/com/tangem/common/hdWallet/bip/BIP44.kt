package com.tangem.common.hdWallet.bip

import com.tangem.common.hdWallet.DerivationNode
import com.tangem.common.hdWallet.DerivationPath

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
     * @param notHardenedOnly: Because we don't have access to the private key, we can use non-hardened
     * derivation only without tapping the Tangem card.
     * @return Path according BIP32
     */
    fun buildPath(notHardenedOnly: Boolean = true): DerivationPath {
        val nodes = listOf(
                DerivationNode.create(purpose, notHardenedOnly),
                DerivationNode.create(coinType, notHardenedOnly),
                DerivationNode.create(account, notHardenedOnly),
                DerivationNode.NotHardened(change.ordinal.toLong()),
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

    enum class Chain {
        External, Internal
    }
}
package com.tangem.common.hdWallet

import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.calculateHashCode
import com.tangem.common.extensions.remove
import com.tangem.common.hdWallet.bip.BIP32

/**
 * BIP32 derivation Path
 */
class DerivationPath {
    val rawPath: String
    val nodes: List<DerivationNode>

    /**
     * Init with master node
     */
    constructor() : this(listOf())

    /**
     * Parse derivation path.
     * @param rawPath: Path. E.g. "m/0'/0/1/0"
     */
    @Throws(HDWalletError::class)
    constructor(rawPath: String) : this(rawPath, createPath(rawPath))

    constructor(path: List<DerivationNode>) : this(createRawPath(path), path)

    constructor(rawPath: String, path: List<DerivationNode>) {
        this.rawPath = rawPath
        this.nodes = path
    }

    fun extendedPath(node: DerivationNode): DerivationPath = DerivationPath(nodes + listOf(node))

    override fun equals(other: Any?): Boolean {
        val other = other as? DerivationPath ?: return false

        this.nodes.forEachIndexed { index, node ->
            if (node != other.nodes[index]) return false
        }
        return true
    }

    override fun hashCode(): Int = calculateHashCode(
        rawPath.hashCode(),
        calculateHashCode(*nodes.map { it.hashCode() }.toIntArray())
    )

    companion object {

        @Throws(TangemSdkError::class)
        fun from(data: ByteArray): DerivationPath {
            if (data.size % 4 != 0) {
                val message = "Failed to parse DerivationPath. Data too short."
                throw TangemSdkError.DecodingFailed(message)
            }
            val chunks = 0 until data.size / 4
            val dataChunks = chunks.map { data.drop(it * 4).take(4).toByteArray() }
            val path = dataChunks.map { DerivationNode.deserialize(it) }
            return DerivationPath(path)
        }

        @Throws(HDWalletError::class)
        private fun createPath(rawPath: String): List<DerivationNode> {
            val splittedPath = rawPath.toLowerCase().split(BIP32.separatorSymbol)
            if (splittedPath.size < 2) throw HDWalletError.WrongPath
            if (splittedPath[0].trim() != BIP32.masterKeySymbol) throw HDWalletError.WrongPath

            val derivationPath = splittedPath.subList(1, splittedPath.size).map { pathItem ->
                val isHardened = pathItem.contains(BIP32.hardenedSymbol)
                        || pathItem.contains(BIP32.alternativeHardenedSymbol)
                val cleanedPathItem = pathItem.trim()
                        .remove(BIP32.hardenedSymbol, BIP32.alternativeHardenedSymbol)
                val index = cleanedPathItem.toLongOrNull() ?: throw HDWalletError.WrongPath

                if (isHardened) DerivationNode.Hardened(index) else DerivationNode.NonHardened(index)
            }
            return derivationPath
        }

        private fun createRawPath(nodes: List<DerivationNode>): String {
            var path = BIP32.masterKeySymbol

            val nodesPath = nodes.joinToString(BIP32.separatorSymbol) { it.pathDescription }
            if (nodesPath.isNotEmpty()) path += "${BIP32.separatorSymbol}$nodesPath"

            return path
        }
    }
}
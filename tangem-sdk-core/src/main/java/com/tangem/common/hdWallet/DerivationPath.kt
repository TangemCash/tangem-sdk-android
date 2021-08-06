package com.tangem.common.hdWallet

import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.remove
import com.tangem.common.hdWallet.bip.BIP32

class DerivationPath constructor(
    val rawPath: String,
    val path: List<DerivationNode>
) {

    constructor(rawPath: String) : this(rawPath, createPath(rawPath))

    constructor(path: List<DerivationNode>) : this(createRawPath(path), path)

    override fun equals(other: Any?): Boolean {
        val other = other as? DerivationPath ?: return false

        return this.path.isEquals(other.path)
    }

    companion object {
        @Throws(TangemSdkError::class)
        fun from(data: ByteArray): DerivationPath {
            if (data.size % 4 != 0) {
                val message = "Failed to parse DerivationPath. Data too short."
                throw TangemSdkError.DecodingFailed(message)
            }
            val chunks = 0 until data.size / 4
            val dataChunks = chunks.map {
                val howMuchDrop = it * 4
                val dropped = data.drop(howMuchDrop)
                val taked = dropped.take(4)
                taked.toByteArray()
            }
            val path = dataChunks.map { DerivationNode.deserialize(it) }
            return DerivationPath(path)
        }

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

                if (isHardened) DerivationNode.Hardened(index) else DerivationNode.NotHardened(index)
            }
            return derivationPath
        }

        private fun createRawPath(path: List<DerivationNode>): String {
            val description = path.joinToString(BIP32.separatorSymbol) { it.pathDescription }
            return "${BIP32.masterKeySymbol}${BIP32.separatorSymbol}$description"
        }
    }
}

private fun List<DerivationNode>.isEquals(path: List<DerivationNode>): Boolean {
    this.forEachIndexed { index, node ->
        if (node != path[index]) return false
    }
    return true
}
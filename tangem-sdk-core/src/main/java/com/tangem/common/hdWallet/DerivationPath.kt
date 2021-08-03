package com.tangem.common.hdWallet

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
        val hardenedSymbol: String = "'"
        val masterKeySymbol: String = "m"
        val separatorSymbol: String = "/"

        private fun createPath(rawPath: String): List<DerivationNode> {
            val splittedPath = rawPath.toLowerCase().split(separatorSymbol)
            if (splittedPath.size < 2) throw HDWalletError.WrongPath
            if (splittedPath[0].trim() != masterKeySymbol) throw HDWalletError.WrongPath

            val derivationPath = splittedPath.subList(1, splittedPath.size).map { pathItem ->
                val isHardened = pathItem.contains(hardenedSymbol)
                val cleanedPathItem = pathItem.trim().replace(hardenedSymbol, "")
                val index = cleanedPathItem.toIntOrNull() ?: throw HDWalletError.WrongPath

                if (isHardened) DerivationNode.Hardened(index) else DerivationNode.NotHardened(index)
            }
            return derivationPath
        }

        private fun createRawPath(path: List<DerivationNode>): String {
            val description = path.joinToString(separatorSymbol) { it.pathDescription }
            return "$masterKeySymbol$separatorSymbol$description"
        }
    }
}

private fun List<DerivationNode>.isEquals(path: List<DerivationNode>): Boolean {
    this.forEachIndexed { index, node ->
        if (node != path[index]) return false
    }
    return true
}


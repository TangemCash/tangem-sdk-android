package com.tangem.common.hdWallet

import com.tangem.common.extensions.toByteArray
import com.tangem.common.extensions.toInt

sealed class DerivationNode(val index: Int, val pathDescription: String) {
    class Hardened(index: Int) : DerivationNode(index.withOffset(), "$index${DerivationPath.hardenedSymbol}")
    class NotHardened(index: Int) : DerivationNode(index, "$index")

    override fun equals(other: Any?): Boolean {
        val other = other as? DerivationNode ?: return false

        when (this) {
            is Hardened -> if (other !is Hardened) return false
            is NotHardened -> if (other !is NotHardened) return false
        }
        return this.index == other.index && this.pathDescription == other.pathDescription
    }

    companion object {
        val hardenedOffset: Long = 0x80000000

        fun DerivationNode.serialize(): ByteArray {
            return index.toByteArray(4)
        }

        fun deserialize(data: ByteArray): DerivationNode {
            val index = data.toInt()
            return if (index >= hardenedOffset) {
                Hardened(index.withOutOffset())
            } else {
                NotHardened(index)
            }
        }
    }
}

private fun Int.withOffset(): Int {
    return (this + DerivationNode.hardenedOffset).toInt()
}

private fun Int.withOutOffset(): Int {
    return (this - DerivationNode.hardenedOffset).toInt()
}


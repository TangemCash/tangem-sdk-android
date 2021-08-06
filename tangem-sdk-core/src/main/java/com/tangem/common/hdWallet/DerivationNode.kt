package com.tangem.common.hdWallet

import com.tangem.common.extensions.calculateHashCode
import com.tangem.common.extensions.toByteArray
import com.tangem.common.extensions.toLong
import com.tangem.common.hdWallet.bip.BIP32

sealed class DerivationNode(
    private val internalIndex: Long,
    private val isHardened: Boolean = true
) {
    val index: Long
        get() = if (isHardened) internalIndex + BIP32.hardenedOffset
        else internalIndex

    val pathDescription: String
        get() = if (isHardened) "$internalIndex${BIP32.hardenedSymbol}"
        else "$internalIndex"

    class Hardened(index: Long) : DerivationNode(index, true)
    class NotHardened(index: Long) : DerivationNode(index, false)

    fun toNonHardened(): DerivationNode {
        return when (this) {
            is Hardened -> NotHardened(this.index - BIP32.hardenedOffset)
            else -> this
        }
    }

    override fun equals(other: Any?): Boolean {
        val other = other as? DerivationNode ?: return false

        return this.internalIndex == other.internalIndex && this.isHardened == other.isHardened
    }

    override fun hashCode(): Int = calculateHashCode(
            isHardened.hashCode(),
            internalIndex.hashCode()
    )

    companion object {
        fun DerivationNode.serialize(): ByteArray {
            return index.toByteArray(4)
        }

        fun deserialize(data: ByteArray): DerivationNode {
            val index = data.toLong()
            return if (index >= BIP32.hardenedOffset) {
                Hardened(index - BIP32.hardenedOffset)
            } else {
                NotHardened(index)
            }
        }
    }
}
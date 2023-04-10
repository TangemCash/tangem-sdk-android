package com.tangem.crypto.hdWallet

import com.tangem.common.extensions.calculateHashCode
import com.tangem.common.extensions.toByteArray
import com.tangem.common.extensions.toLong
import com.tangem.crypto.hdWallet.bip32.BIP32

sealed class DerivationNode(
    private val internalIndex: Long,
    val isHardened: Boolean = true,
) {
    val index: Long
        get() = if (isHardened) internalIndex + BIP32.Constants.hardenedOffset else internalIndex

    val pathDescription: String
        get() = if (isHardened) "$internalIndex${BIP32.Constants.hardenedSymbol}" else "$internalIndex"

    class Hardened(index: Long) : DerivationNode(index, true)
    class NonHardened(index: Long) : DerivationNode(index, false)

    override fun equals(other: Any?): Boolean {
        val other = other as? DerivationNode ?: return false

        return this.internalIndex == other.internalIndex && this.isHardened == other.isHardened
    }

    override fun hashCode(): Int = calculateHashCode(
        isHardened.hashCode(),
        internalIndex.hashCode(),
    )

    companion object {
        fun fromIndex(index: Long): DerivationNode = if (index < BIP32.Constants.hardenedOffset) {
            NonHardened(index)
        } else {
            Hardened(index - BIP32.Constants.hardenedOffset)
        }

        fun DerivationNode.serialize(): ByteArray {
            return index.toByteArray(size = 4)
        }

        fun deserialize(data: ByteArray): DerivationNode {
            val index = data.toLong()
            return if (index >= BIP32.Constants.hardenedOffset) {
                Hardened(index - BIP32.Constants.hardenedOffset)
            } else {
                NonHardened(index)
            }
        }
    }
}

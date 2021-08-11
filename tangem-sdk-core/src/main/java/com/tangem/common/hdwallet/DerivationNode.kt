package com.tangem.common.hdwallet

import com.tangem.common.hdWallet.bip.BIP32
import java.nio.ByteBuffer

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

fun calculateHashCode(vararg hashCodes: Int, factor: Int = 31): Int {
    return if (hashCodes.size == 1) {
        factor * hashCodes[0]
    } else {
        hashCodes.drop(1).fold(hashCodes[0], { acc, hash -> acc * factor + hash })
    }
}

fun Long.toByteArray(size: Int = Long.SIZE_BYTES): ByteArray {
    return when (size) {
        Long.SIZE_BYTES -> {
            val buffer = ByteBuffer.allocate(Long.SIZE_BYTES)
            buffer.putLong(this)
            return buffer.array()
        }
        4 -> {
            val bytes = ByteArray(4)
            bytes[3] = (this and 0xFF).toByte()
            bytes[2] = ((this ushr 8) and 0xFF).toByte()
            bytes[1] = ((this ushr 16) and 0xFF).toByte()
            bytes[0] = ((this ushr 24) and 0xFF).toByte()
            return bytes
        }
        1 -> byteArrayOf(this.toByte())
        else -> byteArrayOf()
    }
}

fun ByteArray.toLong(): Long {
    return when (size) {
        4 -> {
            val mediator = ByteArray(Long.SIZE_BYTES)
            System.arraycopy(this, 0, mediator, 4, this.size)
            mediator.toLong()
        }
        else -> {
            val buffer = ByteBuffer.allocate(Long.SIZE_BYTES)
            buffer.put(this)
            buffer.flip()
            buffer.long
        }
    }
}
package com.tangem.common.extensions

import java.nio.ByteBuffer

/**
 * Created by Anton Zhilenkov on 04/08/2021.
 */
@Suppress("MagicNumber")
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
            bytes[2] = (this ushr 8 and 0xFF).toByte()
            bytes[1] = (this ushr 16 and 0xFF).toByte()
            bytes[0] = (this ushr 24 and 0xFF).toByte()
            return bytes
        }
        1 -> byteArrayOf(this.toByte())
        else -> byteArrayOf()
    }
}
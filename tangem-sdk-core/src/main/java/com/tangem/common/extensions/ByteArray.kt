package com.tangem.common.extensions

import com.tangem.crypto.CryptoUtils
import org.spongycastle.crypto.digests.RIPEMD160Digest
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.*
import kotlin.experimental.and
import kotlin.experimental.xor

/**
 * Extension functions for [ByteArray].
 */
fun ByteArray.toHexString(): String = joinToString("") { "%02X".format(it) }

fun ByteArray.toUtf8(): String = String(this).removeSuffix("\u0000")

@Suppress("MagicNumber")
fun ByteArray.toInt(): Int {
    return when (this.size) {
        1 -> (this[0] and 0xFF.toByte()).toInt()
        2 -> ByteBuffer.wrap(this).short.toInt()
        4 -> ByteBuffer.wrap(this).int
        else -> throw IllegalArgumentException("Length must be 1,2 or 4. Length = " + this.size)
    }
}

@Suppress("MagicNumber")
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

@Suppress("MagicNumber")
fun ByteArray.toDate(): Date {
    val year = copyOfRange(0, 2).toInt()
    val month = if (this.size > 2) this[2] - 1 else 0
    val day = if (this.size > 3) this[3].toInt() else 0
    val cd = Calendar.getInstance()
    cd.set(year, month, day, 0, 0, 0)
    return cd.time
}

fun ByteArray.calculateSha512(): ByteArray = MessageDigest.getInstance("SHA-512").digest(this)

fun ByteArray.calculateSha256(): ByteArray = MessageDigest.getInstance("SHA-256").digest(this)

@OptIn(ExperimentalUnsignedTypes::class)
fun UByteArray.calculateSha256(): UByteArray =
    MessageDigest.getInstance("SHA-256").digest(this.toByteArray()).toUByteArray()

fun ByteArray.calculateRipemd160(): ByteArray {
    val digest = RIPEMD160Digest()
    digest.update(this, 0, this.size)
    val out = ByteArray(size = 20)
    digest.doFinal(out, 0)
    return out
}

fun ByteArray.toCompressedPublicKey(): ByteArray = CryptoUtils.compressPublicKey(this)

fun ByteArray.toDecompressedPublicKey(): ByteArray = CryptoUtils.decompressPublicKey(this)

@Suppress("MagicNumber")
fun ByteArray.calculateCrc16(): ByteArray {
    var chBlock: Byte
    // STEP 1	Initialize the CRC-16 value
    var wCRC = 0x6363 // ITU-V.41
    var i = 0
    // STEP 2	Update data and Calucuate their CRC
    do {
        chBlock = this.get(i++)
        chBlock = chBlock xor (wCRC and 0x00FF).toByte()
        val chBlockInt = chBlock.toInt() xor (chBlock.toInt() shl 4)
        wCRC = wCRC
            .shr(bitCount = 8)
            .xor(
                other = chBlockInt.and(other = 0xFF).shl(bitCount = 8),
            )
            .and(other = 0xFFFF)
            .xor(
                other = chBlockInt.and(other = 0xFF).shl(bitCount = 3).and(other = 0xFFFF),
            )
            .xor(
                other = chBlockInt.and(other = 0xFF).shr(bitCount = 4).and(other = 0xFFFF),
            )
        // (wCRC>>8)^((int)chBlock<<8)^((int) chBlock<<3)^((int)chBlock>>4);
    } while (i < this.size)

    return byteArrayOf((wCRC and 0xFF).toByte(), (wCRC and 0xFFFF shr 8).toByte())
}

data class ByteArrayKey(val bytes: ByteArray) {
    override fun equals(other: Any?): Boolean {
        return this === other || other is ByteArrayKey && this.bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int = bytes.contentHashCode()
}

fun ByteArray.toMapKey(): ByteArrayKey = ByteArrayKey(this)

/**
 * Transforms [ByteArray] to string representation of bits ["1", "0", ...]
 */
fun ByteArray.toBits(): List<String> {
    return this.flatMap {
        it.toBits()
    }
}

/**
 * Transforms [UByteArray] to string representation of bits ["1", "0", ...]
 */
@OptIn(ExperimentalUnsignedTypes::class)
fun UByteArray.toBits(): List<String> {
    return this.flatMap {
        it.toBits()
    }
}

/**
 * Transforms UByte to string representation of bits ["1", "0", ...]
 */
@Suppress("MagicNumber")
fun UByte.toBits(): List<String> {
    val totalBitsCount = 8
    val value = this.toInt()
    val bits = arrayOfNulls<String>(totalBitsCount)
    for (index in 0 until totalBitsCount) {
        bits[totalBitsCount - 1 - index] = if (value and (1 shl index) != 0) "1" else "0"
    }

    return bits.map { it ?: "0" }
}

/**
 * Transforms [Byte] to string representation of bits ["1", "0", ...]
 */
@Suppress("MagicNumber")
fun Byte.toBits(): List<String> {
    val totalBitsCount = 8
    val value = this.toInt()
    val bits = arrayOfNulls<String>(totalBitsCount)
    for (index in 0 until totalBitsCount) {
        bits[totalBitsCount - 1 - index] = if (value and (1 shl index) != 0) "1" else "0"
    }

    return bits.map { it ?: "0" }
}

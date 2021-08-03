package com.tangem.common.extensions

import org.spongycastle.crypto.digests.RIPEMD160Digest
import org.spongycastle.jce.ECNamedCurveTable
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

fun ByteArray.toInt(): Int {
    return when (this.size) {
        1 -> (this[0] and 0xFF.toByte()).toInt()
        2 -> ByteBuffer.wrap(this).short.toInt()
        4 -> ByteBuffer.wrap(this).int
        else -> throw IllegalArgumentException("Length must be 1,2 or 4. Length = " + this.size)
    }
}

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

fun ByteArray.calculateRipemd160(): ByteArray {
    val digest = RIPEMD160Digest()
    digest.update(this, 0, this.size)
    val out = ByteArray(20)
    digest.doFinal(out, 0)
    return out
}

fun ByteArray.toCompressedPublicKey(): ByteArray {
    return if (this.size == 65) {
        val spec = ECNamedCurveTable.getParameterSpec("secp256k1")
        val publicKeyPoint = spec.curve.decodePoint(this)
        publicKeyPoint.getEncoded(true)
    } else {
        this
    }
}

fun ByteArray.calculateCrc16(): ByteArray {
    var chBlock: Byte
    // STEP 1	Initialize the CRC-16 value
    var wCRC = 0x6363 // ITU-V.41
    var i = 0
    // STEP 2	Update data and Calucuate their CRC
    do {
        chBlock = this.get(i++)
        chBlock = chBlock xor (wCRC and 0x00FF).toByte()
        val chBlockInt = (chBlock.toInt() xor (chBlock.toInt() shl 4))
        wCRC = wCRC shr 8 xor (chBlockInt and 0xFF shl 8) and 0xFFFF xor (chBlockInt and 0xFF shl 3 and 0xFFFF) xor (chBlockInt and 0xFF shr 4 and 0xFFFF)
        // (wCRC>>8)^((int)chBlock<<8)^((int) chBlock<<3)^((int)chBlock>>4);
    } while (i < this.size)

    return byteArrayOf((wCRC and 0xFF).toByte(), (wCRC and 0xFFFF shr 8).toByte())
}
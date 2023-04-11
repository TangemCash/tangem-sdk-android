package com.tangem.common.extensions

import java.nio.charset.Charset
import java.security.MessageDigest

/**
 * Extension functions for [String].
 */
fun String.calculateSha256(): ByteArray {
    val sha256 = MessageDigest.getInstance("SHA-256")
    val data = this.toByteArray(Charset.forName("UTF-8"))
    return sha256.digest(data)
}

fun String.calculateSha512(): ByteArray {
    val sha = MessageDigest.getInstance("SHA-512")
    val data = this.toByteArray(Charset.forName("UTF-8"))
    return sha.digest(data)
}

@Suppress("MagicNumber")
fun String.hexToBytes(): ByteArray {
    val hexString = this.removePrefix("0x")
    return ByteArray(hexString.length / 2) { i ->
        Integer.parseInt(hexString.substring(2 * i, 2 * i + 2), 16).toByte()
    }
}

fun String.toSnakeCase(): String = replace("(?<=.)(?=\\p{Upper})".toRegex(), "_")

fun String.titleFormatted(symbol: String = "=", maxLength: Int = 50): String {
    val quotesSize = (maxLength - this.length) / 2
    val quote = symbol.repeat(quotesSize)
    return "$quote $this $quote"
}

fun String.remove(vararg symbols: String): String {
    var newString = this
    symbols.forEach { newString = newString.replace(it, "") }
    return newString
}

fun String.leadingZeroPadding(newLength: Int): String {
    if (length >= newLength) return this
    val prefix = StringBuilder()
    repeat(newLength - length) {
        prefix.append("0")
    }
    return prefix.append(this).toString()
}

/**
 * Converts binary [String] to [ByteArray]
 *
 */
@OptIn(ExperimentalUnsignedTypes::class)
@Suppress("MagicNumber")
fun String.binaryToByteArray(): UByteArray? {
    if (this.length % BYTE_SIZE != 0) return null

    val binaryBytes = this.chunked(BYTE_SIZE)

    val bytes = UByteArray(this.length / BYTE_SIZE)

    binaryBytes.forEachIndexed { i, binaryByte ->
        val byte = try {
            binaryByte.toUByte(2)
        } catch (e: NumberFormatException) {
            return null
        }
        bytes[i] = byte
    }
    return bytes
}

private const val BYTE_SIZE = 8

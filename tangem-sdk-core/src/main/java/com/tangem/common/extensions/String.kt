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

fun String.hexToBytes(): ByteArray {
    return ByteArray(this.length / 2)
    { i ->
        Integer.parseInt(this.substring(2 * i, 2 * i + 2), 16).toByte()
    }
}

fun String.toSnakeCase(): String = replace("(?<=.)(?=\\p{Upper})".toRegex(), "_")

fun String.toCamelCase(): String = split('_').joinToString("", transform = String::capitalize)

fun String.titleFormatted(maxLength: Int = 50): String {
    val quotesSize = (maxLength - this.length) / 2
    val quote = "=".repeat(quotesSize)
    return "$quote $this $quote"
}
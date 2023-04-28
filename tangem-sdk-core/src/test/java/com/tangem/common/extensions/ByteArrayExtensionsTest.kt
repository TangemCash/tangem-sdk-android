package com.tangem.common.extensions

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Calendar

class ByteArrayExtensionsTest {

    @Test
    fun `card Id to Hex String`() {
        val hex = "CB22000000027374"
        val bytes = byteArrayOf(-53, 34, 0, 0, 0, 2, 115, 116)
        assertThat(bytes.toHexString())
            .matches(hex)
    }

    @Test
    fun `batch Id to Hex String`() {
        val hex = "0029"
        val bytes = byteArrayOf(0, 41)
        assertThat(bytes.toHexString())
            .matches(hex)
    }

    @Test
    fun `curve name to Utf8`() {
        val bytes = byteArrayOf(115, 101, 99, 112, 50, 53, 54, 107, 49, 0)
        val expected = "secp256k1"
        val converted = bytes.toUtf8()
        assertThat(converted)
            .matches(expected)
    }

    @Test
    fun `empty byteArray to Utf8 returns empty String`() {
        val bytes = byteArrayOf()
        val expected = ""
        assertThat(bytes.toUtf8())
            .matches(expected)
    }

    @Test
    fun `blockchain name to Utf8`() {
        val bytes = byteArrayOf(69, 84, 72)
        val expected = "ETH"
        val converted = bytes.toUtf8()
        assertThat(converted)
            .matches(expected)
    }

    @Test
    fun `bytes to int`() {
        val bytes = byteArrayOf(0, 2, 106, 3)
        val expected = 158211
        assertThat(bytes.toInt())
            .isEqualTo(expected)

        val bytes1 = byteArrayOf(0, 0, 0, 13)
        val expected1 = 13
        assertThat(bytes1.toInt())
            .isEqualTo(expected1)
    }

    @Test
    fun `zero to int`() {
        val bytes = byteArrayOf(0)
        val expected = 0
        assertThat(bytes.toInt())
            .isEqualTo(expected)
    }

    @Test
    fun `to long and back`() {
        fun compare(toCompare: Long) {
            val left = toCompare.toByteArray().toLong()
            val right = toCompare.toByteArray().toLong()
            assert(left == right)
        }

        compare(100)
        compare(Long.MIN_VALUE)
        compare(Long.MAX_VALUE)
    }

    @Test
    fun toDate() {
        val bytes1 = byteArrayOf(7, -30, 7, 27)
        val expected1 = Calendar.getInstance().apply { this.set(2018, 6, 27, 0, 0, 0) }.time
        val converted1 = bytes1.toDate()
        assertThat(converted1.toString())
            .isEqualTo(expected1.toString())

        val bytes2 = byteArrayOf(7, -30, 7, 27, 30)
        val expected2 = Calendar.getInstance().apply { this.set(2018, 6, 27, 0, 0, 0) }.time
        val converted2 = bytes2.toDate()
        assertThat(converted2.toString())
            .isEqualTo(expected2.toString())

        val bytes3 = byteArrayOf(7, -30, 7)
        val expected3 = Calendar.getInstance().apply { this.set(2018, 6, 0, 0, 0, 0) }.time
        val converted3 = bytes3.toDate()
        assertThat(converted3.toString())
            .isEqualTo(expected3.toString())
    }

    @Test
    fun `calculate sha512`() {
        val bytes = ByteArray(64) { 5 }
        val expected = byteArrayOf(
            -123, 96, 121, 57, -117, -23, -108, 57, 25, -119, -22, 97, 11, -91,
            74, -19, -88, 21, -108, -116, -100, 111, 6, -78, 114, -115, 70, -121, 29, 102, 104, 65,
            -21, -68, -111, 121, -51, 109, -94, -24, -40, 108, -25, 70, -26, 61, 38, 12, -127, -34,
            -77, -81, 81, -32, -89, -112, -31, -33, 91, 114, 89, 127, -123, -58,
        )
        assertThat(bytes.calculateSha512())
            .isEqualTo(expected)
    }
}

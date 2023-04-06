package com.tangem.common.extensions

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class StringExtensionsTest {

    @Test
    fun `calculate SHA 256 for default PIN 1`() {
        val pin = "000000"
        val expected = byteArrayOf(
            -111, -76, -47, 66, -126, 63, 125, 32, -59, -16, -115, -10, -111,
            34, -34, 67, -13, 95, 5, 122, -104, -115, -106, 25, -10, -45, 19, -124, -123, -55, -94, 3,
        )
        assertThat(pin.calculateSha256())
            .isEqualTo(expected)
    }

    @Test
    fun `calculate SHA 256 for default PIN 2`() {
        val pin = "000"
        val expected = byteArrayOf(
            42, -55, -90, 116, 106, -54, 84, 58, -8, -33, -13, -104, -108, -49,
            -24, 23, 58, -5, -94, 30, -80, 28, 111, -82, 51, -43, 41, 71, 34, 40, 85, -17,
        )
        assertThat(pin.calculateSha256())
            .isEqualTo(expected)
    }

    @Test
    fun `calculate SHA 256 for a sample PIN 1`() {
        val pin = "999999"
        val expected = byteArrayOf(
            -109, 115, 119, -16, 86, 22, 15, -60, -79, 94, 11, 119, 12, 103,
            19, 106, 95, 3, -63, 82, 5, -76, -45, -65, -111, -126, 104, -2, -6, 44, 109, 10,
        )
        assertThat(pin.calculateSha256())
            .isEqualTo(expected)
    }

    @Test
    fun `calculate SHA 256 for a sample PIN 2`() {
        val pin = "999"
        val expected = byteArrayOf(
            -125, -49, -117, 96, -99, -26, 0, 54, -88, 39, 123, -48, -23, 97,
            53, 117, 27, -68, 7, -21, 35, 66, 86, -44, -74, 91, -119, 51, 96, 101, 27, -14,
        )
        assertThat(pin.calculateSha256())
            .isEqualTo(expected)
    }

    @Test
    fun `card ID hex to bytes`() {
        val cardId = "cb22000000027374"
        val expected = byteArrayOf(-53, 34, 0, 0, 0, 2, 115, 116)
        assertThat(cardId.hexToBytes())
            .isEqualTo(expected)
    }
}

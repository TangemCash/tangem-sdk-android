package com.tangem.crypto

import com.google.common.truth.Truth
import com.tangem.common.extensions.hexToBytes
import org.junit.Test
import org.junit.jupiter.api.Assertions

internal class Base58Test {

    @Test
    fun testRoundTrip() {
        val data = ByteArray(32) { 1 }
        Truth.assertThat(data.encodeToBase58WithChecksum().decodeBase58WithChecksum())
            .isEqualTo(data)
        Truth.assertThat(data.encodeToBase58String().decodeBase58())
            .isEqualTo(data)
    }

    @Test
    fun testBase58() {
        val ethalonString = "1NS17iag9jJgTHD1VXjvLCEnZuQ3rJDE9L"
        val testData = "00eb15231dfceb60925886b67d065299925915aeb172c06647".hexToBytes()
        Assertions.assertEquals(ethalonString, testData.encodeToBase58String())
    }
}

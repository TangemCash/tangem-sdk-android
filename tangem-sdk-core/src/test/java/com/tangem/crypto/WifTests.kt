package com.tangem.crypto

import com.tangem.common.extensions.hexToBytes
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

internal class WifTests {
    @Test
    fun testRoundTrip() {
        val key = "589aeb596710f33d7ac31598ec10440a7df8808cf2c3d69ba670ff3fae66aafb".hexToBytes()
        val wif = "KzBwvPW6L5iwJSiE5vgS52Y69bUxfwizW3wF4C4Xa3ba3pdd7j63"
        assertContentEquals(key, WIF.decodeWIFCompressed(wif))
        assertEquals(wif, WIF.encodeToWIFCompressed(privateKey = key, networkType = NetworkType.Mainnet))
    }
}

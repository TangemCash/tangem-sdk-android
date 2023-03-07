package com.tangem.common.extensions

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class IntExtensionsTest {

    @Test
    fun `small int toByteArray`() {
        val int = 13
        val expected = byteArrayOf(0, 0, 0, 13)
        assertThat(int.toByteArray())
            .isEqualTo(expected)
    }

    @Test
    fun `int toByteArray`() {
        val int = 999
        val expected = byteArrayOf(0, 0, 3, -25)
        assertThat(int.toByteArray())
            .isEqualTo(expected)
    }
}
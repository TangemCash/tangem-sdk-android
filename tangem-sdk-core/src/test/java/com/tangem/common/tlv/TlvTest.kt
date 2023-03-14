package com.tangem.common.tlv

import com.google.common.truth.Truth.assertThat
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.hexToBytes
import com.tangem.operations.files.FileSettings
import com.tangem.operations.files.FileVisibility
import org.junit.jupiter.api.Test

class TlvTest {

    @Test
    fun `TLVs to bytes, only PIN`() {
        val tlvs = listOf(
            Tlv(TlvTag.Pin, "000000".calculateSha256())
        )
        val expected = byteArrayOf(
            16, 32, -111, -76, -47, 66, -126, 63, 125, 32, -59, -16, -115,
            -10, -111, 34, -34, 67, -13, 95, 5, 122, -104, -115, -106, 25, -10, -45, 19, -124,
            -123, -55, -94, 3
        )

        assertThat(tlvs.serialize())
            .isEqualTo(expected)
    }

    @Test
    fun `TLVs to bytes, check wallet`() {
        val tlvs = listOf(
            Tlv(TlvTag.Pin, "000000".calculateSha256()),
            Tlv(TlvTag.CardId, "cb22000000027374".hexToBytes()),
            Tlv(
                TlvTag.Challenge,
                byteArrayOf(-82, -78, -31, 34, 66, -19, -86, -1, 26, 8, 100, -126, -74, 20, -28, 83)
            )
        )

        val expected = byteArrayOf(
            16, 32, -111, -76, -47, 66, -126, 63, 125, 32, -59, -16, -115, -10,
            -111, 34, -34, 67, -13, 95, 5, 122, -104, -115, -106, 25, -10, -45, 19, -124, -123,
            -55, -94, 3, 1, 8, -53, 34, 0, 0, 0, 2, 115, 116, 22, 16, -82, -78, -31, 34, 66, -19,
            -86, -1, 26, 8, 100, -126, -74, 20, -28, 83
        )

        assertThat(tlvs.serialize())
            .isEqualTo(expected)
    }

    @Test
    fun `Bytes to Tlvs, only PIN`() {
        val bytes = byteArrayOf(
            16, 32, -111, -76, -47, 66, -126, 63, 125, 32, -59, -16, -115,
            -10, -111, 34, -34, 67, -13, 95, 5, 122, -104, -115, -106, 25, -10, -45, 19, -124,
            -123, -55, -94, 3
        )

        val tlvs = Tlv.deserialize(bytes)

        assertThat(tlvs)
            .isNotNull()
        assertThat(tlvs)
            .isNotEmpty()

        val pin = tlvs!!.find { it.tag == TlvTag.Pin }?.value
        val pinExpected = "000000".calculateSha256()

        assertThat(pin)
            .isEqualTo(pinExpected)
    }

    @Test
    fun `Bytes to TLVs, check wallet TLVs`() {
        val bytes = byteArrayOf(
            16, 32, -111, -76, -47, 66, -126, 63, 125, 32, -59, -16, -115, -10,
            -111, 34, -34, 67, -13, 95, 5, 122, -104, -115, -106, 25, -10, -45, 19, -124, -123,
            -55, -94, 3, 1, 8, -53, 34, 0, 0, 0, 2, 115, 116, 22, 16, -82, -78, -31, 34, 66, -19,
            -86, -1, 26, 8, 100, -126, -74, 20, -28, 83
        )

        val tlvs = Tlv.deserialize(bytes)

        assertThat(tlvs)
            .isNotNull()
        assertThat(tlvs)
            .isNotEmpty()

        val pin = tlvs!!.find { it.tag == TlvTag.Pin }?.value
        val pinExpected = "000000".calculateSha256()
        assertThat(pin)
            .isEqualTo(pinExpected)

        val cardId = tlvs.find { it.tag == TlvTag.CardId }?.value
        val cardIdExpected = "cb22000000027374".hexToBytes()
        assertThat(cardId)
            .isEqualTo(cardIdExpected)

        val challenge = tlvs.find { it.tag == TlvTag.Challenge }?.value
        val challengeExpected =
            byteArrayOf(-82, -78, -31, 34, 66, -19, -86, -1, 26, 8, 100, -126, -74, 20, -28, 83)
        assertThat(challenge)
            .isEqualTo(challengeExpected)
    }

    @Test
    fun `Bytes to TLVs, wrong values`() {
        val bytes = byteArrayOf(0)
        val tlvs = Tlv.deserialize(bytes)
        assertThat(tlvs)
            .isNull()

        val bytes1 = byteArrayOf(0, 0, 0, 0, 0, 0, 0)
        val tlvs1 = Tlv.deserialize(bytes1)
        assertThat(tlvs1)
            .isNull()
    }

    @Test
    fun `parse Slix tag response`() {
        val response = "03ff010f91010b550474616e67656d2e636f6d140f11616e64726f69642e636f6d3a706b67636f6d2e74616e6765" +
            "6d2e77616c6c65745411c974616e67656d2e636f6d3a77616c6c657490000c618102ffff8a0102820407e40109830b54414e474" +
            "54d2053444b008403584c4d86400e71c1f060387029688254320b90abeae471bcafbbe8ea3880903bdb8d1cc389d032b982e1ff" +
            "d7ef49e66f1780123b763dd2f3a9a9494eb0fad4ae8cf306672c60207c967a51077c14fc49d867f23b8d0eaf60cad479a56587e" +
            "894571b7fb33690176140345fbe53f5be0ec871e91c317cde2bd0396d47e4b945c138c153b0271f636a73cf531df1bc54ac4fcd" +
            "bce42f81b40d58e0265d34e28121a4c50fdfe329a97f6000fe00000000000000000000000000000000000000000000000000000" +
            "0000000000000000000000000"

        val tlvs = Tlv.deserialize(response.hexToBytes(), true)
        assertThat(tlvs).isNotEmpty()
    }

    @Test
    fun testFileSettings() {
        val byte = byteArrayOf(0x11)
        val settings = FileSettings(byte)
        assertThat(settings).isNotNull()
        assertThat(settings!!.isPermanent).isTrue()
        assertThat(settings.visibility).isEqualTo(FileVisibility.Public)
    }
}
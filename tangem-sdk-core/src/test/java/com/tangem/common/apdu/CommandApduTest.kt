package com.tangem.common.apdu

import com.google.common.truth.Truth.assertThat
import com.tangem.common.core.SessionEnvironment
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvTag
import org.junit.jupiter.api.Test


class CommandApduTest {

    private val environment = SessionEnvironment.Dummy

    @Test
    fun `simple READ command to bytes`() {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.Pin, environment.accessCode.value)
        val commandApdu = CommandApdu(
                Instruction.Read,
                tlvBuilder.serialize()
        )
        val expected = byteArrayOf(0, -14, 0, 0, 0, 0, 34, 16, 32, -111, -76, -47, 66, -126, 63, 125,
                32, -59, -16, -115, -10, -111, 34, -34, 67, -13, 95, 5, 122, -104, -115, -106, 25, -10,
                -45, 19, -124, -123, -55, -94, 3)

        assertThat(commandApdu.apduData)
                .isEqualTo(expected)
        assertThat(listOf(1, 2)).containsExactlyElementsIn(listOf(1, 2))
    }

    @Test
    fun `READ with terminal key to bytes`() {
        val terminalPublicKey = byteArrayOf(4, 80, -122, 58, -42, 74, -121, -82, -118, 47, -24, 60,
                26, -15, -88, 64, 60, -75, 63, 83, -28, -122, -40, 81, 29, -83, -118, 4, -120, 126,
                91, 35, 82, 44, -44, 112, 36, 52, 83, -94, -103, -6, -98, 119, 35, 119, 22, 16, 58,
                -68, 17, -95, -33, 56, -123, 94, -42, -14, -18, 24, 126, -100, 88, 43, -90)
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.Pin, environment.accessCode.value)
        tlvBuilder.append(TlvTag.TerminalPublicKey, terminalPublicKey)
        val commandApdu = CommandApdu(
                Instruction.Read,
                tlvBuilder.serialize()
        )

        val expected = byteArrayOf(0, -14, 0, 0, 0, 0, 101, 16, 32, -111, -76, -47, 66, -126, 63,
                125, 32, -59, -16, -115, -10, -111, 34, -34, 67, -13, 95, 5, 122, -104, -115, -106, 25,
                -10, -45, 19, -124, -123, -55, -94, 3, 92, 65, 4, 80, -122, 58, -42, 74, -121, -82, -118,
                47, -24, 60, 26, -15, -88, 64, 60, -75, 63, 83, -28, -122, -40, 81, 29, -83, -118, 4, -120,
                126, 91, 35, 82, 44, -44, 112, 36, 52, 83, -94, -103, -6, -98, 119, 35, 119, 22, 16, 58,
                -68, 17, -95, -33, 56, -123, 94, -42, -14, -18, 24, 126, -100, 88, 43, -90)

        assertThat(commandApdu.apduData)
                .isEqualTo(expected)
    }
}

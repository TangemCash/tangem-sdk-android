package com.tangem.common.apdu

import com.google.common.truth.Truth.assertThat
import com.tangem.common.core.SessionEnvironment
import com.tangem.common.tlv.TlvTag
import org.junit.jupiter.api.Test

class ResponseApduTest {

    private val environment = SessionEnvironment.Dummy

    @Test
    fun `get StatusWord returns Unknown`() {
        val corruptData = byteArrayOf(0, 0, 0, 0)
        val responseApdu = ResponseApdu(corruptData)
        assertThat(responseApdu.statusWord)
                .isEqualTo(StatusWord.Unknown)
    }

    @Test
    fun `get StatusWord returns ProcessCompleted`() {
        val data = byteArrayOf(0, 0, 0, 0, -112, 0)
        val responseApdu = ResponseApdu(data)
        assertThat(responseApdu.statusWord)
                .isEqualTo(StatusWord.ProcessCompleted)
    }

    @Test
    fun `corrupt response, getTlvData returns null`() {
        val corruptData = byteArrayOf(0, 0, 0)
        val responseApdu = ResponseApdu(corruptData)
        assertThat(responseApdu.getTlvData(environment.encryptionKey))
                .isNull()
    }

    @Test
    fun `response, getTlvData returns cardId`() {
        val data = byteArrayOf(1, 8, -53, 34, 0, 0, 0, 2, 115, 116, 32, 11, 83, 77, 65, 82, 84, 32, 67, 65, 83, 72, 0, 2, 1, 2, -128, 6, 50, 46, 49, 49, 114, 0, 3, 65, 4, -49, 11, -50, -66, -121, -25, -2, 65, 65, -13, 14, 49, 27, -82, -33, -85, -113, 65, 20, 8, -39, -75, 57, 45, 65, -31, 35, 44, 38, 40, 63, -44, 113, -45, -75, -95, -118, 118, 29, 65, 117, -24, -53, 82, -72, 91, -20, -96, -77, -103, -14, -63, 52, -127, -123, -27, -16, -128, -67, -3, -104, -26, -22, 65, 10, 4, 0, 0, 126, 33, 12, 90, -127, 2, 0, 41, -126, 4, 7, -29, 5, 2, -125, 7, 84, 65, 78, 71, 69, 77, 0, -124, 3, 69, 84, 72, -122, 64, 111, -103, 48, -114, -40, 18, -103, 26, -102, -12, -38, -78, -90, -9, -98, 88, -47, -100, -24, 24, -105, -70, -72, 6, 94, -96, -77, 11, -123, -28, -118, 37, 63, 107, -55, -11, 23, -12, 13, -23, -121, -63, 36, -59, 70, 116, 91, -125, -34, -69, 23, -112, 6, 17, 4, -49, 68, -56, 29, -45, 81, 10, 97, 83, 48, 65, 4, -127, -106, -86, 75, 65, 10, -60, 74, 59, -100, -50, 24, -25, -66, 34, 106, -22, 7, 10, -52, -125, -87, -49, 103, 84, 15, -84, 73, -81, 37, 18, -97, 106, 83, -118, 40, -83, 99, 65, 53, -114, 60, 79, -103, 99, 6, 79, 126, 54, 83, 114, -90, 81, -45, 116, -27, -62, 60, -35, 55, -3, 9, -101, -14, 5, 10, 115, 101, 99, 112, 50, 53, 54, 107, 49, 0, 8, 4, 0, 15, 66, 64, 7, 1, 0, 9, 2, 11, -72, 96, 65, 4, -42, -5, -41, -84, -23, 88, 2, 86, -63, -118, -123, -10, -66, -82, -107, -68, -93, 111, 47, 93, -20, -86, 74, 28, 21, 81, 93, -21, -124, -57, -102, 55, 17, 84, -66, -68, -22, -128, 126, -99, -65, -54, -42, 59, -25, -21, -124, 5, 59, -16, -72, 73, 48, 16, -27, 103, -112, -73, 2, 96, -51, 41, -42, 116, 98, 4, 0, 15, 66, 52, 99, 4, 0, 0, 0, 13, 15, 1, 0, -112, 0)
        val responseApdu = ResponseApdu(data)
        assertThat(responseApdu.getTlvData(environment.encryptionKey))
                .isNotNull()
        assertThat(responseApdu.getTlvData(environment.encryptionKey))
                .isNotEmpty()
        assertThat(responseApdu.getTlvData(environment.encryptionKey)?.filter { it.tag == TlvTag.Unknown })
                .isEmpty()
    }
}

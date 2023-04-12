package com.tangem.common

import com.tangem.common.card.FirmwareVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.Test

/**
 * Created by Anton Zhilenkov on 01/02/2022.
 */
class CommonTests {

    @Test
    fun testFirmwareParse() {
        val dev = FirmwareVersion("4.45d SDK")
        assertEquals(dev.major, 4)
        assertEquals(dev.minor, 45)
        assertEquals(dev.patch, 0)
        assertEquals(dev.type, FirmwareVersion.FirmwareType.Sdk)

        val spec = FirmwareVersion("4.45 mfi")
        assertEquals(spec.major, 4)
        assertEquals(spec.minor, 45)
        assertEquals(spec.patch, 0)
        assertEquals(spec.type, FirmwareVersion.FirmwareType.Sprecial)

        val spec1 = FirmwareVersion("4.45m")
        assertEquals(spec, spec1)

        val rel = FirmwareVersion("4.45r")
        assertEquals(rel.major, 4)
        assertEquals(rel.minor, 45)
        assertEquals(rel.patch, 0)
        assertEquals(rel.type, FirmwareVersion.FirmwareType.Release)

        val rel1 = FirmwareVersion("4.45")
        assertEquals(rel, rel1)
    }
}

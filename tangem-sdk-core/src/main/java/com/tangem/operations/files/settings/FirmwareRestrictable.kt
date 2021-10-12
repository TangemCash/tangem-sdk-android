package com.tangem.operations.files.settings

import com.tangem.common.card.FirmwareVersion

/**
 * Protocol that determines what firmware versions will be capable for performing command
 */
interface FirmwareRestrictable {
    val minFirmwareVersion: FirmwareVersion
    val maxFirmwareVersion: FirmwareVersion
}
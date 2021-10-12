package com.tangem.common.extensions

import com.tangem.common.card.FirmwareVersion
import com.tangem.operations.files.settings.FirmwareRestrictable

/**
 * Created by Anton Zhilenkov on 20/07/2021.
 */
fun Set<FirmwareRestrictable>.minFirmwareVersion(): FirmwareVersion {
    return map { it.minFirmwareVersion }.maxOrNull() ?: FirmwareVersion.Min
}

fun Set<FirmwareRestrictable>.maxFirmwareVersion(): FirmwareVersion {
    return map { it.maxFirmwareVersion }.minOrNull() ?: FirmwareVersion.Min
}
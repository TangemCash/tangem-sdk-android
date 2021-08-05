package com.tangem.operations.files.settings

import com.tangem.common.card.FirmwareVersion

/**
 * Created by Anton Zhilenkov on 20/07/2021.
 */
enum class ReadFileCommandSettings : FirmwareRestrictable {
    CheckFileValidationHash;

    override val minFirmwareVersion: FirmwareVersion
        get() = FirmwareVersion(3, 34)
    override val maxFirmwareVersion: FirmwareVersion
        get() = FirmwareVersion.Max
}
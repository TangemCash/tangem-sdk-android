package com.tangem.operations.files.settings

import com.tangem.common.card.FirmwareVersion

/**
 * Created by Anton Zhilenkov on 20/07/2021.
 */
enum class FileWriteSettings : FirmwareRestrictable {
    None, VerifiedWithPasscode;

    override val minFirmwareVersion: FirmwareVersion
        get() = when (this) {
            None -> FirmwareVersion(3, 29)
            VerifiedWithPasscode -> FirmwareVersion(3, 34)
        }

    override val maxFirmwareVersion: FirmwareVersion
        get() = when (this) {
            None, VerifiedWithPasscode -> FirmwareVersion.Max
        }
}
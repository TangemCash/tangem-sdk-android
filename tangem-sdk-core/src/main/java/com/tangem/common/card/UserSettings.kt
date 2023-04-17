package com.tangem.common.card

import com.tangem.common.BaseMask
import com.tangem.common.Mask
import com.tangem.common.MaskBuilder

data class UserSettings(
    /**
     * Is allowed to recover user codes
     */
    val isUserCodeRecoveryAllowed: Boolean,
) {
    val mask: UserSettingsMask
        get() {
            val builder = MaskBuilder()
            if (isUserCodeRecoveryAllowed) builder.add(UserSettingsMask.Code.ForbidResetPIN)
            return builder.build()
        }

    internal constructor(
        mask: UserSettingsMask,
    ) : this(mask.contains(UserSettingsMask.Code.ForbidResetPIN))
}

class UserSettingsMask(override var rawValue: Int) : BaseMask() {

    override val values: List<Code> = Code.values().toList()

    enum class Code(override val value: Int) : Mask.Code {
        ForbidResetPIN(value = 0x00000001),
    }
}

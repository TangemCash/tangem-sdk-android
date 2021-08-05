package com.tangem.common

import com.tangem.common.core.SessionEnvironment
import com.tangem.common.extensions.calculateSha256

class UserCode constructor(
    val type: UserCodeType,
    val value: ByteArray?
) {
    constructor(type: UserCodeType) : this(type, type.defaultValue)

    constructor(type: UserCodeType, stringValue: String) : this(type, stringValue.calculateSha256())

    companion object {
        const val DefaultAccessCode = "000000"
        const val DefaultPasscode = "000"
    }
}

enum class UserCodeType(val defaultValue: String) {
    AccessCode(UserCode.DefaultAccessCode),
    Passcode(UserCode.DefaultPasscode);

    fun isWrongPinEntered(environment: SessionEnvironment?): Boolean {
        val environment = environment ?: return false
        return when (this) {
            AccessCode -> !environment.isUserCodeSet(AccessCode)
            Passcode -> !environment.isUserCodeSet(Passcode)
        }
    }
}


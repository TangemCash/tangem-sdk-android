package com.tangem.operations.resetcode

enum class AuthorizeMode(val rawValue: Int) {
    FileOwnerGetChallenge(rawValue = 0x01),
    FileOwnerAuthenticate(rawValue = 0x02),
    TokenGet(rawValue = 0x03),
    TokenSign(rawValue = 0x04),
    TokenAuthenticate(rawValue = 0x05),
    ;

    companion object {
        private val values = com.tangem.operations.read.ReadMode.values()
        fun byRawValue(rawValue: Int): com.tangem.operations.read.ReadMode? = values.find { it.rawValue == rawValue }
    }
}
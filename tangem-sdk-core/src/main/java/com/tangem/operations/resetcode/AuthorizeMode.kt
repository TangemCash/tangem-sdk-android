package com.tangem.operations.resetcode

enum class AuthorizeMode(val rawValue: Int) {
    FileOwnerGetChallenge(0x01),
    FileOwnerAuthenticate(0x02),
    TokenGet(0x03),
    TokenSign(0x04),
    TokenAuthenticate(0x05),
    ;

    companion object {
        private val values = com.tangem.operations.read.ReadMode.values()
        fun byRawValue(rawValue: Int): com.tangem.operations.read.ReadMode? = values.find { it.rawValue == rawValue }
    }
}
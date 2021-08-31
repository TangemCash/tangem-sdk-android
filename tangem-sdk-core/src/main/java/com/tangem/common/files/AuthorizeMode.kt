package com.tangem.common.files

enum class AuthorizeMode(val rawValue: Int) {
    FileOwner_GetChallenge(0x01),
    FileOwner_Authenticate(0x02),
    Token_Get(0x03),
    Token_Sign(0x04),
    Token_Authenticate(0x05);

    companion object {
        private val values = values()
        fun byRawValue(rawValue: Int): AuthorizeMode? = values.find { it.rawValue == rawValue }
    }
}
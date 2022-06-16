package com.tangem.common.accesscode

interface AccessCodeRepository {
    fun get(cardId: String): Result<ByteArray>
    fun append(cardId: String, accessCode: ByteArray)

    fun getRememberCodeToggleState(cardId: String, defaultState: Boolean = true): Result<Boolean>
    fun saveRememberCodeToggleState(cardId: String, isToggled: Boolean)
}

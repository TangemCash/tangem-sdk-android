package com.tangem.common.accesscode

class DummyAccessCodeRepository : AccessCodeRepository {
    override fun get(cardId: String): Result<ByteArray> {
        return Result.success(value = ByteArray(1))
    }

    override fun append(cardId: String, accessCode: ByteArray) {
        /* no-op */
    }

    override fun getRememberCodeToggleState(
        cardId: String,
        defaultState: Boolean
    ): Result<Boolean> {
        return Result.success(value = false)
    }

    override fun saveRememberCodeToggleState(cardId: String, isToggled: Boolean) {
        /* no-op */
    }
}

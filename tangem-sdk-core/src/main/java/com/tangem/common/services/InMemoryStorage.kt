package com.tangem.common.services

import com.tangem.common.services.secure.SecureStorage

class InMemoryStorage : SecureStorage {
    private val byteArrayStorage = mutableMapOf<String, ByteArray>()

    override fun get(account: String): ByteArray? {
        return byteArrayStorage[account]
    }

    override fun store(data: ByteArray, account: String, overwrite: Boolean) {
        byteArrayStorage[account] = data
    }

    override fun delete(account: String) {
        byteArrayStorage.remove(account)
    }

    override fun storeKey(key: ByteArray, account: String) {
        store(key, account)
    }

    override fun readKey(account: String): ByteArray? {
        return get(account)
    }
}

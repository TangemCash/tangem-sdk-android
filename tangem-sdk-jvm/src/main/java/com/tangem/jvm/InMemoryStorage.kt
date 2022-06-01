package com.tangem.jvm

import com.tangem.common.services.Storage
import com.tangem.common.services.secure.SecureStorage

internal class InMemoryStorage : SecureStorage, Storage {
    private val byteArrayStorage = mutableMapOf<String, ByteArray>()
    private val stringStorage = mutableMapOf<String, String>()

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

    override fun putString(key: String, value: String) {
        stringStorage[key] = value
    }

    override fun getString(key: String): String? {
        return stringStorage[key]
    }
}

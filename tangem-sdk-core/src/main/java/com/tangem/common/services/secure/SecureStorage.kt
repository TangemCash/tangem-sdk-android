package com.tangem.common.services.secure

/**
 * Created by Anton Zhilenkov on 06/10/2020.
 */
interface SecureStorage {
    fun get(account: String): ByteArray?
    fun store(data: ByteArray, account: String, overwrite: Boolean = true)
    fun delete(account: String)
    fun storeKey(key: ByteArray, account: String)
    fun readKey(account: String): ByteArray?

    companion object
}

class UnsafeInMemoryStorage : SecureStorage {
    private val storage = mutableMapOf<String, ByteArray>()

    override fun get(account: String): ByteArray? {
        return storage[account]
    }

    override fun store(data: ByteArray, account: String, overwrite: Boolean) {
        storage[account] = data
    }

    override fun delete(account: String) {
        storage.remove(account)
    }

    override fun storeKey(key: ByteArray, account: String) {
        store(key, account)
    }

    override fun readKey(account: String): ByteArray? {
        return get(account)
    }
}

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

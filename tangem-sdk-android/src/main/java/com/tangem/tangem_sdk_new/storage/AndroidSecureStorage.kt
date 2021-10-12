package com.tangem.tangem_sdk_new.storage

import android.content.Context
import at.favre.lib.armadillo.Armadillo
import at.favre.lib.armadillo.ArmadilloSharedPreferences
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import com.tangem.common.services.secure.SecureStorage

/**
 * Created by Anton Zhilenkov on 25/07/2021.
 */
class AndroidSecureStorage(
    private val preferences: ArmadilloSharedPreferences
) : SecureStorage {

    private val editor = preferences.edit()

    override fun get(account: String): ByteArray? {
        return preferences.getString(account, null)?.hexToBytes()
    }

    override fun store(data: ByteArray, account: String, overwrite: Boolean) {
        editor.putString(account, data.toHexString()).apply()
    }

    override fun delete(account: String) {
        editor.remove(account).apply()
    }

    override fun storeKey(key: ByteArray, account: String) {
        store(key, account)
    }

    override fun readKey(account: String): ByteArray? {
        return get(account)
    }
}

fun SecureStorage.Companion.createEncryptedSharedPreferences(
    context: Context,
    storageName: String
): ArmadilloSharedPreferences = Armadillo.create(context, storageName).encryptionFingerprint(context).build()

fun SecureStorage.Companion.create(context: Context): SecureStorage {
    val sharedPreferences = createEncryptedSharedPreferences(context, "tangemSdkStorage")
    return AndroidSecureStorage(sharedPreferences)
}
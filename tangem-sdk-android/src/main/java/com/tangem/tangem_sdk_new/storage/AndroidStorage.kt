package com.tangem.tangem_sdk_new.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.tangem.common.services.Storage

class AndroidStorage private constructor(
    private val preferences: SharedPreferences
) : Storage {
    override fun putString(key: String, value: String) {
        preferences.edit { putString(key, value) }
    }

    override fun getString(key: String): String? {
        return preferences.getString(key, null)
    }

    companion object {
        fun create(context: Context): Storage {
            return AndroidStorage(
                preferences = context.getSharedPreferences(
                    "tangemSdkPreferences",
                    Context.MODE_PRIVATE
                )
            )
        }
    }
}

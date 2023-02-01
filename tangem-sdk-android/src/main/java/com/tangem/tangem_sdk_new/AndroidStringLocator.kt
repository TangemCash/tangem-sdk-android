package com.tangem.tangem_sdk_new

import android.content.Context
import com.tangem.common.StringsLocator
import java.lang.reflect.Field

class AndroidStringLocator(val context: Context) : StringsLocator {
    override fun getString(stringId: StringsLocator.ID, vararg formatArgs: Any): String {
        val resId = try {
            val idField: Field = R.string::class.java.getDeclaredField(stringId.name)
            idField.getInt(idField)
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }

        return if (formatArgs.isEmpty()) {
            context.getString(resId)
        } else {
            context.getString(resId, *formatArgs)
        }
    }
}
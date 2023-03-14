package com.tangem.sdk

import android.content.Context
import android.content.res.Resources
import com.tangem.common.StringsLocator
import java.lang.reflect.Field

class AndroidStringLocator(val context: Context) : StringsLocator {

    @Throws(Resources.NotFoundException::class)
    override fun getString(stringId: StringsLocator.ID, vararg formatArgs: Any): String {
        val resId = try {
            val idField: Field = R.string::class.java.getDeclaredField(stringId.name)
            idField.getInt(idField)
        } catch (e: Exception) {
            -1
        }

        return context.getString(resId, *formatArgs)
    }
}
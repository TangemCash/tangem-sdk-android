package com.tangem.sdk

import android.content.Context
import com.tangem.Log
import com.tangem.common.StringsLocator
import java.lang.reflect.Field

class AndroidStringLocator(val context: Context) : StringsLocator {

    override fun getString(
        stringId: StringsLocator.ID,
        vararg formatArgs: Any,
        defaultValue: String,
    ): String {
        return runCatching {
            val idField: Field = R.string::class.java.getDeclaredField(stringId.name)
            idField.getInt(idField)
        }
            .map { context.getString(it, *formatArgs) }
            .onFailure { e ->
                Log.error {
                    """
                        Unable to find string
                        |- ID: $stringId
                        |- Args: $formatArgs
                        |- Cause: ${e.localizedMessage}
                    """.trimIndent()
                }
            }
            .getOrDefault(defaultValue)
    }
}

package com.tangem.demo

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import androidx.annotation.LayoutRes
import androidx.core.widget.addTextChangedListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Created by Anton Zhilenkov on 24/12/2020.
 */
private val uiHandler = Handler(Looper.getMainLooper())
private val backgroundHandler = Handler(HandlerThread("DemoWorkerThread").apply { start() }.looper)

internal fun postUi(ms: Long = 0, func: Runnable) {
    if (ms == 0L) uiHandler.post { func.run() } else uiHandler.postDelayed(func, ms)
}

internal fun postBackground(ms: Long = 0, func: Runnable) {
    if (ms == 0L) backgroundHandler.post { func.run() } else backgroundHandler.postDelayed(func, ms)
}

fun post(ms: Long = 0, func: Runnable) {
    val currentLooper = Looper.myLooper() ?: return
    val handler = Handler(currentLooper)
    if (ms == 0L) {
        handler.post { func.run() }
    } else {
        handler.postDelayed(func, ms)
    }
}

fun <T> ViewGroup.inflate(@LayoutRes resId: Int, attach: Boolean = false): T =
    LayoutInflater.from(context).inflate(resId, this, attach) as T

@Suppress("ImplicitDefaultLocale")
fun String.splitCamelCase(): String {
    return replace(
        regex = String
            .format(
                "%s|%s|%s",
                "(?<=[A-Z])(?=[A-Z][a-z])",
                "(?<=[^A-Z])(?=[A-Z])",
                "(?<=[A-Za-z])(?=[^A-Za-z])"
            ).toRegex(),
        replacement = " "
    )
}

fun EditText.asFlow(): Flow<String> = callbackFlow {
    val watcher = addTextChangedListener { editable -> trySend(editable?.toString() ?: "").isSuccess }
    awaitClose { removeTextChangedListener(watcher) }
}

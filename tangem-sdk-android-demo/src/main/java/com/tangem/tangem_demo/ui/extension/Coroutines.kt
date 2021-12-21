package com.tangem.tangem_demo.ui.extension

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Created by Anton Zhilenkov on 13/12/2021.
 */
suspend fun <T> withMainContext(block: suspend CoroutineScope.() -> T) {
    withContext(Dispatchers.Main, block)
}

suspend fun <T> withIOContext(block: suspend CoroutineScope.() -> T) {
    withContext(Dispatchers.IO, block)
}
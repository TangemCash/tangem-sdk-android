package com.tangem.common.extensions

/**
 * Created by Anton Zhilenkov on 26/07/2021.
 */
fun <K, V, R> Map<K, V>.mapNotNullValues(transform: (Map.Entry<K, V>) -> R?): Map<K, R> {
    val result = LinkedHashMap<K, R>()
    for (entry in this) {
        val resultValue = transform(entry)
        if (resultValue != null) result[entry.key] = resultValue
    }
    return result
}

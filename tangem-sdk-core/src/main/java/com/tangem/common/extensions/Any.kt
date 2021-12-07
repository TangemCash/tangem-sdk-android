package com.tangem.common.extensions

/**
 * Created by Anton Zhilenkov on 23/11/2020.
 */
inline fun <T> T?.guard(nullClause: () -> Nothing): T {
    return this ?: nullClause()
}

inline fun <reified T> Any.cast(): T = this::javaClass as T

inline fun <reified T> Any?.castOrNull(): T? = if (this == null) null else this::javaClass as T

fun calculateHashCode(vararg hashCodes: Int, factor: Int = 31): Int {
    if (hashCodes.isEmpty()) return 0

    return if (hashCodes.size == 1) {
        factor * hashCodes[0]
    } else {
        hashCodes.drop(1).fold(hashCodes[0], { acc, hash -> acc * factor + hash })
    }
}

typealias VoidCallback = () -> Unit
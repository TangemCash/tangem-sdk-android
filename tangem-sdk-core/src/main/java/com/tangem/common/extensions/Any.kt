package com.tangem.common.extensions

/**
 * Created by Anton Zhilenkov on 23/11/2020.
 */
inline fun <T> T?.guard(nullClause: () -> Nothing): T {
    return this ?: nullClause()
}

inline fun <reified T> Any.cast(): T = this::javaClass as T

inline fun <reified T> Any?.castOrNull(): T? = if (this == null) null else this::javaClass as T

typealias VoidCallback = () -> Unit
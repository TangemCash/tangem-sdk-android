package com.tangem.common.extensions

import com.tangem.common.card.CardWallet
import com.tangem.common.card.WalletIndex

fun <T> List<T>.print(delimiter: String = ", ", wrap: Boolean = true): String {
    val builder = StringBuilder()
    forEach { builder.append(it).append(delimiter) }
    val length = builder.length
    if (length > delimiter.length) {
        builder.delete(length - delimiter.length, length)
    }
    val result = builder.toString()

    return if (wrap) "[$result]" else result
}

operator fun List<CardWallet>.get(walletIndex: WalletIndex): CardWallet? {
    return this.find { it.index == walletIndex }
}

fun <T> List<T>.plusIfNotContains(element: T): List<T> {
    return if (contains(element)) {
        this
    } else {
        this + element
    }
}
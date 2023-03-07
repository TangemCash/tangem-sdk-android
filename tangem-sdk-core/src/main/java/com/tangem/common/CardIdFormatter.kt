package com.tangem.common

import com.tangem.common.core.CardIdDisplayFormat

class CardIdFormatter(
    var style: CardIdDisplayFormat
) {
    fun getFormattedCardId(cardId: String): String? {
        return when (val style = style) {
            CardIdDisplayFormat.None -> null
            CardIdDisplayFormat.Full -> {
                cardId.splitBySpace()
            }
            is CardIdDisplayFormat.Last -> {
                cardId.takeLast(style.numbers).splitBySpace()
            }
            is CardIdDisplayFormat.LastLuhn -> {
                cardId.dropLast(1).takeLast(style.numbers).splitBySpace()
            }
            is CardIdDisplayFormat.LastMasked -> {
                val formattedCardId = cardId.takeLast(style.numbers).splitBySpace()
                return style.mask + formattedCardId
            }
        }
    }

    private fun String.splitBySpace() = reversed().chunked(size = 4).joinToString(separator = " ").reversed()
}
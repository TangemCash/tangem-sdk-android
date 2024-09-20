package com.tangem.operations.preflightread

import com.tangem.common.CardIdFormatter
import com.tangem.common.card.Card
import com.tangem.common.core.SessionEnvironment
import com.tangem.common.core.TangemSdkError

class CardIdPreflightReadFilter(private val expectedCardId: String) : PreflightReadFilter {

    override fun onCardRead(card: Card, environment: SessionEnvironment) {
        if (!expectedCardId.equals(card.cardId, ignoreCase = true)) {
            val formatter = CardIdFormatter(style = environment.config.cardIdDisplayFormat)
            val expectedCardIdFormatted = formatter.getFormattedCardId(expectedCardId)

            throw TangemSdkError.WrongCardNumber(cardId = expectedCardIdFormatted ?: expectedCardId)
        }
    }

    override fun onFullCardRead(card: Card, environment: SessionEnvironment) = Unit

    companion object {
        fun initOrNull(cardId: String?): PreflightReadFilter? {
            return cardId?.let { CardIdPreflightReadFilter(it) }
        }
    }
}

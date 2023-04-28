package com.tangem.common

import com.tangem.common.card.Card
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.TangemSdkError

/**
 * Filter that can be used to limit cards that can be interacted with in TangemSdk.
 */
class CardFilter(
    /**
     * Filter that can be used to limit cards that can be interacted with in TangemSdk.
     */
    var allowedCardTypes: List<FirmwareVersion.FirmwareType> = listOf(
        FirmwareVersion.FirmwareType.Release,
        FirmwareVersion.FirmwareType.Sdk,
    ),

    /**
     * Use this filter to configure cards allowed to work with your app
     */
    var cardIdFilter: CardIdFilter? = null,

    /**
     * Use this filter to configure batches allowed to work with your app
     */
    var batchIdFilter: ItemFilter? = null,

    /**
     * Use this filter to configure issuers allowed to work with your app
     */
    var issuerFilter: ItemFilter? = null,

    /**
     * Use this filter to configure the highest firmware version allowed to work with your app.
     * Null to allow all versions.
     */
    var maxFirmwareVersion: FirmwareVersion? = null,

    /**
     * Custom error localized description
     */
    var localizedDescription: String? = null,
) {

    private val wrongCardError: TangemSdkError
        get() = TangemSdkError.WrongCardType(localizedDescription)

    @Throws(TangemSdkError.WrongCardType::class)
    fun verifyCard(card: Card): Boolean {
        maxFirmwareVersion?.let { maxFirmwareVersion ->
            if (card.firmwareVersion > maxFirmwareVersion) throw wrongCardError
        }

        if (!allowedCardTypes.contains(card.firmwareVersion.type)) throw wrongCardError

        batchIdFilter?.let {
            if (!it.isAllowed(card.batchId)) throw wrongCardError
        }
        issuerFilter?.let {
            if (!it.isAllowed(card.issuer.name)) throw wrongCardError
        }
        cardIdFilter?.let {
            if (!it.isAllowed(card.cardId)) throw wrongCardError
        }

        return true
    }

    companion object {
        fun default(): CardFilter = CardFilter()

        sealed class ItemFilter(val items: Set<String>) {
            class Allow(items: Set<String>) : ItemFilter(items)
            class Deny(items: Set<String>) : ItemFilter(items)

            fun isAllowed(item: String): Boolean {
                return when (this) {
                    is Allow -> items.contains(item)
                    is Deny -> !items.contains(item)
                }
            }
        }

        sealed class CardIdFilter(
            val items: Set<String>,
            val ranges: List<CardIdRange>,
        ) {
            class Allow(items: Set<String>, ranges: List<CardIdRange> = listOf()) : CardIdFilter(items, ranges)
            class Deny(items: Set<String>, ranges: List<CardIdRange> = listOf()) : CardIdFilter(items, ranges)

            fun isAllowed(item: String): Boolean {
                return when (this) {
                    is Allow -> items.contains(item) || ranges.contains(item)
                    is Deny -> !(items.contains(item) || ranges.contains(item))
                }
            }
        }
    }
}

class CardIdRange private constructor(
    val batch: String,
    val start: Long,
    val end: Long,
) {

    fun contains(cardId: String): Boolean {
        if (cardId.getBatchPrefix() != batch) return false
        val cardNumber = cardId.toLong() ?: return false

        return (start..end).contains(cardNumber)
    }

    companion object {
        operator fun invoke(start: String, end: String): CardIdRange? {
            val startBatch = start.getBatchPrefix()
            val endBatch = end.getBatchPrefix()

            if (startBatch != endBatch) return null
            val startValue = start.toLong() ?: return null
            val endValue = end.toLong() ?: return null

            return CardIdRange(
                batch = startBatch,
                start = startValue,
                end = endValue,
            )
        }

        private fun String.getBatchPrefix(): String = this.take(n = 4).uppercase()

        private fun String.stripBatchPrefix(): String = this.drop(n = 4)

        private fun String.toLong(): Long? = this.stripBatchPrefix().toLongOrNull()
    }
}

fun List<CardIdRange>.contains(cardId: String): Boolean = this.any { it.contains(cardId) }

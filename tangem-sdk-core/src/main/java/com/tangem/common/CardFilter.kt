package com.tangem.common

import com.tangem.common.card.Card
import com.tangem.common.card.FirmwareVersion

/**
 * Filter that can be used to limit cards that can be interacted with in TangemSdk.
 */
class CardFilter(
    /**
     * Filter that can be used to limit cards that can be interacted with in TangemSdk.
     */
    var allowedCardTypes: List<FirmwareVersion.FirmwareType> = listOf(
            FirmwareVersion.FirmwareType.Release,
            FirmwareVersion.FirmwareType.Sdk
    ),

    /**
     * Use this filter to configure batches allowed to work with your app
     */
    var batchIdFilter: ItemFilter? = null,

    /**
     * Use this filter to configure issuers allowed to work with your app
     */
    var issuerFilter: ItemFilter? = null
) {

    fun isCardAllowed(card: Card): Boolean {
        if (!allowedCardTypes.contains(card.firmwareVersion.type)) return false

        batchIdFilter?.let {
            if (it.isAllowed(card.batchId)) return false
        }
        issuerFilter?.let {
            if (it.isAllowed(card.issuer.name)) return false
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
    }
}

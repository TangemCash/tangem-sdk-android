package com.tangem.operations.personalization.entities

import com.tangem.common.BaseMask
import com.tangem.common.Mask

/**
 * Mask of products enabled on card
 * @property rawValue Products mask values
 */
class ProductMask(override val rawValue: Int) : BaseMask() {

    override val values: List<Code> = Code.values().toList()

    enum class Code(override val value: Int) : Mask.Code {
        Note(value = 0x01),
        Tag(value = 0x02),
        IdCard(value = 0x04),
        IdIssuer(value = 0x08),
        Authentication(value = 0x10),
        TwinCard(value = 0x20);
    }
}
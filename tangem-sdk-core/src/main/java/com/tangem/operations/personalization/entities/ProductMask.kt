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
        Note(0x01),
        Tag(0x02),
        IdCard(0x04),
        IdIssuer(0x08),
        Authentication(0x10),
        TwinCard(0x20);
    }
}
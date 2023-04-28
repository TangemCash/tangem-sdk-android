package com.tangem.common.card

import com.tangem.common.BaseMask
import com.tangem.common.Mask

/**
 * Determines which type of data is required for signing.
 */
class SigningMethod(override val rawValue: Int) : BaseMask() {

    override val values: List<Code> = Code.values().toList()

    @Suppress("MagicNumber")
    override fun contains(code: Mask.Code): Boolean {
        return if (rawValue and 0x80 == 0) {
            code.value == rawValue
        } else {
            rawValue and (0x01 shl code.value) != 0
        }
    }

    enum class Code(override val value: Int) : Mask.Code {
        SignHash(value = 0),
        SignRaw(value = 1),
        SignHashSignedByIssuer(value = 2),
        SignRawSignedByIssuer(value = 3),
        SignHashSignedByIssuerAndUpdateIssuerData(value = 4),
        SignRawSignedByIssuerAndUpdateIssuerData(value = 5),
        SignPos(value = 6),
    }

    companion object {
        fun build(vararg methods: Code): SigningMethod {
            return SigningMethodMaskBuilder().apply {
                methods.forEach { this.add(it) }
            }.build()
        }
    }
}

private class SigningMethodMaskBuilder {

    private val signingMethods = mutableSetOf<SigningMethod.Code>()

    fun add(signingMethod: SigningMethod.Code) {
        signingMethods.add(signingMethod)
    }

    @Suppress("MagicNumber")
    fun build(): SigningMethod {
        val rawValue: Int = when {
            signingMethods.isEmpty() -> 0
            signingMethods.count() == 1 -> signingMethods.iterator().next().value
            else -> signingMethods.fold(0x80) { acc, code -> acc + (0x01 shl code.value) }
        }
        return SigningMethod(rawValue)
    }
}

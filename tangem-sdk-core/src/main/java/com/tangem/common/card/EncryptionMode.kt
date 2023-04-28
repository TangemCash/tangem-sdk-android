package com.tangem.common.card

/**
 * All possible encryption modes.
 */
enum class EncryptionMode(val byteValue: Int) {
    None(0x0),
    Fast(0x1),
    Strong(0x2),
}

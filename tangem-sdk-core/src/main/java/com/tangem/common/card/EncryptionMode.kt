package com.tangem.common.card

enum class AesMode(val p1: Int) {
    None(0), CbcFast(0x01), CbcStrong(0x02), Ccm(0x10)
}

/**
 * All possible encryption modes.
 */
enum class EncryptionMode(val byteValue: Int, val aesMode: AesMode) {
    None(0x0, AesMode.None),
    Fast(0x1, AesMode.CbcFast),
    Strong(0x2, AesMode.CbcStrong),
    CcmWithSecurityDelay(0x10, AesMode.Ccm),
    CcmWithAccessToken(0x11, AesMode.Ccm),
    CcmWithAsymmetricKeys(0x12, AesMode.Ccm),
}

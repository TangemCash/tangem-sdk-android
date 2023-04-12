package com.tangem.crypto

import com.google.common.truth.Truth.assertThat
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import com.tangem.crypto.CryptoUtils.generatePublicKey
import com.tangem.crypto.CryptoUtils.generateRandomBytes
import com.tangem.crypto.CryptoUtils.verify
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import java.util.Locale

internal class CryptoUtilsTest {

    init {
        CryptoUtils.initCrypto()
    }

    @Test
    fun generateRandomBytesTest() {
        val privateKey: ByteArray = generateRandomBytes(32)
        assertThat(privateKey)
            .hasLength(32)
        assertThat(privateKey.sum())
            .isNotEqualTo(0)
    }

    @Test
    internal fun verifyEd25519Test() {
        val verified = verifySignature_withSampleData(EllipticCurve.Ed25519)
        assertThat(verified)
            .isTrue()
    }

    @Test
    internal fun verifySecp256k1Test() {
        val verified = verifySignature_withSampleData(EllipticCurve.Secp256k1)
        assertThat(verified)
            .isTrue()
    }

    private fun verifySignature_withSampleData(curve: EllipticCurve): Boolean {
        val privateKey = ByteArray(32) { 1 }
        val publicKey = generatePublicKey(privateKey, curve)
        val message = ByteArray(64) { 5 }
        val signature = message.sign(privateKey, curve)
        return verify(publicKey, message, signature, curve)
    }

    @Test
    fun testKeyCompression() {
        val publicKey = (
            "0432f507f6a3029028faa5913838c50f5ff3355b9b000b51889d03a2bdb96570cd750e8187482a27ca9d2dd0c" +
                "92c632155d0384521ed406753c9883621ad0da68c"
            ).hexToBytes()

        val compressedKey = Secp256k1.compressPublicKey(publicKey)
        assert(
            compressedKey.toHexString()
                .lowercase(Locale.getDefault()) == "0232f507f6a3029028faa5913838c50f5ff3355b9b000b51889d03a2bdb96570cd",
        )
        val decompressedKey = Secp256k1.decompressPublicKey(compressedKey)
        assert(decompressedKey.contentEquals(publicKey))
    }

    @Test
    fun testSecp256k1PrivateKeyValidation() {
        CryptoUtils.initCrypto()
        assertFalse(CryptoUtils.isPrivateKeyValid(ByteArray(0)))
        assertFalse(CryptoUtils.isPrivateKeyValid(ByteArray(32) { 0.toByte() }))
    }

    @Test
    fun testSecp256r1PrivateKeyValidation() {
        assertFalse(CryptoUtils.isPrivateKeyValid(ByteArray(0), EllipticCurve.Secp256r1))
        assertFalse(CryptoUtils.isPrivateKeyValid(ByteArray(32) { 0.toByte() }, EllipticCurve.Secp256r1))
        assertFalse(
            CryptoUtils.isPrivateKeyValid(
                "FFFFFFFFFE92BF972115EB5008573E60811CA5A79B40EAAF9036189360F47413".hexToBytes(),
                EllipticCurve.Secp256r1,
            ),
        )
        assertFalse(
            CryptoUtils.isPrivateKeyValid(
                "FFFFFFFF00000000FFFFFFFFFFFFFFFFBCE6FAADA7179E84F3B9CAC4FC632551".hexToBytes(),
                EllipticCurve.Secp256r1,
            ),
        )
        assertTrue(
            CryptoUtils.isPrivateKeyValid(
                "FFFFFFFF00000000FFFFFFFFFFFFFFFFBCE6FAADA7179E84F3B9CAC2FC632550".hexToBytes(),
                EllipticCurve.Secp256r1,
            ),
        )
    }
}

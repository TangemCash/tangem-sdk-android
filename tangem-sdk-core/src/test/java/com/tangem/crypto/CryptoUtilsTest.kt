package com.tangem.crypto

import com.google.common.truth.Truth.assertThat
import com.tangem.common.card.EllipticCurve
import com.tangem.crypto.CryptoUtils.generatePublicKey
import com.tangem.crypto.CryptoUtils.generateRandomBytes
import com.tangem.crypto.CryptoUtils.verify
import com.tangem.operations.personalization.entities.createCardId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


class CryptoUtilsTest {

    @BeforeEach
    internal fun setUp() {
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
    fun sdfsd() {
        val result = createCardId("BB45", 3000000000004)
        assert(result != null)
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

}
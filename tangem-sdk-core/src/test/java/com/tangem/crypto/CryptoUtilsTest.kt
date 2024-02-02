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

    @Test
    internal fun verifyBip0340Sign() {
        val verified = verifySignature_withSampleData(EllipticCurve.Bip0340)
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

    @Test
    fun testSchnorrVerifyByHash() {
        val pubKey = "208BDB9C192B5DE5DDEBA9CA8500EEC10DECB9A0980C4664F5B168F6B37EB92A".hexToBytes()
        val hash = "0000000000000000000000000000000000000000000000000000000000000000".hexToBytes()
        val signature = (
            "735951D8481B99777AB0ABADEFDA903E485756DE3599E75AF655B7F26CB7634956DEDEB89DB3E40A7B9ED095E5" +
                "855290F8EB85C22E57A001A4A64385AB11A5B3"
            ).hexToBytes()
        val verify = CryptoUtils.verifyHash(
            publicKey = pubKey,
            hash = hash,
            signature = signature,
            curve = EllipticCurve.Bip0340,
        )
        assertTrue(verify)
    }

    @Test
    fun testSchnorrVerifyByMessage() {
        val pubKey = "DFF1D77F2A671C5F36183726DB2341BE58FEAE1DA2DECED843240F7B502BA659".hexToBytes()
        val message = "243F6A8885A308D313198A2E03707344A4093822299F31D0082EFA98EC4E6C89".hexToBytes()
        val signature = (
            "0560D3B34117AAE83F028AB92B3F8C16E3FDA34D55D3C7A2E1F2EDFE0A0071491D8D2302A6810FC017EE4CBF6B" +
                "F13ADF36F9C0967FFCFE1A64BCBBDA73CA813B"
            ).hexToBytes()
        val verify = verify(
            publicKey = pubKey,
            message = message,
            signature = signature,
            curve = EllipticCurve.Bip0340,
        )
        assertTrue(verify)
    }
}

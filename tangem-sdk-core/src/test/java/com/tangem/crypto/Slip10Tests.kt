package com.tangem.crypto

import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import com.tangem.crypto.hdWallet.bip32.BIP32
import org.junit.Test
import kotlin.test.assertEquals

internal class Slip10Tests {

    init {
        CryptoUtils.initCrypto()
    }

    @Test
    fun testSecp256r1MasterKeyGenerationRetry() {
        val masterKey = BIP32.makeMasterKey(
            "a7305bc8df8d0951f0cb224c0e95d7707cbdf2c6ce7e8d481fec69c7ff5e9446".hexToBytes(),
            curve = EllipticCurve.Secp256r1,
        )
        assertEquals(
            masterKey.privateKey.toHexString().lowercase(),
            "3b8c18469a4634517d6d0b65448f8e6c62091b45540a1743c5846be55d47d88f".lowercase(),
        )
        assertEquals(
            masterKey.chainCode.toHexString().lowercase(),
            "7762f9729fed06121fd13f326884c82f59aa95c57ac492ce8c9654e60efd130c".lowercase(),
        )
    }

    @Test
    fun testVector1Secp256r1() {
        val seed = "000102030405060708090a0b0c0d0e0f".hexToBytes()

        val masterKey = BIP32.makeMasterKey(seed, curve = EllipticCurve.Secp256r1)
        val masterKeyPublic = Secp256r1.generatePublicKey(masterKey.privateKey, compressed = true)

        assertEquals(
            masterKey.privateKey.toHexString().lowercase(),
            "612091aaa12e22dd2abef664f8a01a82cae99ad7441b7ef8110424915c268bc2",
        )
        assertEquals(
            masterKey.chainCode.toHexString().lowercase(),
            "beeb672fe4621673f722f38529c07392fecaa61015c80c34f29ce8b41b3cb6ea",
        )
        assertEquals(
            masterKeyPublic.toHexString().lowercase(),
            "0266874dc6ade47b3ecd096745ca09bcd29638dd52c2c12117b11ed3e458cfa9e8",
        )
    }

    @Test
    fun testVector2Secp256r1() {
        val seed = (
            "fffcf9f6f3f0edeae7e4e1dedbd8d5d2cfccc9c6c3c0bdbab7b4b1aeaba8a5a29f9c999693908d8a8784817e7b78757" +
                "26f6c696663605d5a5754514e4b484542"
            ).hexToBytes()

        val masterKey = BIP32.makeMasterKey(seed, curve = EllipticCurve.Secp256r1)
        val masterKeyPublic = Secp256r1.generatePublicKey(masterKey.privateKey, compressed = true)

        assertEquals(
            masterKey.privateKey.toHexString().lowercase(),
            "eaa31c2e46ca2962227cf21d73a7ef0ce8b31c756897521eb6c7b39796633357",
        )
        assertEquals(
            masterKey.chainCode.toHexString().lowercase(),
            "96cd4465a9644e31528eda3592aa35eb39a9527769ce1855beafc1b81055e75d",
        )
        assertEquals(
            masterKeyPublic.toHexString().lowercase(),
            "02c9e16154474b3ed5b38218bb0463e008f89ee03e62d22fdcc8014beab25b48fa",
        )
    }

    @Test
    fun testSecp256r1DerivationRetry() {
        val masterKey = BIP32.makeMasterKey(
            "000102030405060708090a0b0c0d0e0f".hexToBytes(),
            curve = EllipticCurve.Secp256r1,
        )

        assertEquals(
            masterKey.privateKey.toHexString().lowercase(),
            "612091aaa12e22dd2abef664f8a01a82cae99ad7441b7ef8110424915c268bc2",
        )
        assertEquals(
            masterKey.chainCode.toHexString().lowercase(),
            "beeb672fe4621673f722f38529c07392fecaa61015c80c34f29ce8b41b3cb6ea",
        )
    }

    @Test
    fun testVector1Ed5519Slip0010() {
        val seed = "000102030405060708090a0b0c0d0e0f".hexToBytes()

        val masterKey = BIP32.makeMasterKey(seed, curve = EllipticCurve.Ed25519Slip0010)

        assertEquals(
            masterKey.privateKey.toHexString(),
            "2b4be7f19ee27bbf30c667b642d5f4aa69fd169872f8fc3059c08ebae2eb19e7".uppercase(),
        )
        assertEquals(
            masterKey.chainCode.toHexString(),
            "90046a93de5380a72b5e45010748567d5ea02bbf6522f979e05c0d8d8ca9fffb".uppercase(),
        )
        assertEquals(
            masterKey.makePublicKey(EllipticCurve.Ed25519Slip0010).publicKey.toHexString(),
            "00a4b2856bfec510abab89753fac1ac0e1112364e7d250545963f135f2a33188ed".drop(2).uppercase(),
        )
    }

    @Test
    fun testVector2Ed5519Slip0010() {
        val seed = (
            "fffcf9f6f3f0edeae7e4e1dedbd8d5d2cfccc9c6c3c0bdbab7b4b1aeaba8a5a29f9c999693908d8a8784817e7b78757" +
                "26f6c696663605d5a5754514e4b484542"
            ).hexToBytes()

        val masterKey = BIP32.makeMasterKey(seed, curve = EllipticCurve.Ed25519Slip0010)

        assertEquals(
            masterKey.privateKey.toHexString(),
            "171cb88b1b3c1db25add599712e36245d75bc65a1a5c9e18d76f9f2b1eab4012".uppercase(),
        )
        assertEquals(
            masterKey.chainCode.toHexString(),
            "ef70a74db9c3a5af931b5fe73ed8e1a53464133654fd55e7a66f8570b8e33c3b".uppercase(),
        )
        assertEquals(
            masterKey.makePublicKey(EllipticCurve.Ed25519Slip0010).publicKey.toHexString(),
            "008fe9693f8fa62a4305a140b9764c5ee01e455963744fe18204b4fb948249308a".drop(2)
                .uppercase(),
        )
    }
}

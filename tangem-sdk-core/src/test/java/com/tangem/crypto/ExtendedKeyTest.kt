package com.tangem.crypto

import com.tangem.common.card.EllipticCurve
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.calculateRipemd160
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import com.tangem.common.successOrNull
import com.tangem.crypto.bip39.BIP39Wordlist
import com.tangem.crypto.bip39.BIP39WordlistTest
import com.tangem.crypto.bip39.DefaultMnemonic
import com.tangem.crypto.bip39.Wordlist
import com.tangem.crypto.hdWallet.DerivationNode
import com.tangem.crypto.hdWallet.bip32.ExtendedPrivateKey
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import java.io.InputStream
import kotlin.test.assertContentEquals

internal class ExtendedKeyTest {

    init {
        CryptoUtils.initCrypto()
    }

    @Test
    fun testRoundTripPub() {
        val key = ExtendedPublicKey(
            publicKey = "0339a36013301597daef41fbe593a02cc513d0b55527ec2df1050e2e8ff49c85c2".hexToBytes(),
            chainCode = "873dff81c02f525623fd1fe5167eac3a55a049de3d314bb42ee227ffed37d508".hexToBytes(),
            depth = 3,
            parentFingerprint = "00000000".hexToBytes(),
            childNumber = 2147483648L,
        )

        val xpubString = key.serialize(NetworkType.Mainnet)
        val deserializedKey = ExtendedPublicKey.from(xpubString, NetworkType.Mainnet)

        assertEquals(key, deserializedKey)
    }

    @Test
    fun testRoundTripPriv() {
        val xpriv = "xprv9s21ZrQH143K3Dp5U6YoTum8c6rvMLxbEncwSjfnq12ShNzEhwbCmfvQDPNQTCsEcZJZcLrnf6rt6MCzsMiJYrhLGQw" +
            "kK1uPCC5QsiAu4tW"
        val key = ExtendedPrivateKey.from(xpriv, NetworkType.Mainnet)
        val serialized = key.serialize(NetworkType.Mainnet)
        assertEquals(xpriv, serialized)
    }

    @Test
    fun testDerived() {
        val parentKey = "0339a36013301597daef41fbe593a02cc513d0b55527ec2df1050e2e8ff49c85c2".hexToBytes()
        val parentFingerprint = parentKey.calculateSha256().calculateRipemd160().take(4).toByteArray()

        val key = ExtendedPublicKey(
            publicKey = "0339a36013301597daef41fbe593a02cc513d0b55527ec2df1050e2e8ff49c85c2".hexToBytes(),
            chainCode = "873dff81c02f525623fd1fe5167eac3a55a049de3d314bb42ee227ffed37d508".hexToBytes(),
        )

        val derivedKey = key.derivePublicKey(node = DerivationNode.NonHardened(2))

        assertContentEquals(derivedKey.parentFingerprint, parentFingerprint)
        assertEquals(derivedKey.depth, 1)
        assertEquals(derivedKey.childNumber, 2L)
    }

    @Test
    fun testInitMaster() {
        val key = ExtendedPublicKey(
            publicKey = "0339a36013301597daef41fbe593a02cc513d0b55527ec2df1050e2e8ff49c85c2".hexToBytes(),
            chainCode = "873dff81c02f525623fd1fe5167eac3a55a049de3d314bb42ee227ffed37d508".hexToBytes(),
        )

        assertEquals(key.depth, 0)
        assertContentEquals(key.parentFingerprint, "00000000".hexToBytes())
        assertEquals(key.childNumber, 0L)
    }

    @Test
    fun testSerializeEdKey() {
        val key = ExtendedPublicKey(
            publicKey = "9FE5BB2CC7D83C1DA10845AFD8A34B141FD8FD72500B95B1547E12B9BB8AAC3D".hexToBytes(),
            chainCode = "02fc9e5af0ac8d9b3cecfe2a888e2117ba3d089d8585886c9c826b6b22a98d12ea".hexToBytes(),
        )
        assertThrows<TangemSdkError.UnsupportedCurve> { key.serialize(NetworkType.Mainnet) }
    }

    @Test
    fun testSerialization() {
        val mKeyString = "xpub661MyMwAqRbcFtXgS5sYJABqqG9YLmC4Q1Rdap9gSE8NqtwybGhePY2gZ29ESFjqJoCu1Rupje8YtGqsefD265" +
            "TMg7usUDFdp6W1EGMcet8"
        val mXpubKey = ExtendedPublicKey.from(mKeyString, NetworkType.Mainnet)

        val key = ExtendedPublicKey(
            publicKey = "035a784662a4a20a65bf6aab9ae98a6c068a81c52e4b032c0fb5400c706cfccc56".hexToBytes(),
            chainCode = "47fdacbd0f1097043b78c63c20c34ef4ed9a111d980047ad16282c7ae6236141".hexToBytes(),
            depth = 1,
            parentFingerprint = mXpubKey.publicKey.calculateSha256().calculateRipemd160().take(4).toByteArray(),
            childNumber = 2147483648L,
        )

        val serialized = key.serialize(NetworkType.Mainnet)
        assertEquals(
            serialized,
            "xpub68Gmy5EdvgibQVfPdqkBBCHxA5htiqg55crXYuXoQRKfDBFA1WEjWgP6LHhwBZeNK1VTsfTFUHCdrfp1bgwQ9xv5ski8" +
                "PX9rL2dZXvgGDnw",
        )
    }

    @Test
    fun testMetaMaskTwCompatible() {
        val mnemonicPhrase = "scale wave venue cloth fruit empower afford one domain blouse romance artist"
        val mnemonic = DefaultMnemonic(mnemonicPhrase, createDefaultWordlist())
        val seed = mnemonic.generateSeed().successOrNull()!!
        assertEquals(
            seed.toHexString().lowercase(),
            "d3eea633215dc4cb8ec2acd0d413adec1ebccb597ecf279886e584e9cb9ceb0788eb6f17a585acc12bc58fd586df6bbb" +
                "df39af955656f24215cceab174344e62",
        )

        val extendedPrivateKey = com.tangem.crypto.hdWallet.bip32.BIP32.makeMasterKey(seed, EllipticCurve.Secp256k1)

        val pk = extendedPrivateKey.privateKey.toHexString().lowercase()
        assertEquals(pk, "589aeb596710f33d7ac31598ec10440a7df8808cf2c3d69ba670ff3fae66aafb")

        assertEquals(
            extendedPrivateKey.serializeToWIFCompressed(NetworkType.Mainnet),
            "KzBwvPW6L5iwJSiE5vgS52Y69bUxfwizW3wF4C4Xa3ba3pdd7j63",
        )
    }

    private fun createDefaultWordlist(): Wordlist {
        val wordlistStream = getInputStreamForTestFile()
        return BIP39Wordlist(wordlistStream)
    }

    private fun getInputStreamForTestFile(): InputStream {
        return object {}.javaClass.classLoader.getResourceAsStream(BIP39WordlistTest.TEST_DICTIONARY_FILE_NAME)!!
    }
}

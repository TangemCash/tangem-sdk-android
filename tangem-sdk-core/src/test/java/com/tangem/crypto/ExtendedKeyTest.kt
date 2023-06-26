package com.tangem.crypto

import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.calculateRipemd160
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.hexToBytes
import com.tangem.crypto.hdWallet.DerivationNode
import com.tangem.crypto.hdWallet.bip32.ExtendedPrivateKey
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

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

    // https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki#user-content-Test_Vectors
    @Suppress("LongMethod")
    @Test
    fun testBadKeys() {
        // (invalid pubkey 020000000000000000000000000000000000000000000000000000000000000007)
        assertFailsWith<Exception> {
            ExtendedPublicKey.from(
                "xpub661MyMwAqRbcEYS8w7XLSVeEsBXy79zSzH1J8vCdxAZningWLdN3zgtU6Q5JXayek4PRsn35jii4ve" +
                    "Mimro1xefsM58PgBMrvdYre8QyULY",
                NetworkType.Mainnet,
            )
        }

        // (unknown extended key version)
        assertFailsWith<Exception> {
            ExtendedPublicKey.from(
                "DMwo58pR1QLEFihHiXPVykYB6fJmsTeHvyTp7hRThAtCX8CvYzgPcn8XnmdfHPmHJiEDXkTiJTVV9rHEBU" +
                    "em2mwVbbNfvT2MTcAqj3nesx8uBf9",
                NetworkType.Mainnet,
            )
        }

        // (unknown extended key version)
        assertFailsWith<Exception> {
            ExtendedPrivateKey.from(
                "DMwo58pR1QLEFihHiXPVykYB6fJmsTeHvyTp7hRThAtCX8CvYzgPcn8XnmdfHGMQzT7ayAmfo4z3gY5Kfb" +
                    "rZWZ6St24UVf2Qgo6oujFktLHdHY4",
                NetworkType.Mainnet,
            )
        }

        // (zero depth with non-zero index)
        assertFailsWith<Exception> {
            ExtendedPublicKey.from(
                "xpub661MyMwAuDcm6CRQ5N4qiHKrJ39Xe1R1NyfouMKTTWcguwVcfrZJaNvhpebzGerh7gucBvzEQWRugZ" +
                    "DuDXjNDRmXzSZe4c7mnTK97pTvGS8",
                NetworkType.Mainnet,
            )
        }

        // (zero depth with non-zero parent fingerprint)
        assertFailsWith<Exception> {
            ExtendedPublicKey.from(
                "xpub661no6RGEX3uJkY4bNnPcw4URcQTrSibUZ4NqJEw5eBkv7ovTwgiT91XX27VbEXGENhYRCf7hyEbWr" +
                    "R3FewATdCEebj6znwMfQkhRYHRLpJ",
                NetworkType.Mainnet,
            )
        }

        // (pubkey version / prvkey mismatch)
        assertFailsWith<Exception> {
            ExtendedPublicKey.from(
                "xpub661MyMwAqRbcEYS8w7XLSVeEsBXy79zSzH1J8vCdxAZningWLdN3zgtU6LBpB85b3D2yc8sfvZU521" +
                    "AAwdZafEz7mnzBBsz4wKY5fTtTQBm",
                NetworkType.Mainnet,
            )
        }

        // (prvkey version / pubkey mismatch)
        assertFailsWith<Exception> {
            ExtendedPrivateKey.from(
                "xprv9s21ZrQH143K24Mfq5zL5MhWK9hUhhGbd45hLXo2Pq2oqzMMo63oStZzFGTQQD3dC4H2D5GBj7vWvS" +
                    "QaaBv5cxi9gafk7NF3pnBju6dwKvH",
                NetworkType.Mainnet,
            )
        }

        // (invalid pubkey prefix 04)
        assertFailsWith<Exception> {
            ExtendedPublicKey.from(
                "xpub661MyMwAqRbcEYS8w7XLSVeEsBXy79zSzH1J8vCdxAZningWLdN3zgtU6Txnt3siSujt9RCVYsx4qH" +
                    "ZGc62TG4McvMGcAUjeuwZdduYEvFn",
                NetworkType.Mainnet,
            )
        }

        // (invalid prvkey prefix 04)
        assertFailsWith<Exception> {
            ExtendedPrivateKey.from(
                "xprv9s21ZrQH143K24Mfq5zL5MhWK9hUhhGbd45hLXo2Pq2oqzMMo63oStZzFGpWnsj83BHtEy5Zt8CcDr" +
                    "1UiRXuWCmTQLxEK9vbz5gPstX92JQ",
                NetworkType.Mainnet,
            )
        }

        // (invalid pubkey prefix 01)
        assertFailsWith<Exception> {
            ExtendedPublicKey.from(
                "xpub661MyMwAqRbcEYS8w7XLSVeEsBXy79zSzH1J8vCdxAZningWLdN3zgtU6N8ZMMXctdiCjxTNq964yK" +
                    "kwrkBJJwpzZS4HS2fxvyYUA4q2Xe4",
                NetworkType.Mainnet,
            )
        }

        // (invalid prvkey prefix 01)
        assertFailsWith<Exception> {
            ExtendedPrivateKey.from(
                "xprv9s21ZrQH143K24Mfq5zL5MhWK9hUhhGbd45hLXo2Pq2oqzMMo63oStZzFAzHGBP2UuGCqWLTAPLcMt" +
                    "D9y5gkZ6Eq3Rjuahrv17fEQ3Qen6J",
                NetworkType.Mainnet,
            )
        }

        // (zero depth with non-zero parent fingerprint)
        assertFailsWith<Exception> {
            ExtendedPrivateKey.from(
                "xprv9s2SPatNQ9Vc6GTbVMFPFo7jsaZySyzk7L8n2uqKXJen3KUmvQNTuLh3fhZMBoG3G4ZW1N2kZuHEPY" +
                    "53qmbZzCHshoQnNf4GvELZfqTUrcv",
                NetworkType.Mainnet,
            )
        }

        // (zero depth with non-zero index)
        assertFailsWith<Exception> {
            ExtendedPrivateKey.from(
                "xprv9s21ZrQH4r4TsiLvyLXqM9P7k1K3EYhA1kkD6xuquB5i39AU8KF42acDyL3qsDbU9NmZn6MsGSUYZE" +
                    "suoePmjzsB3eFKSUEh3Gu1N3cqVUN",
                NetworkType.Mainnet,
            )
        }

        // (private key 0 not in 1..n-1)
        assertFailsWith<Exception> {
            ExtendedPrivateKey.from(
                "xprv9s21ZrQH143K24Mfq5zL5MhWK9hUhhGbd45hLXo2Pq2oqzMMo63oStZzF93Y5wvzdUayhgkkFoicQZ" +
                    "cP3y52uPPxFnfoLZB21Teqt1VvEHx",
                NetworkType.Mainnet,
            )
        }

        // (private key n not in 1..n-1)
        assertFailsWith<Exception> {
            ExtendedPrivateKey.from(
                "xprv9s21ZrQH143K24Mfq5zL5MhWK9hUhhGbd45hLXo2Pq2oqzMMo63oStZzFAzHGBP2UuGCqWLTAPLcMt" +
                    "D5SDKr24z3aiUvKr9bJpdrcLg1y3G",
                NetworkType.Mainnet,
            )
        }

        // (invalid checksum)
        assertFailsWith<Exception> {
            ExtendedPrivateKey.from(
                "xprv9s21ZrQH143K3QTDL4LXw2F7HEK3wJUD2nW2nRk4stbPy6cq3jPPqjiChkVvvNKmPGJxWUtg6LnF5k" +
                    "ejMRNNU3TGtRBeJgk33yuGBxrMPHL",
                NetworkType.Mainnet,
            )
        }
    }
}

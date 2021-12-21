package com.tangem

import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toByteArray
import com.tangem.common.extensions.toHexString
import com.tangem.common.extensions.toLong
import com.tangem.common.hdWallet.DerivationNode
import com.tangem.common.hdWallet.DerivationPath
import com.tangem.common.hdWallet.ExtendedPublicKey
import com.tangem.common.hdWallet.HDWalletError
import com.tangem.common.hdWallet.bip.BIP44
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.common.tlv.TlvEncoder
import com.tangem.common.tlv.TlvTag
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class HDWalletTests {

    @Test
    fun indexSerialization() {
        fun compare(toCompare: Long, shouldBeEqual: Boolean = true) {
            val fromStandard = toCompare.toByteArray().toLong()
            val fromHalf = toCompare.toByteArray(4).toLong()

            if (shouldBeEqual) assertEquals(fromStandard, fromHalf) else assertNotEquals(fromStandard, fromHalf)
        }

        compare(100)
        compare(Int.MAX_VALUE.toLong() + 100L)
        compare(Long.MAX_VALUE.toByteArray(4).toLong())
        compare(Long.MAX_VALUE, false)
        compare(-100500, false)
    }

    @Test
    fun testDerivation1() {
        val masterKey = ExtendedPublicKey(
            "0339a36013301597daef41fbe593a02cc513d0b55527ec2df1050e2e8ff49c85c2".hexToBytes(),
            "873dff81c02f525623fd1fe5167eac3a55a049de3d314bb42ee227ffed37d508".hexToBytes(),
            DerivationPath()
        )

        val derived = masterKey.derivePublicKey(DerivationNode.NonHardened(1))
        val key = derived.compressedPublicKey.toHexString().toLowerCase()
        val chainCode = derived.chainCode.toHexString().toLowerCase()
        assertEquals(key, "037c2098fd2235660734667ff8821dbbe0e6592d43cfd86b5dde9ea7c839b93a50")
        assertEquals(chainCode, "8dd96414ff4d5b4750be3af7fecce207173f86d6b5f58f9366297180de8e109b")
    }

    @Test
    fun testDerivation0() {
        val masterKey = ExtendedPublicKey(
            "03cbcaa9c98c877a26977d00825c956a238e8dddfbd322cce4f74b0b5bd6ace4a7".hexToBytes(),
            "60499f801b896d83179a4374aeb7822aaeaceaa0db1f85ee3e904c4defbd9689".hexToBytes(),
            DerivationPath()
        )

        val derived = masterKey.derivePublicKey(DerivationNode.NonHardened(0))
        val key = derived.compressedPublicKey.toHexString().toLowerCase()
        val chainCode = derived.chainCode.toHexString().toLowerCase()
        assertEquals(key, "02fc9e5af0ac8d9b3cecfe2a888e2117ba3d089d8585886c9c826b6b22a98d12ea")
        assertEquals(chainCode, "f0909affaa7ee7abe5dd4e100598d4dc53cd709d5a5c2cac40e7412f232f7c9c")
    }

    @Test
    fun testParsePath() {
        val derivationPath = DerivationPath("m / 44' / 0' / 0' / 1 / 0")
        val derivationPath1 = DerivationPath("m/44'/0'/0'/1/0")
        val derivationPath2 = DerivationPath("M/44'/0'/0'/1/0")
        val derivationPath3 = DerivationPath(listOf(
            DerivationNode.Hardened(44),
            DerivationNode.Hardened(0),
            DerivationNode.Hardened(0),
            DerivationNode.NonHardened(1),
            DerivationNode.NonHardened(0)
        ))
        assertEquals(derivationPath, derivationPath1)
        assertEquals(derivationPath, derivationPath2)
        assertEquals(derivationPath, derivationPath3)

        assertEquals(derivationPath.nodes[0], DerivationNode.Hardened(44))
        assertEquals(derivationPath.nodes[1], DerivationNode.Hardened(0))
        assertEquals(derivationPath.nodes[2], DerivationNode.Hardened(0))
        assertEquals(derivationPath.nodes[3], DerivationNode.NonHardened(1))
        assertEquals(derivationPath.nodes[4], DerivationNode.NonHardened(0))

        assertThrows<HDWalletError> { DerivationPath("44'/m'/0'/1/0") }
        assertThrows<HDWalletError> { DerivationPath("m /") }
        assertThrows<HDWalletError> { DerivationPath("m|44'|0'|0'|1|0") }
    }

    @Test
    fun testTlvSerialization() {
        fun encodeToHexString(derivationPath: DerivationPath): String {
            return TlvEncoder().encode(TlvTag.WalletHDPath, derivationPath).value.toHexString()
        }
        assertEquals("0000000000000001", encodeToHexString(DerivationPath("m/0/1")))
        assertEquals("800000008000000100000002", encodeToHexString(DerivationPath("m/0'/1'/2")))
    }

    @Test
    fun testTlvDeserialization() {
        fun decodeRawPathFromHex(hexValue: String): String? = try {
            DerivationPath.from(hexValue.hexToBytes()).rawPath
        } catch (ex: Exception) {
            null
        }
        assertEquals("m/0/1", decodeRawPathFromHex("0000000000000001"))
        assertEquals("m/0'/1'/2", decodeRawPathFromHex("800000008000000100000002"))
        assertEquals(null, decodeRawPathFromHex("000000000000000100"))
    }

    @Test
    fun testBitcoinBip44() {
        val builder = BIP44(0, 0, BIP44.Chain.External, 0)
        val path = builder.buildPath().rawPath
        assertEquals(path, "m/44'/0'/0'/0/0")
    }

    @Test
    fun testPathDerivation() {
        val path = DerivationPath("m/0")
        val masterKey = ExtendedPublicKey(
            "03cbcaa9c98c877a26977d00825c956a238e8dddfbd322cce4f74b0b5bd6ace4a7".hexToBytes(),
            "60499f801b896d83179a4374aeb7822aaeaceaa0db1f85ee3e904c4defbd9689".hexToBytes(),
            DerivationPath()
        )

        val childKey = masterKey.derivePublicKey(path)
        val code = childKey.chainCode.toHexString().toLowerCase()
        val key = childKey.compressedPublicKey.toHexString().toLowerCase()
        assertEquals(code, "f0909affaa7ee7abe5dd4e100598d4dc53cd709d5a5c2cac40e7412f232f7c9c")
        assertEquals(key, "02fc9e5af0ac8d9b3cecfe2a888e2117ba3d089d8585886c9c826b6b22a98d12ea")
    }

    @Test
    fun testPathDerivation1() {
        //xpub661MyMwAqRbcFW31YEwpkMuc5THy2PSt5bDMsktWQcFF8syAmRUapSCGu8ED9W6oDMSgv6Zz8doc4a6mr8BDzTJY47LJhkJ8UB7WEGuduB
        val path = DerivationPath("m/0/1")
        val masterKey = ExtendedPublicKey(
            "03cbcaa9c98c877a26977d00825c956a238e8dddfbd322cce4f74b0b5bd6ace4a7".hexToBytes(),
            "60499f801b896d83179a4374aeb7822aaeaceaa0db1f85ee3e904c4defbd9689".hexToBytes(),
            DerivationPath()
        )

        val childKey = masterKey.derivePublicKey(path)
        val code = childKey.chainCode.toHexString().toLowerCase()
        val key = childKey.compressedPublicKey.toHexString().toLowerCase()
        assertEquals(code, "8d5e25bfe038e4ef37e2c5ec963b7a7c7a745b4319bff873fc40f1a52c7d6fd1")
        assertEquals(key, "02d27a781fd1b3ec5ba5017ca55b9b900fde598459a0204597b37e6c66a0e35c98")
    }

    @Test
    fun testPathDerivationFailed() {
        val path = BIP44(0, 0, BIP44.Chain.External, 0).buildPath()
        val masterKey = ExtendedPublicKey(
            "03cbcaa9c98c877a26977d00825c956a238e8dddfbd322cce4f74b0b5bd6ace4a7".hexToBytes(),
            "60499f801b896d83179a4374aeb7822aaeaceaa0db1f85ee3e904c4defbd9689".hexToBytes(),
            DerivationPath()
        )
        assertThrows<HDWalletError.HardenedNotSupported> { masterKey.derivePublicKey(path) }
    }

    @Test
    fun testPathExtension() {
        val derivationPath = DerivationPath("m/44'/0'/0'/1")
        val extendedPath = derivationPath.extendedPath(DerivationNode.NonHardened(0))
        assertEquals(extendedPath.rawPath, "m/44'/0'/0'/1/0")
    }

    @Test
    fun jsonDerivationNode() {
        val converter = MoshiJsonConverter.INSTANCE
        val derivationPath = DerivationPath("m/44'/0'/0'/1")
        var json = converter.toJson(derivationPath)
        val resultDerivationPath = converter.fromJson<DerivationPath>(json)
        assertNotNull(resultDerivationPath)
        assertEquals(derivationPath, resultDerivationPath)

        val node = derivationPath.nodes[0]
        json = converter.toJson(node)
        val resultNode = converter.fromJson<DerivationNode>(json)
        assertNotNull(resultNode)
        assertEquals(node, resultNode)
    }
}
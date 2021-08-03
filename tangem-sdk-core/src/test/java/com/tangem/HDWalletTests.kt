package com.tangem

import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import com.tangem.common.hdWallet.DerivationNode
import com.tangem.common.hdWallet.DerivationPath
import com.tangem.common.hdWallet.ExtendedPublicKey
import org.junit.Test

class HDWalletTests {

    @Test
    fun testDerivation1() {
        val masterKey = ExtendedPublicKey(
                "0339a36013301597daef41fbe593a02cc513d0b55527ec2df1050e2e8ff49c85c2".hexToBytes(),
                "873dff81c02f525623fd1fe5167eac3a55a049de3d314bb42ee227ffed37d508".hexToBytes()
        )

        val derived = masterKey.derivePublicKey(1)
        val key = derived.compressedPublicKey.toHexString().toLowerCase()
        val chainCode = derived.chainCode.toHexString().toLowerCase()
        assert(key == "037c2098fd2235660734667ff8821dbbe0e6592d43cfd86b5dde9ea7c839b93a50")
        assert(chainCode == "8dd96414ff4d5b4750be3af7fecce207173f86d6b5f58f9366297180de8e109b")
    }

    @Test
    fun testDerivation0() {
        val masterKey = ExtendedPublicKey(
                "03cbcaa9c98c877a26977d00825c956a238e8dddfbd322cce4f74b0b5bd6ace4a7".hexToBytes(),
                "60499f801b896d83179a4374aeb7822aaeaceaa0db1f85ee3e904c4defbd9689".hexToBytes()
        )

        val derived = masterKey.derivePublicKey(0)
        val key = derived.compressedPublicKey.toHexString().toLowerCase()
        val chainCode = derived.chainCode.toHexString().toLowerCase()
        assert(key == "02fc9e5af0ac8d9b3cecfe2a888e2117ba3d089d8585886c9c826b6b22a98d12ea")
        assert(chainCode == "f0909affaa7ee7abe5dd4e100598d4dc53cd709d5a5c2cac40e7412f232f7c9c")
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
                DerivationNode.NotHardened(1),
                DerivationNode.NotHardened(0)
        ))
        assert(derivationPath == derivationPath1)
        assert(derivationPath == derivationPath2)
        assert(derivationPath == derivationPath3)

        assert(derivationPath.path[0] == DerivationNode.Hardened(44))
        assert(derivationPath.path[1] == DerivationNode.Hardened(0))
        assert(derivationPath.path[2] == DerivationNode.Hardened(0))
        assert(derivationPath.path[3] == DerivationNode.NotHardened(1))
        assert(derivationPath.path[4] == DerivationNode.NotHardened(0))

        assert(derivationPathIsThrowException("44'/m'/0'/1/0"))
        assert(derivationPathIsThrowException("m /"))
        assert(derivationPathIsThrowException("m|44'|0'|0'|1|0"))
    }

    private fun derivationPathIsThrowException(rawPath: String): Boolean {
        try {
            DerivationPath(rawPath)
        } catch (ex: Exception) {
            return true
        }
        return false
    }
}
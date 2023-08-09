package com.tangem.crypto

import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import com.tangem.common.successOrNull
import com.tangem.crypto.bip39.BIP39Wordlist
import com.tangem.crypto.bip39.BIP39WordlistTest
import com.tangem.crypto.bip39.DefaultMnemonic
import com.tangem.crypto.bip39.Wordlist
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.crypto.hdWallet.masterkey.Bip32MasterKeyFactory
import com.tangem.crypto.hdWallet.masterkey.Eip2333MasterKeyFactory
import com.tangem.crypto.hdWallet.masterkey.IkarusMasterKeyFactory
import org.junit.Test
import java.io.InputStream
import kotlin.test.assertEquals

internal class KeysImportTests {

    init {
        CryptoUtils.initCrypto()
    }

    private val entropy = "6610b25967cdcca9d59875f5cb50b0ea75433311869e930b".hexToBytes()
    private val mnemonicString = "gravity machine north sort system female filter attitude volume fold club stay " +
        "feature office ecology stable narrow fog"
    private val passphrase = "TREZOR"
    private val mnemonic = DefaultMnemonic(mnemonicString, createDefaultWordlist())
    private val seed = mnemonic.generateSeed(passphrase).successOrNull()!!

    @Test
    fun testKeyImportSecp256k1() {
        val prvKey = Bip32MasterKeyFactory(seed, EllipticCurve.Secp256k1).makePrivateKey()
        val pubKey = prvKey.makePublicKey(EllipticCurve.Secp256k1)

        // validate with WalletCore
        assertEquals(
            prvKey.privateKey.toHexString(),
            "0BF8F75E9EB03C1FE723DA7E30CAE8D267A9ADF4091DC8140868CBBF16F650DF",
        )
        // validate with card and WalletCore
        assertEquals(
            pubKey.publicKey.toHexString(),
            "030B0DE47DF425C4BA08D77F927477195FB65E1B238E2366AA1B1BB82E0ACEE1A6",
        )
        assertEquals(pubKey.chainCode.toHexString(), "B975E8D7517ED618CBBEBE87555E415874438B670C9E54E57F70FF02A15C4C10")
    }

    @Test
    fun testKeyImportSchnorr() {
        val prvKey = Bip32MasterKeyFactory(seed, EllipticCurve.Bip0340).makePrivateKey()
        val pubKey = prvKey.makePublicKey(EllipticCurve.Bip0340)

        // validate with secp256k1
        val publicKeyFromSecp256k1 = Secp256k1.generatePublicKey(prvKey.privateKey, compressed = true).drop(1)
            .toByteArray()
        assertEquals(pubKey.publicKey.toHexString(), publicKeyFromSecp256k1.toHexString())

        // validate with card
        assertEquals(
            pubKey.publicKey.toHexString(),
            "0B0DE47DF425C4BA08D77F927477195FB65E1B238E2366AA1B1BB82E0ACEE1A6",
        )
        assertEquals(
            pubKey.chainCode.toHexString(),
            "B975E8D7517ED618CBBEBE87555E415874438B670C9E54E57F70FF02A15C4C10",
        )
    }

    @Test
    fun testKeyImportSecp256r1() {
        val prvKey = Bip32MasterKeyFactory(seed, EllipticCurve.Secp256r1).makePrivateKey()
        val pubKey = Secp256r1.generatePublicKey(prvKey.privateKey, compressed = true)

        // validate with WalletCore
        assertEquals(
            prvKey.privateKey.toHexString(),
            "9ED5DEBE7F5E6430171509A96D68E9966B01AE517D01E49614B1460673788365",
        )

        // validate with card and WalletCore
        assertEquals(pubKey.toHexString(), "03D195B795DB30CB1CE7F13F29E1D7E072DA26ED79AB1D0E9F7C999C88A1C800C2")
        assertEquals(prvKey.chainCode.toHexString(), "DB214B56C922478EA482F7AF007DB85FDCC8FB1AE03707A21371770EE7D5700F")
    }

    @Test
    fun testKeyImportEd25519Slip0010() {
        val prvKey = Bip32MasterKeyFactory(seed, EllipticCurve.Ed25519Slip0010).makePrivateKey()
        val pubKey = prvKey.makePublicKey(EllipticCurve.Ed25519Slip0010)

        // validate with WalletCore
        assertEquals(
            prvKey.privateKey.toHexString(),
            "0CD28B28383FAF7FDDBE79E34919BCB9FCDA3F505CC3360C2DEADF01C88412FF",
        )

        // validate with card and WalletCore
        assertEquals(pubKey.publicKey.toHexString(), "335215FCF3105D6A379B8A0372A9E92B42CEED0B2D4E0D7E78E80D16DF41EA6B")
        assertEquals(pubKey.chainCode.toHexString(), "837FFAF1B96CA1ACD4A3CB9E08398DC1F21EC657A3BE7679435FC55F9FAE4A46")
    }

    @Test
    fun testKeyImportEd25519() {
        // / TrustWallet ignores passphrase https://github.com/trustwallet/wallet-core/blob/master/src/HDWallet.cpp
        val prvKey = IkarusMasterKeyFactory(entropy, passphrase = "").makePrivateKey()

        // validate with WalletCore
        assertEquals(
            prvKey.privateKey.toHexString().lowercase(),
            "58b41cb27297be1fbf192a65e526179f43b779a383f5d72f14e5db8a82bd77525f65dbfe80724cd61254ec14b351312b63b51c87238ebd3c880a6ad158a161cb",
        )
        assertEquals(
            prvKey.chainCode.toHexString().lowercase(),
            "4c3bd3e0df9ea6371678ccf2b741a762825783b8746e2527d8c15749e64b9d60",
        )

        // from TrustWallet's WalletCore
        val pubKey = ExtendedPublicKey(
            publicKey = "8d1dbcbe742b3db49533a3ee1166e9b69348fe200a2369443973b826e65b6a61".hexToBytes(),
            chainCode = "4c3bd3e0df9ea6371678ccf2b741a762825783b8746e2527d8c15749e64b9d60".hexToBytes(),
        )

        // validate with card
        assertEquals(pubKey.publicKey.toHexString(), "8D1DBCBE742B3DB49533A3EE1166E9B69348FE200A2369443973B826E65B6A61")
        assertEquals(pubKey.chainCode.toHexString(), "4C3BD3E0DF9EA6371678CCF2B741A762825783B8746E2527D8C15749E64B9D60")
    }

    // / All BLS schemes tested to produce same public key.
    @Test
    fun testKeyImportBLS() {
        val prvKey = Eip2333MasterKeyFactory(seed).makePrivateKey()
        // static from code to prevent any changes in future
        // Validated via https://iancoleman.io/eip2333/
        assertEquals(
            prvKey.privateKey.toHexString(),
            "3492BB55324A0F413798EA71856358556D25AD4EBC9458F062435C7B559E4670",
        )
        assertEquals(prvKey.chainCode.toHexString(), "")

        // Validated via:
        // https://iancoleman.io/blsttc_ui/
        // https://github.com/Chia-Network/bls-signatures
        // https://iancoleman.io/eip2333/
        val pubKey = ExtendedPublicKey(
            publicKey =
            "a6d8551cfcf8aefa062c60ffa246466c158e017fba12570327f47a004d5846cf2fabc2952b8f1653f7d224efd9d9b826".hexToBytes(),
            chainCode = byteArrayOf(),
        )

        assertEquals(
            pubKey.publicKey.toHexString(),
            "A6D8551CFCF8AEFA062C60FFA246466C158E017FBA12570327F47A004D5846CF2FABC2952B8F1653F7D224EFD9D9B826",
        )
        assertEquals(pubKey.chainCode.toHexString(), "")

        // proof validated via https://github.com/Chia-Network/bls-signatures
        // A73E200D8FA36522EBBBBD285EFCC9E9F173E9AE7284E42E91FE13D7D7514204E92813AB8082878EAB45867C32EBAA0B106E98FC8DFEB8D3CDA3E1130B8E17AC99BCFC005513F73BB8A17135D78923169BD9567C33C6E9D69D382E018B33FFC1
    }

    private fun createDefaultWordlist(): Wordlist {
        val wordlistStream = getInputStreamForTestFile()
        return BIP39Wordlist(wordlistStream)
    }

    private fun getInputStreamForTestFile(): InputStream {
        return object {}.javaClass.classLoader.getResourceAsStream(BIP39WordlistTest.TEST_DICTIONARY_FILE_NAME)!!
    }
}

package com.tangem.crypto

import com.tangem.common.MasterKeyFactory
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import org.junit.Test
import kotlin.test.assertEquals

// https://eips.ethereum.org/EIPS/eip-2333#hkdf_mod_r-1

internal class BlsTests {

    @Test
    fun testCase0() {
        val key = MasterKeyFactory.makePrivateKey(
            "c55257c360c07c72029aebc1b53c05ed0362ada38ead3e3e9efa3708e53495531f09a6987599d18264c1e1c92f2cf141630c7a3c4ab7c81b2f001698e7463b04".hexToBytes(),
            EllipticCurve.Bls12381G2,
        )
        // 6083874454709270928345386274498605044986640685124978867557563392430687146096 in decimal representation
        assertEquals(
            key.privateKey.toHexString().lowercase(),
            "0d7359d57963ab8fbbde1852dcf553fedbc31f464d80ee7d40ae683122b45070",
        )
    }

    @Test
    fun testCase1() {
        val key = MasterKeyFactory.makePrivateKey(
            "0x3141592653589793238462643383279502884197169399375105820974944592".hexToBytes(),
            EllipticCurve.Bls12381G2,
        )
        // 29757020647961307431480504535336562678282505419141012933316116377660817309383 in decimal representation
        assertEquals(
            key.privateKey.toHexString().lowercase(),
            "41c9e07822b092a93fd6797396338c3ada4170cc81829fdfce6b5d34bd5e7ec7",
        )
    }

    @Test
    fun testCase2() {
        val key = MasterKeyFactory.makePrivateKey(
            "0x0099FF991111002299DD7744EE3355BBDD8844115566CC55663355668888CC00".hexToBytes(),
            EllipticCurve.Bls12381G2,
        )
        // 27580842291869792442942448775674722299803720648445448686099262467207037398656 in decimal representation
        assertEquals(
            key.privateKey.toHexString().lowercase(),
            "3cfa341ab3910a7d00d933d8f7c4fe87c91798a0397421d6b19fd5b815132e80",
        )
    }

    @Test
    fun testCase3() {
        val key = MasterKeyFactory.makePrivateKey(
            "0xd4e56740f876aef8c010b86a40d5f56745a118d0906a34e69aec8c0db1cb8fa3".hexToBytes(),
            EllipticCurve.Bls12381G2,
        )
        // 19022158461524446591288038168518313374041767046816487870552872741050760015818 in decimal representation
        assertEquals(
            key.privateKey.toHexString().lowercase(),
            "2a0e28ffa5fbbe2f8e7aad4ed94f745d6bf755c51182e119bb1694fe61d3afca",
        )
    }
}

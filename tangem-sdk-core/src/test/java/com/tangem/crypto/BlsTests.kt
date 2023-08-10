package com.tangem.crypto

import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// https://eips.ethereum.org/EIPS/eip-2333#hkdf_mod_r-1

@OptIn(ExperimentalUnsignedTypes::class)
internal class BlsTests {
    @Test
    fun testCase0() {
        val key = Bls.makeMasterKey(
            (
                "c55257c360c07c72029aebc1b53c05ed0362ada38ead3e3e9efa3708e53495531f09a6987599d18264c1e1c92f2cf14163" +
                    "0c7a3c4ab7c81b2f001698e7463b04"
                ).hexToBytes(),
        )
        // 6083874454709270928345386274498605044986640685124978867557563392430687146096 in decimal representation
        assertEquals(
            key.toHexString().lowercase(),
            "0d7359d57963ab8fbbde1852dcf553fedbc31f464d80ee7d40ae683122b45070",
        )
    }

    @Test
    fun testCase1() {
        val key = Bls.makeMasterKey(
            "0x3141592653589793238462643383279502884197169399375105820974944592".hexToBytes(),
        )
        // 29757020647961307431480504535336562678282505419141012933316116377660817309383 in decimal representation
        assertEquals(
            key.toHexString().lowercase(),
            "41c9e07822b092a93fd6797396338c3ada4170cc81829fdfce6b5d34bd5e7ec7",
        )
    }

    @Test
    fun testCase2() {
        val key = Bls.makeMasterKey(
            "0x0099FF991111002299DD7744EE3355BBDD8844115566CC55663355668888CC00".hexToBytes(),
        )
        // 27580842291869792442942448775674722299803720648445448686099262467207037398656 in decimal representation
        assertEquals(
            key.toHexString().lowercase(),
            "3cfa341ab3910a7d00d933d8f7c4fe87c91798a0397421d6b19fd5b815132e80",
        )
    }

    @Test
    fun testCase3() {
        val key = Bls.makeMasterKey(
            "0xd4e56740f876aef8c010b86a40d5f56745a118d0906a34e69aec8c0db1cb8fa3".hexToBytes(),
        )
        // 19022158461524446591288038168518313374041767046816487870552872741050760015818 in decimal representation
        assertEquals(
            key.toHexString().lowercase(),
            "2a0e28ffa5fbbe2f8e7aad4ed94f745d6bf755c51182e119bb1694fe61d3afca",
        )
    }

    @Test
    fun testVerifyHash_SignedByCard_Bls12381G2() {
        val hash = "B0AF1CC5BEF3C46231ED41D85EFB3F0A53B5F95FA26C38E597AD00BE90F2244D768B4AA6B137FA60B664AE6A8ABD37AD" +
            "0971DE794B8FB66998A22DECD419DFE522D2F2064127734A603C023AA8C876624CC6CB93B3BBDAD2AEF6F0075F08AAF6"
        val walletPublicKey = "A45D3B42CA147B5681B586B35D2103044DFEA0472A74AB04215C2BD9135FFA5D4BD36C95739583A78ED89" +
            "17C6B62C41E"
        val signature = "A330DBB07CBF54DDB92D9D38E227AF20B2B704F41B46E4698B09B4F74EB6D09FBF956BDCB88D9DC32682659EECA" +
            "AFC2219AECB9E15735797409878919F8DC40E4C1884F34FA2C671EEFA5D3D80D590624889EE6EFC298BFCA4102CCF4FE726B2"

        val verified = CryptoUtils.verifyHash(
            walletPublicKey.hexToBytes(),
            hash.hexToBytes(),
            signature.hexToBytes(),
            EllipticCurve.Bls12381G2,
        )
        assertTrue(verified)
    }

    @Test
    fun testVerifyHash_SignedByCard_Bls12381G2_POP() {
        val hash = "8DA90BD912A577A73394C1DC4AA1FB22FFA552C585EEE08B1A6880ADDDB142C4C5B0BC841497E9B4D8A768014840CC72" +
            "0AAD77CC86716A518BD4BB126E3F23B3F1AAEA80620BF105B73726A7AD94D4192357199313F17B4207008772E0D9C9E7"
        val walletPublicKey = "A45D3B42CA147B5681B586B35D2103044DFEA0472A74AB04215C2BD9135FFA5D4BD36C95739583A78ED89" +
            "17C6B62C41E"
        val signature = "865D3B9EB8F7E7B1239D85187A6F4195411E363ED72BB6EEFD8EF28999C23E3FE66D8FF4A67DD9B172261415A15" +
            "EAE9410097BAB2A0DFCB23BB08EB542730D8AB6500F91CC623BA4C268A689AFD44FB055BA73531F7D0E452579372D1CE50BE3"

        val verified = CryptoUtils.verifyHash(
            walletPublicKey.hexToBytes(),
            hash.hexToBytes(),
            signature.hexToBytes(),
            EllipticCurve.Bls12381G2Pop,
        )
        assertTrue(verified)
    }

    @Test
    fun testVerifyHash_SignedByCard_Bls12381G2_AUG() {
        val hash = "B3928708839DB859E49010C6C154DB62568682EE80BD6056B42963468DA66074A4E74DAB825E910727236D28436D91CE" +
            "0D65B13C6D909E765154496CE81F63F57A39ABE4F787F197CE4AFCC9BC77CDD8CBB2A573FA949CCAA0A8637F41303BCC"
        val walletPublicKey = "A45D3B42CA147B5681B586B35D2103044DFEA0472A74AB04215C2BD9135FFA5D4BD36C95739583A78ED89" +
            "17C6B62C41E"
        val signature = "A68244E488B9A57EAA02C13D0A64A190ED66277239395F9FC83AFE77FC28EE974F38EC7D130565AB2B77F266724" +
            "4FC5815EDF9B66C82B3705BEBC6FF38009CCF8E69000D617911435EA930AE7BCB7D26B98E1D5A2AFDF5B427FC85BFE7FA7E6B"

        val verified = CryptoUtils.verifyHash(
            walletPublicKey.hexToBytes(),
            hash.hexToBytes(),
            signature.hexToBytes(),
            EllipticCurve.Bls12381G2Aug,
        )
        assertTrue(verified)
    }

    @Test
    fun testVerifyMessage_SignedByCard_Bls12381G2() {
        val message =
            "566C4D5645436259736E354344756F485772505538783670383861695176653046"
        val walletPublicKey = "A45D3B42CA147B5681B586B35D2103044DFEA0472A74AB04215C2BD9135FFA5D4BD36C95739583A78ED89" +
            "17C6B62C41E"
        val signature = "8D8AC8C0A51AFC0C6CBE4432AAFF4D1A49721AACAB401E6316132B67CC8C496DFEDF7D3EE65B99A3ADF48116B19" +
            "1382D071D6241886B81A93071DC4E5089CEEB4412DA23275B867F730C011352ECE85BBA0A70D48F034F3874F0F4D240F3BE3C"

        val verified = CryptoUtils.verify(
            walletPublicKey.hexToBytes(),
            message.hexToBytes(),
            signature.hexToBytes(),
            EllipticCurve.Bls12381G2,
        )
        assertTrue(verified)
    }

    @Test
    fun testVerifyMessage_SignedByCard_Bls12381G2_forSasha() {
        val hash =
            "AE099E7C6C03D5267C454F4F31693CCC59162E1E89B3FD4DBD642833CA7A0CD7F47AB272366818EA307374EA0854430B116C709" +
                "B54C5165BE14C5FB36DB17FB8EEC98183B439CC622AE6793543B3A1083B94F34107F7714C5EB34730F772590C"
        val walletPublicKey = "A2C975348667926ACF12F3EECB005044E08A7A9B7D95F30BD281B55445107367A2E5D0558BE7943C8BD13" +
            "F9A1A7036FB"
        val signature = "A16951EC0C3BBCFC4C39D4E8927A1984C0D4AE64650FD9A48F5B0D2A7CC7D453677C5711859D2D71F200E16C267" +
            "0F0750036851A6C244F275BAD4A7382B7730A5231DD1BCF6C1BDFC36C1D993788954E02207ABA3F76643FDD7A59BB0C78EB35"

        val verified = CryptoUtils.verifyHash(
            walletPublicKey.hexToBytes(),
            hash.hexToBytes(),
            signature.hexToBytes(),
            EllipticCurve.Bls12381G2,
        )
        assertTrue(verified)
    }
}

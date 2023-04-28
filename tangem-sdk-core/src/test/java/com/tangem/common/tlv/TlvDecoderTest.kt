package com.tangem.common.tlv

import com.google.common.truth.Truth.assertThat
import com.tangem.common.card.Card
import com.tangem.common.card.EllipticCurve
import com.tangem.common.card.SigningMethod
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.hexToBytes
import com.tangem.operations.personalization.entities.ProductMask
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import java.util.Calendar
import java.util.Date

@Suppress("LargeClass")
class TlvDecoderTest {

    private val rawData =
        byteArrayOf(
            1,
            8,
            -53,
            34,
            0,
            0,
            0,
            2,
            115,
            116,
            32,
            11,
            83,
            77,
            65,
            82,
            84,
            32,
            67,
            65,
            83,
            72,
            0,
            2,
            1,
            2,
            -128,
            6,
            50,
            46,
            49,
            49,
            114,
            0,
            3,
            65,
            4,
            -49,
            11,
            -50,
            -66,
            -121,
            -25,
            -2,
            65,
            65,
            -13,
            14,
            49,
            27,
            -82,
            -33,
            -85,
            -113,
            65,
            20,
            8,
            -39,
            -75,
            57,
            45,
            65,
            -31,
            35,
            44,
            38,
            40,
            63,
            -44,
            113,
            -45,
            -75,
            -95,
            -118,
            118,
            29,
            65,
            117,
            -24,
            -53,
            82,
            -72,
            91,
            -20,
            -96,
            -77,
            -103,
            -14,
            -63,
            52,
            -127,
            -123,
            -27,
            -16,
            -128,
            -67,
            -3,
            -104,
            -26,
            -22,
            65,
            10,
            4,
            0,
            0,
            126,
            33,
            12,
            90,
            -127,
            2,
            0,
            41,
            -126,
            4,
            7,
            -29,
            5,
            2,
            -125,
            7,
            84,
            65,
            78,
            71,
            69,
            77,
            0,
            -124,
            3,
            69,
            84,
            72,
            -122,
            64,
            111,
            -103,
            48,
            -114,
            -40,
            18,
            -103,
            26,
            -102,
            -12,
            -38,
            -78,
            -90,
            -9,
            -98,
            88,
            -47,
            -100,
            -24,
            24,
            -105,
            -70,
            -72,
            6,
            94,
            -96,
            -77,
            11,
            -123,
            -28,
            -118,
            37,
            63,
            107,
            -55,
            -11,
            23,
            -12,
            13,
            -23,
            -121,
            -63,
            36,
            -59,
            70,
            116,
            91,
            -125,
            -34,
            -69,
            23,
            -112,
            6,
            17,
            4,
            -49,
            68,
            -56,
            29,
            -45,
            81,
            10,
            97,
            83,
            48,
            65,
            4,
            -127,
            -106,
            -86,
            75,
            65,
            10,
            -60,
            74,
            59,
            -100,
            -50,
            24,
            -25,
            -66,
            34,
            106,
            -22,
            7,
            10,
            -52,
            -125,
            -87,
            -49,
            103,
            84,
            15,
            -84,
            73,
            -81,
            37,
            18,
            -97,
            106,
            83,
            -118,
            40,
            -83,
            99,
            65,
            53,
            -114,
            60,
            79,
            -103,
            99,
            6,
            79,
            126,
            54,
            83,
            114,
            -90,
            81,
            -45,
            116,
            -27,
            -62,
            60,
            -35,
            55,
            -3,
            9,
            -101,
            -14,
            5,
            10,
            115,
            101,
            99,
            112,
            50,
            53,
            54,
            107,
            49,
            0,
            8,
            4,
            0,
            15,
            66,
            64,
            7,
            1,
            0,
            9,
            2,
            11,
            -72,
            96,
            65,
            4,
            -42,
            -5,
            -41,
            -84,
            -23,
            88,
            2,
            86,
            -63,
            -118,
            -123,
            -10,
            -66,
            -82,
            -107,
            -68,
            -93,
            111,
            47,
            93,
            -20,
            -86,
            74,
            28,
            21,
            81,
            93,
            -21,
            -124,
            -57,
            -102,
            55,
            17,
            84,
            -66,
            -68,
            -22,
            -128,
            126,
            -99,
            -65,
            -54,
            -42,
            59,
            -25,
            -21,
            -124,
            5,
            59,
            -16,
            -72,
            73,
            48,
            16,
            -27,
            103,
            -112,
            -73,
            2,
            96,
            -51,
            41,
            -42,
            116,
            98,
            4,
            0,
            15,
            66,
            52,
            99,
            4,
            0,
            0,
            0,
            13,
            15,
            1,
            0,
        )

    private val tlvData = Tlv.deserialize(rawData)

    private val tlvMapper = TlvDecoder(tlvData!!)

    private val cardDataRaw: ByteArray = tlvMapper.decode(TlvTag.CardData)
    private val cardDataMapper = TlvDecoder(Tlv.deserialize(cardDataRaw)!!)

    @Test
    fun `map optional when value is present`() {
        val mask: Card.SettingsMask? = tlvMapper.decodeOptional(TlvTag.SettingsMask)
        assertThat(mask).isNotNull()
    }

    @Test
    fun `map optional when no tag returns null`() {
        val tokenSymbol: String? = tlvMapper.decodeOptional(TlvTag.TokenSymbol)
        assertThat(tokenSymbol)
            .isNull()
    }

    @Test
    fun `map when value is null throws MissingTagException`() {
        assertThrows<TangemSdkError.DecodingFailedMissingTag> {
            tlvMapper.decode<String>(TlvTag.TokenSymbol)
        }
    }

    @Test
    fun `map optional to wrong type throws WrongTypeException`() {
        assertThrows<TangemSdkError.DecodingFailedTypeMismatch> {
            tlvMapper.decodeOptional<String?>(TlvTag.CardData)
        }
    }

    @Test
    fun `map to wrong type throws WrongTypeException`() {
        assertThrows<TangemSdkError.DecodingFailedTypeMismatch> {
            tlvMapper.decode<String>(TlvTag.CardData)
        }
    }

    @Test
    fun `map boolean missing flag returns false`() {
        val terminalIsLinked: Boolean = tlvMapper.decode(TlvTag.TerminalIsLinked)
        assertThat(terminalIsLinked)
            .isFalse()
    }

    @Test
    fun `map SettingsMask returns correct value`() {
        val mask: Card.SettingsMask = tlvMapper.decode(TlvTag.SettingsMask)
        assertThat(mask)
            .isNotNull()
        assertThat(mask.rawValue)
            .isEqualTo(32289)
        assertThat(mask.contains(Card.SettingsMask.Code.SkipSecurityDelayIfValidatedByLinkedTerminal))
            .isFalse()
        assertThat(mask.contains(Card.SettingsMask.Code.IsReusable))
            .isTrue()
        assertThat(mask.contains(Card.SettingsMask.Code.AllowSetPIN2))
            .isTrue()
        assertThat(mask.contains(Card.SettingsMask.Code.UseDynamicNDEF))
            .isTrue()
        assertThat(mask.contains(Card.SettingsMask.Code.PermanentWallet))
            .isFalse()
    }

    @Test
    fun `map SigningMethods single value returns correct value`() {
        val signingMethods: SigningMethod = tlvMapper.decode(TlvTag.SigningMethod)
        assertThat(signingMethods.contains(SigningMethod.Code.SignHash))
            .isTrue()
    }

    @Test
    fun `map SigningMethods set of methods returns correct value`() {
        val localMapper = TlvDecoder(Tlv.deserialize("070195".hexToBytes())!!)

        val signingMethods: SigningMethod = localMapper.decode(TlvTag.SigningMethod)
        assertThat(signingMethods.contains(SigningMethod.Code.SignHash))
            .isTrue()
        assertThat(signingMethods.contains(SigningMethod.Code.SignHashSignedByIssuer))
            .isTrue()
        assertThat(signingMethods.contains(SigningMethod.Code.SignHashSignedByIssuerAndUpdateIssuerData))
            .isTrue()
        assertThat(signingMethods.contains(SigningMethod.Code.SignRaw))
            .isFalse()
        assertThat(signingMethods.contains(SigningMethod.Code.SignRawSignedByIssuer))
            .isFalse()
        assertThat(signingMethods.contains(SigningMethod.Code.SignRawSignedByIssuerAndUpdateIssuerData))
            .isFalse()
        assertThat(signingMethods.contains(SigningMethod.Code.SignPos))
            .isFalse()
    }

    @Test
    fun `map CardStatus returns correct value`() {
        val cardStatus: Card.Status = tlvMapper.decode(TlvTag.Status)
        assertThat(cardStatus)
            .isEqualTo(Card.Status.Loaded)
    }

    @Test
    fun `map ProductMask with raw value 5 returns correct value`() {
        val localMapper = TlvDecoder(listOf(Tlv(TlvTag.ProductMask, byteArrayOf(5))))
        val productMask: ProductMask = localMapper.decode(TlvTag.ProductMask)
        assertThat(productMask.contains(ProductMask.Code.Note) && productMask.contains(ProductMask.Code.IdCard))
            .isTrue()
    }

    @Test
    fun `map ProductMask with raw value 1 returns correct value`() {
        val localMapper = TlvDecoder(listOf(Tlv(TlvTag.ProductMask, byteArrayOf(1))))
        val productMask: ProductMask = localMapper.decode(TlvTag.ProductMask)
        assertThat(productMask.contains(ProductMask.Code.Note))
            .isTrue()
    }

    @Test
    fun `map Enum with unknown code throws ConversionException error`() {
        val localMapper = TlvDecoder(listOf(Tlv(TlvTag.CurveId, "test".toByteArray())))
        assertThrows<TangemSdkError.DecodingFailed> {
            localMapper.decode<EllipticCurve>(TlvTag.CurveId)
        }
    }

    @Test
    fun `map DateTime returns correct value`() {
        val date: Date = cardDataMapper.decode(TlvTag.ManufactureDateTime)
        val expected = Calendar.getInstance().apply { this.set(2019, 4, 2, 0, 0, 0) }.time
        assertThat(date.toString())
            .isEqualTo(expected.toString())
    }

    @Test
    fun `map EllipticCurve returns correct value`() {
        val ellipticCurve: EllipticCurve = tlvMapper.decode(TlvTag.CurveId)
        assertThat(ellipticCurve)
            .isEqualTo(EllipticCurve.Secp256k1)
    }

    @Test
    fun `map ByteArray returns correctly`() {
        val cardPublicKey: ByteArray = tlvMapper.decode(TlvTag.CardPublicKey)
        assertThat(cardPublicKey)
            .isInstanceOf(ByteArray::class.java)
    }

    @Test
    fun `map Int returns correct value`() {
        val signedHashes: Int = tlvMapper.decode(TlvTag.WalletSignedHashes)
        assertThat(signedHashes)
            .isEqualTo(13)
    }

    @Test
    fun `map Int with wrong value throws ConversionException`() {
        val localMapper = TlvDecoder(listOf(Tlv(TlvTag.WalletSignedHashes, byteArrayOf(1, 2, 3, 4, 5))))
        assertThrows<TangemSdkError.DecodingFailed> {
            localMapper.decode<Int>(TlvTag.WalletSignedHashes)
        }
    }

    @Test
    fun `map UTF8 returns correct value`() {
        val blockchainId: String = cardDataMapper.decode(TlvTag.BlockchainName)
        assertThat(blockchainId)
            .isEqualTo("ETH")
    }

    @Test
    fun `map Hex returns correct value`() {
        val cardId: String = tlvMapper.decode(TlvTag.CardId)
        assertThat(cardId)
            .isEqualTo("CB22000000027374")
    }
}

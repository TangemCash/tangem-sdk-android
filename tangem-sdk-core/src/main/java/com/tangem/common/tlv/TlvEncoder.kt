package com.tangem.common.tlv

import com.tangem.Log
import com.tangem.common.card.*
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toByteArray
import com.tangem.common.files.FileDataMode
import com.tangem.common.files.FileSettings
import com.tangem.operations.issuerAndUserData.IssuerExtraDataMode
import com.tangem.operations.personalization.entities.ProductMask
import com.tangem.operations.read.ReadMode
import java.util.*

/**
 * Encodes information that is to be written on the card from parsed classes into [ByteArray]
 * (according to the provided [TlvTag] and corresponding [TlvValueType])
 * and then forms [Tlv] with the encoded values.
 */
class TlvEncoder {
    /**
     * @param tag [TlvTag] which defines the type of [Tlv] that will be created.
     * @param value information that is to be encoded into [Tlv].
     */
    inline fun <reified T> encode(tag: TlvTag, value: T?): Tlv {
        if (value != null) {
            val tlv = Tlv(tag, encodeValue(tag, value))
            Log.tlv { tlv.toString() }
            return tlv
        } else {
            val error = TangemSdkError.EncodingFailed("Encoding error. Value for tag $tag is null")
            Log.error { error.customMessage }
            throw error
        }
    }

    inline fun <reified T> encodeValue(tag: TlvTag, value: T): ByteArray {
        return when (tag.valueType()) {
            TlvValueType.HexString -> {
                typeCheck<T, String>(tag)
                (value as String).hexToBytes()
            }
            TlvValueType.HexStringToHash -> {
                typeCheck<T, String>(tag)
                (value as String).calculateSha256()
            }
            TlvValueType.Utf8String -> {
                typeCheck<T, String>(tag)
                (value as String).toByteArray()
            }
            TlvValueType.Uint8 -> {
                typeCheck<T, Int>(tag)
                (value as Int).toByteArray(1)
            }
            TlvValueType.Uint16 -> {
                typeCheck<T, Int>(tag)
                (value as Int).toByteArray(2)
            }
            TlvValueType.Uint32 -> {
                typeCheck<T, Int>(tag)
                (value as Int).toByteArray()
            }
            TlvValueType.BoolValue -> {
                typeCheck<T, Boolean>(tag)
                val booleanValue = value as Boolean
                if (booleanValue) byteArrayOf(1) else byteArrayOf(0)
            }
            TlvValueType.ByteArray -> {
                typeCheck<T, ByteArray>(tag)
                value as ByteArray
            }
            TlvValueType.EllipticCurve -> {
                typeCheck<T, EllipticCurve>(tag)
                (value as EllipticCurve).curve.toByteArray()
            }
            TlvValueType.DateTime -> {
                typeCheck<T, Date>(tag)
                val calendar = Calendar.getInstance().apply { time = (value as Date) }
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH) + 1
                val day = calendar.get(Calendar.DAY_OF_MONTH)
                return year.toByteArray(2) + month.toByte() + day.toByte()
            }
            TlvValueType.ProductMask -> {
                typeCheck<T, ProductMask>(tag)
                byteArrayOf((value as ProductMask).rawValue.toByte())
            }
            TlvValueType.SettingsMask -> {
                try {
                    typeCheck<T, CardSettingsMask>(tag)
                    val rawValue = (value as CardSettingsMask).rawValue
                    rawValue.toByteArray(determineByteArraySize(rawValue))
                } catch (ex: TangemSdkError.EncodingFailedTypeMismatch) {
                    Log.warning { "Type of mask is not CardSettingsMask. Trying to check CardWalletSettingsMask" }
                    typeCheck<T, CardWalletSettingsMask>(tag)
                    val rawValue = (value as CardWalletSettingsMask).rawValue
                    rawValue.toByteArray(4)
                }
            }
            TlvValueType.Status -> {
                try {
                    typeCheck<T, Card.Status>(tag)
                    (value as Card.Status).code.toByteArray()
                } catch (ex: Exception) {
                    Log.warning { "Status is not Card.Status type. Trying to check Card.Wallet.Status" }
                    typeCheck<T, Card.Wallet.Status>(tag)
                    (value as Card.Wallet.Status).code.toByteArray()
                }
            }
            TlvValueType.SigningMethod -> {
                typeCheck<T, SigningMethod>(tag)
                byteArrayOf((value as SigningMethod).rawValue.toByte())
            }
            TlvValueType.InteractionMode -> {
                try {
                    typeCheck<T, IssuerExtraDataMode>(tag)
                    byteArrayOf((value as IssuerExtraDataMode).code)
                } catch (ex: Exception) {
                    Log.warning { "InteractionMode is not IssuerDataMode type. Trying to check ReadMode" }
                    typeCheck<T, ReadMode>(tag)
                    byteArrayOf((value as ReadMode).rawValue.toByte())
                }
            }
            TlvValueType.FileDataMode -> {
                typeCheck<T, FileDataMode>(tag)
                byteArrayOf((value as FileDataMode).rawValue.toByte())
            }
            TlvValueType.FileSettings -> {
                typeCheck<T, FileSettings>(tag)
                (value as FileSettings).rawValue.toByteArray(2)
            }
        }
    }

    fun determineByteArraySize(value: Int): Int {
        val mask = 0xFFFF0000.toInt()
        return if ((value and mask) != 0) 4 else 2
    }

    inline fun <reified T, reified ExpectedT> typeCheck(tag: TlvTag) {
        if (T::class != ExpectedT::class) {
            val error = TangemSdkError.EncodingFailedTypeMismatch(
                    "Encoder: Mapping error. Type for tag: $tag must be ${tag.valueType()}. It is ${T::class}"
            )
            Log.error { error.customMessage }
            throw error
        }
    }
}
package com.tangem.common.tlv

import com.tangem.Log
import com.tangem.common.extensions.toHexString
import java.io.ByteArrayInputStream
import java.io.IOException

/**
 * The data converted to the Tag Length Value protocol.
 */
class Tlv {

    val tag: TlvTag
    val value: ByteArray
    val tagRaw: Int

    constructor(tagCode: Int, value: ByteArray = byteArrayOf()) {
        this.tag = TlvTag.byCode(tagCode)
        this.tagRaw = tagCode
        this.value = value
    }

    constructor(tag: TlvTag, value: ByteArray = byteArrayOf()) {
        this.tag = tag
        this.tagRaw = tag.code
        this.value = value
    }


    companion object {
        private fun tlvFromBytes(stream: ByteArrayInputStream): Tlv? {
            val code = stream.read()
            if (code == -1) return null
            var len = stream.read()
            if (len == -1)
                throw IOException("Can't read TLV")
            if (len == 0xFF) {
                val lenH = stream.read()
                if (lenH == -1)
                    throw IOException("Can't read TLV")
                len = stream.read()
                if (len == -1)
                    throw IOException("Can't read TLV")
                len = len or (lenH shl 8)
            }
            val value = ByteArray(len)
            if (len > 0) {
                if (len != stream.read(value)) {
                    throw IOException("Can't read TLV")
                }
            }
            val tag = TlvTag.byCode(code)
            return if (tag == TlvTag.Unknown) Tlv(code, value) else Tlv(tag, value)
        }


        fun deserialize(data: ByteArray, nfcV: Boolean = false): List<Tlv>? {
            val tlvList = mutableListOf<Tlv>()
            val stream = ByteArrayInputStream(data)
            var tlv: Tlv?
            do {
                try {
                    tlv = tlvFromBytes(stream)
                    if (tlv != null) tlvList.add(tlv)
                } catch (ex: IOException) {
                    Log.warning { "Failed to read tag from stream: ${ex.localizedMessage}" }
                    if (nfcV) break else return null
                }

            } while (tlv != null)
            return tlvList
        }
    }

    override fun toString(): String {
        val tagName = this.tag.toString().capitalize()
        val tagFullName = "TAG_$tagName"
        val size = String.format("%02d", value.size)
        return "$tagFullName [0x$tagRaw:$size]: ${value.toHexString()}"
    }
}

inline fun<reified T> Tlv.sendToLog(value: T) {
    var tlvString = this.toString()
    if (this.tag.valueType() != TlvValueType.ByteArray && this.tag.valueType() != TlvValueType.HexString) {
        tlvString += " ($value)"
    }
    Log.tlv { tlvString }
}

fun List<Tlv>.serialize(): ByteArray =
        this.map { it.serialize() }.reduce { arr1, arr2 -> arr1 + arr2 }

fun Tlv.serialize(): ByteArray {
    val tag = byteArrayOf(this.tag.code.toByte())
    val length = getLengthInBytes(this.value.size)
    val value = if (this.value.isNotEmpty()) this.value else byteArrayOf(0x00)
    return tag + length + value
}

private fun getLengthInBytes(tlvLength: Int): ByteArray {
    return if (tlvLength > 0) {
        if (tlvLength > 0xFE) {
            byteArrayOf(
                    0xFF.toByte(),
                    (tlvLength shr 8 and 0xFF).toByte(),
                    (tlvLength and 0xFF).toByte()
            )
        } else {
            byteArrayOf((tlvLength and 0xFF).toByte())
        }
    } else {
        byteArrayOf()
    }
}

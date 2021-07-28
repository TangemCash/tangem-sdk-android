package com.tangem.operations.personalization

import com.tangem.operations.personalization.entities.NdefRecord
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

/**
 * Encodes information that is to be written on the card as an Ndef Tag.
 */
class NdefEncoder(private val ndefRecords: List<NdefRecord>, private val useDinamicNdef: Boolean) {

    fun encode(): ByteArray {
        val bs = ByteArrayOutputStream()
        // space for size
        bs.write(0)
        bs.write(0)

        for (i in ndefRecords.indices) {
            val headerValue = (if (i == 0) 0x80 else 0x00) or (if (!useDinamicNdef && i == ndefRecords.size - 1) 0x40 else 0x00)
            var value: ByteArray = ndefRecords[i].value.toByteArray(StandardCharsets.UTF_8)
            encodeValue(ndefRecords[i], headerValue, bs)
        }

        val result = bs.toByteArray()
        result[0] = (result.size - 2 shr 8).toByte()
        result[1] = (result.size - 2 and 0xFF).toByte()
        return result

    }


    private fun encodeValue(ndefRecord: NdefRecord, headerValue: Int, bs: ByteArrayOutputStream) {
        when (ndefRecord.type) {
            NdefRecord.Type.AAR -> {
                bs.write((headerValue or 0x14)) // NDEF Header
                bs.write(0x0F) // Length of the record type
                bs.write(ndefRecord.valueInBytes().size) // Length of the payload data
                bs.write(byteArrayOf(0x61.toByte(), 0x6E.toByte(), 0x64.toByte(), 0x72.toByte(), 0x6F.toByte(), 0x69.toByte(), 0x64.toByte(), 0x2E.toByte(), 0x63.toByte(), 0x6F.toByte(), 0x6D.toByte(), 0x3A.toByte(),
                        0x70.toByte(), 0x6B.toByte(), 0x67.toByte())) // type name
                bs.write(ndefRecord.valueInBytes())
            }
            NdefRecord.Type.URI -> {
                bs.write((headerValue or 0x11)) // NDEF Header
                bs.write(0x01) // Length of the record type
                val uriIdentifierCode: Byte
                val prefix: String
                when {
                    ndefRecord.value.startsWith("http://www.") -> {
                        uriIdentifierCode = 0x01.toByte()
                        prefix = "http://www."
                    }
                    ndefRecord.value.startsWith("https://www.") -> {
                        uriIdentifierCode = 0x02.toByte()
                        prefix = "https://www."
                    }
                    ndefRecord.value.startsWith("http://") -> {
                        uriIdentifierCode = 0x03.toByte()
                        prefix = "http://"
                    }
                    ndefRecord.value.startsWith("https://") -> {
                        uriIdentifierCode = 0x04.toByte()
                        prefix = "https://"
                    }
                    else -> {
                        throw Exception()
                    }
                }
                val value = ndefRecord.value.substring(prefix.length).toByteArray()
                bs.write(value.size + 1) // Length of the payload data
                bs.write(0x55) // URI
                bs.write(uriIdentifierCode.toInt()) // ?
                bs.write(value)
            }
            NdefRecord.Type.TEXT -> {
                bs.write((headerValue or 0x11)) // NDEF Header
                bs.write(0x01) // Length of the record type
                bs.write(ndefRecord.valueInBytes().size.toByte() + 1 + "en".length) // Length of the payload data
                bs.write(0x54) // Text
                bs.write(0x02) // UTF8(MSB=0)|"en".length
                bs.write("en".toByteArray(StandardCharsets.US_ASCII))
                bs.write(ndefRecord.valueInBytes())
            }
            else -> throw Exception("Invalid NDEF record in config!")
        }
    }


}
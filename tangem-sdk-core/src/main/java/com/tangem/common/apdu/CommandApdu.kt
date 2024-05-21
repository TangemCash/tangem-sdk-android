package com.tangem.common.apdu

import com.tangem.Log
import com.tangem.common.card.AesMode
import com.tangem.common.card.EncryptionMode
import com.tangem.common.extensions.calculateCrc16
import com.tangem.common.extensions.toByteArray
import com.tangem.common.extensions.toHexString
import com.tangem.crypto.encrypt
import com.tangem.crypto.encryptCcm
import java.io.ByteArrayOutputStream

/**
 * Class that provides conversion of serialized request and Instruction code
 * to a raw data that can be sent to the card.
 *
 * @property ins Instruction code that determines the type of request for the card.
 * @property tlvs Tlvs encoded to a [ByteArray] that are to be sent to the card.
 */
class CommandApdu(

    val ins: Int,
    private val tlvs: ByteArray,

    private val p1: Int,
    private val p2: Int,

    private val le: Int = 0x00,

    private val cla: Int = ISO_CLA,
) {

    /**
     * Request converted to a raw data
     */
    val apduData: ByteArray

    init {
        apduData = toBytes()
    }

    constructor(instruction: Instruction, tlvs: ByteArray) : this(instruction.code, tlvs, 0, 0)

    private fun toBytes(): ByteArray {
        val data = tlvs

        val byteStream = ByteArrayOutputStream()
        byteStream.write(cla)
        byteStream.write(ins)
        byteStream.write(p1)
        byteStream.write(p2)
        if (data.isNotEmpty()) {
            byteStream.writeLength(data.size)
            byteStream.write(data)
        }
        return byteStream.toByteArray()
    }

    private fun ByteArrayOutputStream.writeLength(lc: Int) {
        this.write(0)
        this.write(lc.shr(bitCount = 8))
        this.write(lc.and(other = 0xFF))
    }

    fun encrypt(encryptionMode: EncryptionMode, encryptionKey: ByteArray?): CommandApdu {
        if (encryptionKey == null || p1 != EncryptionMode.None.byteValue) {
            return this
        }

        val crc: ByteArray = tlvs.calculateCrc16()
        val dataToEncrypt = tlvs.size.toByteArray(2) + crc + tlvs
        val encryptedData = dataToEncrypt.encrypt(encryptionKey)

        return CommandApdu(ins, encryptedData, encryptionMode.byteValue, p2, le, cla)
    }

    fun encryptCcm(encryptionKey: ByteArray?, encryptionNonce: ByteArray, includeNonce: Boolean=false): CommandApdu {
        if (encryptionKey == null || p1 != EncryptionMode.None.byteValue) {
            return this
        }

        val dataToEncrypt = tlvs
        val p1=if( !includeNonce ) AesMode.Ccm.p1 else 0x00
        val associatedData=byteArrayOf(cla.toByte(), ins.toByte(), p1.toByte(), p2.toByte())
        val encryptedData = (if(includeNonce) encryptionNonce else ByteArray(0))+dataToEncrypt.encryptCcm(encryptionKey, encryptionNonce, associatedData)

        Log.info { "Associated data: ${associatedData.toHexString()}" }
        return CommandApdu(ins, encryptedData, p1, p2, le, cla)
    }

    @Suppress("MagicNumber")
    override fun toString(): String {
        val lc = apduData.size.toByte()
        return ">>>> [${apduData.size + 4}  bytes]: $cla $ins $p1 $p2 $lc ${apduData.toHexString()}"
    }

    companion object {
        const val ISO_CLA = 0x00
    }
}

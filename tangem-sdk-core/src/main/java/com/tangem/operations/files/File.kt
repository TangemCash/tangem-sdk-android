package com.tangem.operations.files

import com.squareup.moshi.JsonClass
import com.tangem.common.extensions.calculateHashCode
import com.tangem.common.tlv.Tlv
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag

/**
 * Created by Anton Zhilenkov on 12/01/2022.
 */
@JsonClass(generateAdapter = true)
data class File(
    val name: String?,
    val fileData: ByteArray,
    val fileIndex: Int,
    val fileSettings: FileSettings?
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as File
        if (fileIndex != other.fileIndex) return false
        if (fileSettings != other.fileSettings) return false
        if (!fileData.contentEquals(other.fileData)) return false

        return true
    }

    override fun hashCode(): Int = calculateHashCode(
        fileData.contentHashCode(),
        fileIndex.hashCode(),
        fileSettings?.hashCode() ?: 0
    )

    companion object {
        operator fun invoke(response: ReadFileResponse): File? {
            if (response.size == null || response.settings == null) return null

            val namedFile = NamedFile(tlvData = response.fileData)
            val (name, fileData) = if (namedFile == null) {
                null to response.fileData
            } else {
                namedFile.name to namedFile.payload
            }

            return File(name, fileData, response.fileIndex, response.settings)
        }
    }
}

data class NamedFile(
    val name: String,
    val payload: ByteArray,
    val counter: Int? = null,
    val signature: ByteArray? = null
) {

    @Throws()
    fun serialize(): ByteArray {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.FileTypeName, name)
        tlvBuilder.append(TlvTag.FileData, payload)
        tlvBuilder.append(TlvTag.FileCounter, counter)
        tlvBuilder.append(TlvTag.FileSignature, signature)

        return tlvBuilder.serialize()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NamedFile
        if (name != other.name) return false
        if (!payload.contentEquals(other.payload)) return false

        return true
    }

    override fun hashCode(): Int = calculateHashCode(
        name.hashCode(),
        payload.contentHashCode(),
    )

    companion object {
        operator fun invoke(tlvData: ByteArray): NamedFile? {
            val tlv = Tlv.deserialize(tlvData) ?: return null

            return try {
                val decoder = TlvDecoder(tlv)
                val name = decoder.decode<String>(TlvTag.FileTypeName)
                val payload = decoder.decode<ByteArray>(TlvTag.FileData)
                val counter = decoder.decodeOptional<Int>(TlvTag.FileCounter)
                val signature = decoder.decodeOptional<ByteArray>(TlvTag.FileSignature)

                NamedFile(name, payload, counter, signature)
            } catch (ex: Exception) {
                null
            }
        }
    }
}

/**
 * File data to write by the user or file  owner.
 */
sealed class FileToWrite {

    val payload: ByteArray
        get() = fileName?.let {
            try {
                NamedFile(it, data).serialize()
            } catch (ex: Exception) {
                throw IllegalArgumentException(ex)
            }
        } ?: data

    private val data: ByteArray
        get() = when (this) {
            is ByFileOwner -> (this as ByFileOwner).data
            is ByUser -> (this as ByUser).data
        }

    private val fileName: String?
        get() = when (this) {
            is ByFileOwner -> (this as ByFileOwner).fileName
            is ByUser -> (this as ByUser).fileName
        }


    /**
     * Write file protected by the user with security delay or user code if set
     * @property data: Data to write
     * @property fileName: Optional name of the file. COS 4.0+
     * @property fileVisibility: Optional visibility setting for the file. COS 4.0+
     * @property walletPublicKey: Optional link to the card's wallet. COS 4.0+
     */
    data class ByUser(
        val data: ByteArray,
        val fileName: String?,
        val fileVisibility: FileVisibility?,
        val walletPublicKey: ByteArray?
    ) : FileToWrite() {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ByUser
            if (!data.contentEquals(other.data)) return false
            if (fileVisibility != other.fileVisibility) return false
            if (!walletPublicKey.contentEquals(other.walletPublicKey)) return false

            return true
        }

        override fun hashCode(): Int = calculateHashCode(
            data.contentHashCode(),
            fileVisibility.hashCode(),
            walletPublicKey.contentHashCode(),
        )
    }

    /**
     * Write file protected by the file owner with two signatures and counter
     * @property data: Data to write
     * @property fileName: Optional name of the file. COS 4.0+
     * @property startingSignature: Starting signature of the file data. You can use `FileHashHelper` to generate
     * signatures or
     * use it as a reference to create the signature yourself
     * @property finalizingSignature: Finalizing signature of the file data. You can use `FileHashHelper` to generate
     * signatures
     * or use it as a reference to create the signature yourself
     * @property counter: File counter to prevent replay attack
     * @property fileVisibility: Optional visibility setting for the file. COS 4.0+
     * @property walletPublicKey: Optional link to the card's wallet. COS 4.0+
     */
    data class ByFileOwner(
        val data: ByteArray,
        val fileName: String?,
        val startingSignature: ByteArray,
        val finalizingSignature: ByteArray,
        val counter: Int,
        val fileVisibility: FileVisibility?,
        val walletPublicKey: ByteArray?
    ) : FileToWrite() {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ByFileOwner
            if (!data.contentEquals(other.data)) return false
            if (!startingSignature.contentEquals(other.startingSignature)) return false
            if (!finalizingSignature.contentEquals(other.finalizingSignature)) return false
            if (counter != other.counter) return false
            if (fileVisibility != other.fileVisibility) return false
            if (!walletPublicKey.contentEquals(other.walletPublicKey)) return false

            return true
        }

        override fun hashCode(): Int = calculateHashCode(
            data.contentHashCode(),
            startingSignature.contentHashCode(),
            finalizingSignature.contentHashCode(),
            counter.hashCode(),
            fileVisibility.hashCode(),
            walletPublicKey.contentHashCode(),
        )
    }
}
package com.tangem.operations.files

import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.containsByte

/**
 * Created by Anton Zhilenkov on 20/07/2021.
 */
data class FileSettings(
    val isPermanent: Boolean,
    val visibility: FileVisibility,
) {

    companion object {

        @Throws(TangemSdkError.DecodingFailed::class)
        operator fun invoke(data: ByteArray): FileSettings {
            val significantByte = try {
                data.last()
            } catch (e: Exception) {
                throw TangemSdkError.DecodingFailed("Failed to decode FileSettings")
            }

            return if (data.size == 2) { //v3 version
                val visibility = if (significantByte == 1.toByte()) FileVisibility.Public else FileVisibility.Private
                FileSettings(false, visibility)
            } else {
                val settings = FileRawSettings(significantByte.toInt())
                val isPermanent = settings.rawValue.containsByte(FileRawSettings.isPermanent.rawValue)
                val visibility = if (settings.rawValue.containsByte(FileRawSettings.isPublic.rawValue)) {
                    FileVisibility.Public
                } else {
                    FileVisibility.Private
                }
                FileSettings(isPermanent, visibility)
            }
        }
    }
}


/**
 * File visibility. Private files can be read only with security delay or user code if set
 */
enum class FileVisibility {
    /**
     * User can read public files without any codes
     */
    Public,

    /**
     * User can read private files only with security delay or user code if set
     */
    Private;

    fun serializeValue(fwVersion: FirmwareVersion): ByteArray {
        return if (fwVersion.doubleValue < 4) {
            byteArrayOf(0x00, permissionsRawValue)
        } else {
            byteArrayOf(permissionsRawValue)
        }
    }

    private val permissionsRawValue: Byte
        get() = when (this) {
            Public -> FileRawSettings.isPublic.rawValue.toByte()
            Private -> 0x00
        }
}

data class FileRawSettings(val rawValue: Int) {
    companion object {
        val isPublic: FileRawSettings = FileRawSettings(0x01)
        val isPermanent: FileRawSettings = FileRawSettings(0x10)
    }
}
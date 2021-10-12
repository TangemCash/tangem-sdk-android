package com.tangem.common.files

import com.squareup.moshi.JsonClass
import com.tangem.operations.files.ReadFileResponse

/**
 * Created by Anton Zhilenkov on 20/07/2021.
 */
@JsonClass(generateAdapter = true)
data class File(
    val fileIndex: Int,
    val fileSettings: FileSettings?,
    val fileData: ByteArray
) {

    constructor(response: ReadFileResponse) : this(response.fileIndex, response.fileSettings, response.fileData)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as File

        if (fileIndex != other.fileIndex) return false
        if (fileSettings != other.fileSettings) return false
        if (!fileData.contentEquals(other.fileData)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fileIndex
        result = 31 * result + (fileSettings?.hashCode() ?: 0)
        result = 31 * result + fileData.contentHashCode()
        return result
    }
}
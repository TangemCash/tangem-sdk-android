package com.tangem.operations.personalization.entities

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class NdefRecord(
    val type: Type,
    val value: String
) {
    enum class Type {
        URI, AAR, TEXT
    }

    fun valueInBytes(): ByteArray = value.toByteArray()
}
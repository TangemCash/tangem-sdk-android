package com.tangem.common

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.operations.CommandResponse

@JsonClass(generateAdapter = true)
data class SuccessResponse(
    @Json(name = "cardId")
    val cardId: String,
) : CommandResponse

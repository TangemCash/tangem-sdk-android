package com.tangem.common

import com.squareup.moshi.JsonClass
import com.tangem.operations.CommandResponse

@JsonClass(generateAdapter = true)
data class SuccessResponse(val cardId: String) : CommandResponse
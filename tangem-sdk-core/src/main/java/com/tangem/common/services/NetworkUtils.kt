package com.tangem.common.services

import com.tangem.Log
import com.tangem.common.core.TangemSdkError
import kotlinx.coroutines.delay
import java.io.IOException

suspend fun <T> retryIO(
    times: Int = 3,
    initialDelay: Long = 100,
    maxDelay: Long = 1000,
    factor: Double = 2.0,
    block: suspend () -> T): T {
    var currentDelay = initialDelay
    repeat(times - 1) {
        try {
            return block()
        } catch (ex: IOException) {
            Log.network { "Request error: ${ex.localizedMessage}" }
        }
        delay(currentDelay)
        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
    }
    return block()
}

sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Failure(val error: Throwable) : Result<Nothing>()
}

fun Result.Failure.toTangemSdkError(): TangemSdkError {
    return when (this.error) {
        is TangemSdkError -> this.error
        else -> TangemSdkError.ExceptionError(this.error)
    }
}

suspend fun <T> performRequest(block: suspend () -> T): Result<T> {
    return try {
        val result = retryIO { block() }
        Result.Success(result)
    } catch (ex: Exception) {
        Log.network { "Perform request error: ${ex.localizedMessage}" }
        Result.Failure(TangemSdkError.NetworkError("Network error. Cause: ${ex.localizedMessage}"))
    }
}
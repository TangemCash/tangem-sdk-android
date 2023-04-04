package com.tangem.common.json

import com.tangem.common.CompletionResult
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.TangemError

/**
 * Created by Anton Zhilenkov on 14/09/2021.
 */
internal class JSONRPCLinker {

    val request: JSONRPCRequest?

    var response: JSONRPCResponse = JSONRPCResponse(null, null, null)
        private set(value) {
            // if incoming id is NULL, get it from the request
            field = field.copy(result = value.result, error = value.error, id = value.id ?: request?.id)
        }

    var runnable: CardSessionRunnable<*>? = null

    constructor(jsonString: String) {
        request = try {
            JSONRPCRequest(jsonString)
        } catch (ex: JSONRPCException) {
            response = JSONRPCResponse(null, ex.jsonRpcError)
            null
        }
    }

    constructor(map: Map<String, Any>) {
        request = try {
            JSONRPCRequest(map)
        } catch (ex: JSONRPCException) {
            response = JSONRPCResponse(null, ex.jsonRpcError)
            null
        }
    }

    fun initRunnable(jsonRpcConverter: JSONRPCConverter) {
        if (request == null) return

        try {
            runnable = jsonRpcConverter.convert(request)
        } catch (ex: JSONRPCException) {
            response = JSONRPCResponse(null, ex.jsonRpcError)
        }
    }

    fun linkResult(result: CompletionResult<*>) {
        when (result) {
            is CompletionResult.Success -> response = JSONRPCResponse(result.data, null)
            is CompletionResult.Failure -> linkError(result.error)
        }
    }

    fun linkError(tangemError: TangemError) {
        response = JSONRPCResponse(null, tangemError.toJSONRPCError())
    }

    fun hasError(): Boolean = request == null || response.error != null

    companion object {
        fun parse(jsonRequest: String, converter: MoshiJsonConverter): List<JSONRPCLinker> {
            val trimmed = jsonRequest.trim()
            val isJsonArray = trimmed.trim().let { it.startsWith("[") && it.endsWith("]") }
            return if (isJsonArray) {
                val listOfMap: List<Map<String, Any>>? = try {
                    converter.fromJson(trimmed)
                } catch (ex: Exception) {
                    throw JSONRPCErrorType.ParseError.toJSONRPCError(ex.localizedMessage).asException()
                }
                listOfMap?.map { JSONRPCLinker(it) }
                    ?: throw JSONRPCErrorType.ParseError.toJSONRPCError("Empty requests").asException()
            } else {
                listOf(JSONRPCLinker(jsonRequest))
            }
        }
    }
}

internal fun List<JSONRPCLinker>.createResult(converter: MoshiJsonConverter): String = when {
    isEmpty() -> ""
    size == 1 -> converter.toJson(this[0].response)
    else -> converter.toJson(this.map { it.response })
}

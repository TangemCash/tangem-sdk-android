package com.tangem.common.json

import com.squareup.moshi.JsonClass
import com.tangem.common.core.TangemError

/**
 * Created by Anton Zhilenkov on 26/04/2021.
 */
class JSONRPCRequest constructor(
    val method: String,
    val params: Map<String, Any?>,
    val id: Int?,
    val jsonrpc: String = "2.0",
) : JSONStringConvertible {

    companion object {
        operator fun invoke(jsonString: String): JSONRPCRequest {
            val converter = MoshiJsonConverter.INSTANCE
            val jsonMap: Map<String, Any?> = try {
                converter.fromJson(jsonString, converter.typedMap())!!
            } catch (ex: Exception) {
                throw JSONRPCErrorType.ParseError.toJSONRPCError(ex.localizedMessage).asException()
            }

            return JSONRPCRequest(jsonMap)
        }

        operator fun invoke(map: Map<String, Any?>): JSONRPCRequest {
            return try {
                val id = if (map.containsKey("id")) (map["id"] as? Double)?.toInt() else null
                val params: Map<String, Any> = if (map.containsKey("params")) extract("params", map) else mapOf()
                val jsonrpc: String = extract("jsonrpc", map)
                val method: String = extract("method", map)
                JSONRPCRequest(method, params, id, jsonrpc)
            } catch (ex: Exception) {
                when (ex) {
                    is JSONRPCException -> throw ex
                    else -> throw JSONRPCErrorType.ParseError.toJSONRPCError(ex.localizedMessage).asException()
                }
            }
        }

        @Throws(IllegalArgumentException::class)
        private inline fun <reified T> extract(name: String, jsonMap: Map<String, Any?>): T {
            try {
                return jsonMap[name] as T
            } catch (ex: Exception) {
                throw JSONRPCErrorType.InvalidRequest.toJSONRPCError(
                    "The field is missing or an unsupported value is used: $name",
                ).asException()
            }
        }
    }
}

@JsonClass(generateAdapter = true)
data class JSONRPCResponse(
    val result: Any?,
    val error: JSONRPCError?,
    val id: Int? = null,
    val jsonrpc: String = "2.0",
) : JSONStringConvertible

data class ErrorData(val code: Int, val message: String)

@JsonClass(generateAdapter = true)
class JSONRPCError constructor(
    val code: Int,
    val message: String,
    val data: ErrorData? = null,
) : JSONStringConvertible {

    constructor(type: JSONRPCErrorType, data: ErrorData? = null) : this(
        code = type.errorData.code,
        message = type.errorData.message,
        data = data,
    )

    fun asException(): JSONRPCException = JSONRPCException(this)

    override fun toString(): String {
        return "code: $code, message: $message. Data: $data"
    }
}

enum class JSONRPCErrorType(val errorData: ErrorData) {
    ParseError(ErrorData(code = -32700, message = "Parse error")),
    InvalidRequest(ErrorData(code = -32600, message = "Invalid request")),
    MethodNotFound(ErrorData(code = -32601, message = "Method not found")),
    InvalidParams(ErrorData(code = -32602, message = "Invalid parameters")),
    InternalError(ErrorData(code = -32603, message = "Internal error")),
    ServerError(ErrorData(code = -32000, message = "Server error")),
    UnknownError(ErrorData(code = -32999, message = "Unknown error")),
    ;

    fun toJSONRPCError(customMessage: String? = null): JSONRPCError {
        val description = ErrorData(this.errorData.code, customMessage ?: this.errorData.message)
        return JSONRPCError(this, description)
    }
}

class JSONRPCException(val jsonRpcError: JSONRPCError) : Exception(jsonRpcError.message) {
    override fun toString(): String = "$jsonRpcError"
}

fun TangemError.toJSONRPCError(): JSONRPCError {
    val description = ErrorData(code, this::class.java.simpleName)
    return JSONRPCError(JSONRPCErrorType.ServerError, description)
}

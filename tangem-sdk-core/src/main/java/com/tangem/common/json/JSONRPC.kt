package com.tangem.common.json

import com.squareup.moshi.JsonClass
import com.tangem.common.CompletionResult
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.TangemError
import com.tangem.operations.CommandResponse

/**
 * Created by Anton Zhilenkov on 26/04/2021.
 */
interface JSONRPCHandler<R : CommandResponse> {
    val method: String
    val requiresCardId: Boolean

    fun makeRunnable(params: Map<String, Any?>): CardSessionRunnable<R>
}

class JSONRPCConverter {

    private val handlers = mutableListOf<JSONRPCHandler<*>>()

    fun register(handler: JSONRPCHandler<*>) {
        handlers.add(handler)
    }

    @Throws(JSONRPCException::class)
    fun convert(request: JSONRPCRequest): CardSessionRunnable<*> {
        return getHandler(request).makeRunnable(request.params)
    }

    @Throws(JSONRPCException::class)
    fun getHandler(request: JSONRPCRequest): JSONRPCHandler<*> {
        val handler = handlers.firstOrNull { it.method == request.method.toUpperCase() }
        if (handler == null) {
            val errorMessage = "Can't create the CardSessionRunnable. " +
                    "Missed converter for the method ${request.method}"
            throw JSONRPCError(JSONRPCError.Type.MethodNotFound, errorMessage).asException()
        }
        return handler
    }

    companion object {
        fun shared(): JSONRPCConverter {
            return JSONRPCConverter().apply {
                register(PersonalizeHandler())
                register(DepersonalizeHandler())
                register(PreflightReadHandler())
                register(ScanHandler())
                register(CreateWalletHandler())
                register(PurgeWalletHandler())
                register(SignHashHandler())
                register(SignHashesHandler())
                register(SetAccessCodeHandler())
                register(SetPasscodeHandler())
                register(ResetUserCodesHandler())
                register(DerivePublicKeyHandler())
            }
        }
    }
}

@JsonClass(generateAdapter = true)
class JSONRPCRequest constructor(
    val method: String,
    val params: Map<String, Any?>,
    val id: Int?,
    val jsonrpc: String = "2.0",
) : JSONStringConvertible {

    companion object {
        @Throws(JSONRPCException::class)
        operator fun invoke(jsonString: String): JSONRPCRequest {
            val converter = MoshiJsonConverter.INSTANCE
            val jsonMap = try {
                converter.toMap(jsonString)
            } catch (ex: Exception) {
                throw JSONRPCError(JSONRPCError.Type.ParseError, ex.localizedMessage).asException()
            }
            return try {
                val method: String = extract("method", jsonMap)
                val params: Map<String, Any> = extract("params", jsonMap)
                val id: Int? = extract<Double>("id", jsonMap)?.toInt()
                val jsonrpc: String = extract("jsonrpc", jsonMap)
                JSONRPCRequest(method, params, id, jsonrpc)
            } catch (ex: JSONRPCException) {
                throw JSONRPCError(JSONRPCError.Type.ParseError, converter.toJson(ex)).asException()
            }
        }

        @Throws(JSONRPCException::class)
        private inline fun <reified T> extract(name: String, jsonMap: Map<String, Any>): T {
            try {
                return jsonMap[name] as T
            } catch (ex: java.lang.Exception) {
                throw JSONRPCError(JSONRPCError.Type.InvalidRequest, name).asException()
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

@JsonClass(generateAdapter = true)
class JSONRPCError constructor(
    val code: Int,
    val message: String,
    val data: Any? = null
) : JSONStringConvertible {

    constructor(type: Type, data: Any? = null) : this(type.error.code, type.error.message, data)

    fun asException(): JSONRPCException = JSONRPCException(this)

    enum class Type(val error: ErrorDescription) {
        ParseError(ErrorDescription(-32700, "Parse error")),
        InvalidRequest(ErrorDescription(-32600, "Invalid request")),
        MethodNotFound(ErrorDescription(-32601, "Method not found")),
        InvalidParams(ErrorDescription(-32602, "Invalid parameters")),
        InternalError(ErrorDescription(-32603, "Internal error")),
        ServerError(ErrorDescription(-32000, "Server error")),
        UnknownError(ErrorDescription(-32999, "Unknown error"));

        data class ErrorDescription(val code: Int, val message: String)
    }

    override fun toString(): String {
        return "code: $code, message: ${message}. Data: $data"
    }
}

class JSONRPCException(val jsonRpcError: JSONRPCError) : Exception(jsonRpcError.message) {
    override fun toString(): String = "$jsonRpcError"
}

fun JSONRPCError.toJSONRPCResponse(id: Int? = null): JSONRPCResponse {
    return JSONRPCResponse(null, this, id)
}

fun JSONRPCException.toJSONRPCResponse(id: Int? = null): JSONRPCResponse {
    return this.jsonRpcError.toJSONRPCResponse(id)
}

fun <T> CompletionResult<T>.toJSONRPCResponse(id: Int? = null): JSONRPCResponse = when (this) {
    is CompletionResult.Success -> this.toJSONRPCResponse(id)
    is CompletionResult.Failure -> this.toJSONRPCResponse(id)
}

fun <T> CompletionResult.Success<T>.toJSONRPCResponse(id: Int? = null): JSONRPCResponse {
    return JSONRPCResponse(this.data, null, id)
}

fun <T> CompletionResult.Failure<T>.toJSONRPCResponse(id: Int? = null): JSONRPCResponse {
    return JSONRPCResponse(null, this.error.toJSONRPCError(), id)
}

fun TangemError.toJSONRPCError(): JSONRPCError {
    return JSONRPCError(JSONRPCError.Type.ServerError, code)
}

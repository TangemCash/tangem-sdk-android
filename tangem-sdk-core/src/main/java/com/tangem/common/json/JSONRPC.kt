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

    fun makeRunnable(params: Map<String, Any?>): CardSessionRunnable<R>
}

class JSONRPCConverter {

    private val handlers = mutableListOf<JSONRPCHandler<*>>()

    fun register(handler: JSONRPCHandler<*>) {
        handlers.add(handler)
    }

    @Throws(JSONRPCException::class)
    fun convert(request: JSONRPCRequest): CardSessionRunnable<*> {
        return try {
            getHandler(request).makeRunnable(request.params)
        } catch (ex: Exception) {
            when (ex) {
                is JSONRPCException -> throw ex
                else -> {
                    // JsonDataException and others
                    throw JSONRPCErrorType.ParseError.toJSONRPCError(ex.localizedMessage).asException()
                }
            }

        }

    }

    @Throws(JSONRPCException::class)
    fun getHandler(request: JSONRPCRequest): JSONRPCHandler<*> {
        val handler = handlers.firstOrNull { it.method == request.method.toUpperCase() }
        if (handler == null) {
            val errorMessage = "Can't create the CardSessionRunnable. " +
                    "Missed converter for the method: ${request.method}"
            throw JSONRPCErrorType.MethodNotFound.toJSONRPCError(errorMessage).asException()
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
                register(ReadFilesHandler())
                register(WriteFilesHandler())
                register(DeleteFilesHandler())
                register(ChangeFileSettingsHandler())
            }
        }
    }
}

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
                        "The field is missing or an unsupported value is used: $name"
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
    val data: ErrorData? = null
) : JSONStringConvertible {

    constructor(type: JSONRPCErrorType, data: ErrorData? = null) : this(type.errorData.code, type.errorData.message, data)

    fun asException(): JSONRPCException = JSONRPCException(this)

    override fun toString(): String {
        return "code: $code, message: ${message}. Data: $data"
    }
}

enum class JSONRPCErrorType(val errorData: ErrorData) {
    ParseError(ErrorData(-32700, "Parse error")),
    InvalidRequest(ErrorData(-32600, "Invalid request")),
    MethodNotFound(ErrorData(-32601, "Method not found")),
    InvalidParams(ErrorData(-32602, "Invalid parameters")),
    InternalError(ErrorData(-32603, "Internal error")),
    ServerError(ErrorData(-32000, "Server error")),
    UnknownError(ErrorData(-32999, "Unknown error"));

    fun toJSONRPCError(customMessage: String? = null): JSONRPCError {
        val description = ErrorData(this.errorData.code, customMessage ?: this.errorData.message)
        return JSONRPCError(this, description)
    }
}

internal class JSONRPCLinker {

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

    val request: JSONRPCRequest?

    var response: JSONRPCResponse = JSONRPCResponse(null, null, null)
        private set(value) {
            // if incoming id is NULL, get it from the request
            field = field.copy(result = value.result, error = value.error, id = value.id ?: request?.id)
        }

    var runnable: CardSessionRunnable<*>? = null

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

class JSONRPCException(val jsonRpcError: JSONRPCError) : Exception(jsonRpcError.message) {
    override fun toString(): String = "$jsonRpcError"
}

fun TangemError.toJSONRPCError(): JSONRPCError {
    val description = ErrorData(code, this::class.java.simpleName)
    return JSONRPCError(JSONRPCErrorType.ServerError, description)
}

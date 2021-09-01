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
                    throw JSONRPCError(JSONRPCError.Type.ParseError, ex.localizedMessage).asException()
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
                register(ReadFilesHandler())
                register(WriteFilesHandler())
                register(DeleteFilesHandler())
                register(ChangeFileSettingsHandler())
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
                val id: Int? = if (jsonMap.containsKey("id")) extract<Double>("id", jsonMap).toInt() else null
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

internal class JSONRPCLinker {

    constructor(jsonString: String) {
        request = createRequest(jsonString)
    }

    constructor(map: Map<String, Any>) {
        request = createRequest(map)
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
            is CompletionResult.Failure -> linkeError(result.error)
        }
    }

    fun linkeError(tangemError: TangemError) {
        response = JSONRPCResponse(null, tangemError.toJSONRPCError())
    }

    fun hasError(): Boolean = request == null || response.error != null

    private fun createRequest(jsonString: String): JSONRPCRequest? {
        val converter = MoshiJsonConverter.INSTANCE
        val jsonMap = try {
            converter.toMap(jsonString)
        } catch (ex: Exception) {
            val error = JSONRPCError(JSONRPCError.Type.ParseError, ex.localizedMessage)
            response = JSONRPCResponse(null, error)
            return null
        }

        return createRequest(jsonMap)
    }

    private fun createRequest(map: Map<String, Any>): JSONRPCRequest? {
        val id = if (map.containsKey("id")) (map["id"] as? Double)?.toInt() else null
        // initiate blank response with id
        response = JSONRPCResponse(null, null, id)

        return try {
            val params: Map<String, Any> = if (map.containsKey("params")) extract("params", map) else mapOf()
            val jsonrpc: String = extract("jsonrpc", map)
            val method: String = extract("method", map)
            JSONRPCRequest(method, params, id, jsonrpc)
        } catch (ex: Exception) {
            val error = JSONRPCError(JSONRPCError.Type.ParseError, ex.localizedMessage)
            response = JSONRPCResponse(null, error)
            null
        }
    }

    @Throws(IllegalArgumentException::class)
    private inline fun <reified T> extract(name: String, jsonMap: Map<String, Any>): T {
        try {
            return jsonMap[name] as T
        } catch (ex: Exception) {
            throw IllegalArgumentException(name, ex)
        }
    }

    companion object {
        fun parse(jsonRequest: String, converter: MoshiJsonConverter): List<JSONRPCLinker> {
            val trimmed = jsonRequest.trim()
            val isJsonArray = trimmed.trim().let { it.startsWith("[") && it.endsWith("]") }
            return if (isJsonArray) {
                val listOfMap: List<Map<String, Any>>? = try {
                    converter.fromJson(trimmed)
                } catch (ex: Exception) {
                    throw JSONRPCError(JSONRPCError.Type.ParseError, ex.localizedMessage).asException()
                }
                listOfMap?.map { JSONRPCLinker(it) }
                        ?: throw JSONRPCError(JSONRPCError.Type.ParseError, "Empty requests").asException()
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
    return JSONRPCError(JSONRPCError.Type.ServerError, "$code: ${this::class.java.simpleName}")
}

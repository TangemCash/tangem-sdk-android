package com.tangem.commands.common.jsonRpc

import com.squareup.moshi.JsonClass
import com.tangem.CardSessionRunnable
import com.tangem.TangemError
import com.tangem.commands.CommandResponse
import com.tangem.commands.SignCommand
import com.tangem.commands.common.jsonConverter.MoshiJsonConverter
import com.tangem.commands.personalization.DepersonalizeCommand
import com.tangem.commands.personalization.PersonalizeCommand
import com.tangem.common.CompletionResult
import com.tangem.extentions.toSnakeCase
import com.tangem.tasks.ScanTask
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.functions

/**
 * Created by Anton Zhilenkov on 26/04/2021.
 */
interface JSONRPCConvertible<R : CommandResponse> {
    fun makeRunnable(params: Map<String, Any?>): CardSessionRunnable<R>
}

class JSONRPCConverter {

    val jsonConverter = MoshiJsonConverter.INSTANCE

    private val runnables = mutableMapOf<String, KClass<*>>()

    fun register(clazz: KClass<*>) {
        val name = clazz.simpleName?.toSnakeCase()?.toUpperCase() ?: return

        runnables[name] = clazz
    }

    @Throws(JSONRPCException::class)
    fun convert(request: JSONRPCRequest): CardSessionRunnable<*> {
        val methodNotFound = JSONRPCError.Type.MethodNotFound

        val kClass = runnables[request.method]
        if (kClass == null) {
            val errorMessage = "Can't create the CardSessionRunnable. " +
                "Missed converter for the method ${request.method}"
            throw JSONRPCError(methodNotFound, errorMessage).asException()
        }

        val companion = kClass.companionObject
        if (companion == null) {
            val errorMessage = "Can't create the CardSessionRunnable. " +
                "Class [${kClass::simpleName}] is missed a companion object"
            throw JSONRPCError(methodNotFound, errorMessage).asException()
        }

        val function = companion.functions.find { it.name == "asJSONRPCConvertible" }
        if (function == null) {
            val errorMessage = "Can't create the CardSessionRunnable. " +
                "Class [${kClass::simpleName}] is missed the [asJSONRPCConvertible] function"
            throw JSONRPCError(methodNotFound, errorMessage).asException()
        }

        try {
            val convertible = function.call(kClass.companionObjectInstance) as JSONRPCConvertible<*>
            return convertible.makeRunnable(request.params)
        } catch (ex: Exception) {
            throw JSONRPCError(methodNotFound, ex.localizedMessage).asException()
        }
    }

    companion object {
        fun shared(): JSONRPCConverter {
            return JSONRPCConverter().apply {
                register(ScanTask::class)
                register(SignCommand::class)
                register(PersonalizeCommand::class)
                register(DepersonalizeCommand::class)
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
                val id: Int? = extract("id", jsonMap)
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
    val data: String? = null
) : JSONStringConvertible {

    constructor(type: Type, data: String? = null) : this(type.error.code, type.error.message, data)

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
}

class JSONRPCException(val jsonRpcError: JSONRPCError) : Throwable(jsonRpcError.message)

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
    return JSONRPCError(JSONRPCError.Type.ServerError, "code: $code, localizedMessage: $customMessage")
}

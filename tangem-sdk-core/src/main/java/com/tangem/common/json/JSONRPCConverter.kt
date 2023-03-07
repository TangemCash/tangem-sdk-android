package com.tangem.common.json

import com.tangem.common.core.CardSessionRunnable
import java.util.Locale

/**
 * Created by Anton Zhilenkov on 14/09/2021.
 */
interface JSONRPCHandler<R> {
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
        val handler = handlers.firstOrNull { it.method == request.method.uppercase(Locale.getDefault()) }
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
                register(DeriveWalletPublicKeyHandler())
                register(DeriveWalletPublicKeysHandler())
            }
        }
    }
}
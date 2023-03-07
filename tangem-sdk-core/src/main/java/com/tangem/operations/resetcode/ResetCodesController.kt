package com.tangem.operations.resetcode

import com.tangem.Log
import com.tangem.common.CardIdFormatter
import com.tangem.common.CompletionResult
import com.tangem.common.StringsLocator
import com.tangem.common.UserCodeType
import com.tangem.common.core.CardIdDisplayFormat
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemError
import com.tangem.common.extensions.VoidCallback
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import java.io.PrintWriter
import java.io.StringWriter

class ResetCodesController(
    private val resetService: ResetPinService,
    private val viewDelegate: ResetCodesViewDelegate,
) {
    var cardIdDisplayFormat: CardIdDisplayFormat = CardIdDisplayFormat.Full

    private var codeType: UserCodeType? = null
    private var callback: CompletionCallback<String>? = null
    private var newCode: String = ""
    private var cardId: String? = null

    val scope = CoroutineScope(Dispatchers.Main) + CoroutineExceptionHandler { _, ex ->
        val sw = StringWriter()
        ex.printStackTrace(PrintWriter(sw))
        Log.error { sw.toString() }
    }

    private val formattedCardId: String?
        get() = cardId?.let { CardIdFormatter(cardIdDisplayFormat).getFormattedCardId(it) }

    init {
        resetService.currentStateAsFlow
            .drop(1)
            .onEach { newState ->
                when (newState) {
                    ResetPinService.State.Finished -> {
                        viewDelegate.showAlert(
                            title = newState.getMessageTitle(viewDelegate.stringsLocator),
                            message = newState.getMessageBody(viewDelegate.stringsLocator),
                            onContinue = { handleContinue(CompletionResult.Success(false)) }
                        )
                    }
                    else -> {
                        viewDelegate.setState(
                            ResetCodesViewState.ResetCodes(
                                codeType!!,
                                state = newState,
                                cardId = formattedCardId,
                                callback = { handleContinue(it) }
                            )
                        )
                    }
                }
            }
            .launchIn(scope)
    }

    fun start(codeType: UserCodeType, cardId: String?, callback: CompletionCallback<String>) {
        this.cardId = cardId
        this.codeType = codeType
        this.callback = callback
        viewDelegate.setState(
            ResetCodesViewState.RequestCode(
                type = codeType,
                cardId = formattedCardId
            ) {
                handleCodeInput(it)
            }
        )
    }

    private fun handleCodeInput(result: CompletionResult<String>) {
        when (result) {
            is CompletionResult.Success -> {
                when (val setCodeResult = resetService.setAccessCode(result.data)) {
                    is CompletionResult.Success -> this.newCode = result.data
                    is CompletionResult.Failure -> viewDelegate.showError(setCodeResult.error)
                }
            }
            is CompletionResult.Failure -> viewDelegate.hide {
                callback?.invoke(CompletionResult.Failure(result.error))
            }
        }
    }

    private fun handleContinue(result: CompletionResult<Boolean>) {
        when (result) {
            is CompletionResult.Success -> {
                if (result.data) {
                    resetService.proceed(cardId)
                } else { // completed
                    viewDelegate.hide {
                        callback?.invoke(CompletionResult.Success(newCode))
                    }
                }
            }
            is CompletionResult.Failure -> viewDelegate.hide {
                this.callback?.invoke(CompletionResult.Failure(result.error))
            }
        }
    }
}

interface ResetCodesViewDelegate {

    var stopSessionCallback: VoidCallback
    val stringsLocator: StringsLocator

    fun setState(state: ResetCodesViewState)

    fun hide(callback: VoidCallback)

    fun showError(error: TangemError)

    fun showAlert(title: String, message: String, onContinue: VoidCallback)
}

sealed class ResetCodesViewState {

    object Empty : ResetCodesViewState()

    data class RequestCode(
        val type: UserCodeType,
        val cardId: String?,
        val callback: CompletionCallback<String>,
    ) : ResetCodesViewState()

    data class ResetCodes(
        val type: UserCodeType,
        val state: ResetPinService.State,
        val cardId: String?,
        val callback: CompletionCallback<Boolean>,
    ) : ResetCodesViewState()
}
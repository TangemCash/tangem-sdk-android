package com.tangem.common.core

import com.tangem.*
import com.tangem.common.*
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.card.Card
import com.tangem.common.card.EncryptionMode
import com.tangem.common.extensions.VoidCallback
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.json.*
import com.tangem.common.nfc.CardReader
import com.tangem.common.services.secure.SecureStorage
import com.tangem.crypto.EncryptionHelper
import com.tangem.crypto.pbkdf2Hash
import com.tangem.operations.*
import com.tangem.operations.read.ReadCommand
import com.tangem.operations.resetcode.ResetCodesController
import com.tangem.operations.resetcode.ResetPinService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Allows interaction with Tangem cards. Should be opened before sending commands.
 *
 * @property environment
 * @property reader  is an interface that is responsible for NFC connection and
 * transfer of data to and from the Tangem Card.
 * @property viewDelegate is an  interface that allows interaction with users and shows relevant UI.
 * @property cardId ID, Unique Tangem card ID number. If not null, the SDK will check that you the card
 * with which you tapped a phone has this [cardId] and SDK will return
 * the [TangemSdkError.WrongCardNumber] otherwise.
 * @property initialMessage A custom description that will be shown at the beginning of the NFC session.
 * If null, a default header and text body will be used.
 */
class CardSession(
    val viewDelegate: SessionViewDelegate,
    val environment: SessionEnvironment,
    private val reader: CardReader,
    private val jsonRpcConverter: JSONRPCConverter,
    private val secureStorage: SecureStorage,
    cardId: String? = null,
    private var initialMessage: Message? = null
) {

    var cardId: String? = cardId
        private set
    var connectedTag: TagType? = null
        private set
    var state = CardSessionState.Inactive
        private set

    private var resetCodesController: ResetCodesController? = null


    val scope = CoroutineScope(Dispatchers.IO) + CoroutineExceptionHandler { _, throwable ->
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val exceptionAsString: String = sw.toString()
        Log.error { exceptionAsString }
        throw throwable
    }

    private var preflightReadMode: PreflightReadMode = PreflightReadMode.FullCardRead

    fun setInitialMessage(message: Message?) {
        initialMessage = message
    }

    fun setMessage(message: Message?) {
        viewDelegate.setMessage(message)
    }

    /**
     * This method starts a card session, performs preflight [ReadCommand],
     * invokes [CardSessionRunnable.run] and closes the session.
     * @param runnable [CardSessionRunnable] that will be performed in the session.
     * @param callback will be triggered with a [CompletionResult] of a session.
     */
    fun <T : CardSessionRunnable<R>, R> startWithRunnable(runnable: T, callback: CompletionCallback<R>) {
        if (state != CardSessionState.Inactive) {
            val error = TangemSdkError.Busy()
            Log.error { "$error" }
            callback(CompletionResult.Failure(error))
            return
        }

        Log.session { "Start card session with runnable" }
        prepareSession(runnable) { prepareResult ->
            when (prepareResult) {
                is CompletionResult.Success -> {
                    start { session, error ->
                        if (error != null) {
                            Log.error { "$error" }
                            callback(CompletionResult.Failure(error))
                            return@start
                        }
                        Log.session { "Start runnable" }
                        runnable.run(this) { result ->
                            Log.session { "Runnable completed" }
                            when (result) {
                                is CompletionResult.Success -> stop()
                                is CompletionResult.Failure -> stopWithError(result.error)
                            }
                            callback(result)
                        }
                    }
                }
                is CompletionResult.Failure -> {
                    Log.error { "${prepareResult.error}" }
                    callback(CompletionResult.Failure(prepareResult.error))
                }
            }

        }
    }

    /**
     * Starts a card session and performs preflight [ReadCommand].
     * @param onSessionStarted: callback with the card session. Can contain [TangemSdkError] if something goes wrong.
     */
    fun start(onSessionStarted: SessionStartedCallback) {
        Log.session { "Start card session with delegate" }
        state = CardSessionState.Active
        viewDelegate.onSessionStarted(cardId, initialMessage, environment.config.howToIsEnabled)

        reader.scope = scope
        reader.startSession()

        scope.launch {
            reader.tag.asFlow()
                    .filterNotNull()
                    .take(1)
                    .collect { tagType ->
                        if (tagType == TagType.Nfc && preflightReadMode != PreflightReadMode.None) {
                            preflightCheck(onSessionStarted)
                        } else {
                            onSessionStarted(this@CardSession, null)
                        }
                    }
        }

        scope.launch {
            reader.tag.asFlow()
                    .drop(1)
                    .collect {
                        if (it == null) {
                            viewDelegate.onTagLost()
                        } else {
                            viewDelegate.onTagConnected()
                            onTagConnectedAfterResume?.invoke()
                            onTagConnectedAfterResume = null
                        }
                    }
        }

        scope.launch {
            reader.tag.asFlow()
                    .onCompletion {
                        if (it is CancellationException && it.message == TangemSdkError.UserCancelled().customMessage) {
                            stopWithError(TangemSdkError.UserCancelled())
                            viewDelegate.dismiss()
                            onSessionStarted(this@CardSession, TangemSdkError.UserCancelled())
                        }
                    }
                    .collect {
                        if (it == null && connectedTag != null && state == CardSessionState.Active) {
                            environment.encryptionKey = null
                            connectedTag = null
                        } else if (it != null) {
                            connectedTag = it
                        }
                    }
        }
    }

    fun run(jsonRequest: String, callback: (String) -> Unit) {
        val jsonrpcLinker = JSONRPCLinker(jsonRequest).apply { initRunnable(jsonRpcConverter) }
        if (jsonrpcLinker.hasError()) {
            callback(jsonrpcLinker.response.toJson())
            return
        }

        jsonrpcLinker.runnable!!.run(this) {
            jsonrpcLinker.linkResult(it)
            callback(jsonrpcLinker.response.toJson())
        }
    }

    private fun <T : CardSessionRunnable<*>> prepareSession(runnable: T, callback: CompletionCallback<Unit>) {
        Log.session { "Prepare card session" }
        preflightReadMode = runnable.preflightReadMode()
        runnable.prepare(this, callback)
    }

    private fun preflightCheck(onSessionStarted: SessionStartedCallback) {
        Log.session { "Start preflight check" }
        val preflightTask = PreflightReadTask(
                preflightReadMode,
                cardId = cardId
        )
        preflightTask.run(this) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    onSessionStarted(this, null)
                }
                is CompletionResult.Failure -> {
                    val wrongType = when (result.error) {
                        is TangemSdkError.WrongCardType -> WrongValueType.CardType
                        is TangemSdkError.WrongCardNumber -> WrongValueType.CardId
                        else -> null
                    }
                    if (wrongType != null) {
                        Log.error { "${result.error}" }
                        viewDelegate.onWrongCard(wrongType)
                        scope.launch(scope.coroutineContext) {
                            delay(3500)
                            if (connectedTag == null) {
                                onSessionStarted(this@CardSession, result.error)
                                stopWithError(result.error)
                            } else {
                                viewDelegate.onTagConnected()
                                preflightCheck(onSessionStarted)
                            }
                        }
                    } else {
                        onSessionStarted(this, result.error)
                        stopWithError(result.error)
                    }

                }
            }
        }
    }

    /**
     * Stops the current session with the text message.
     * @param message If null, the default message will be shown.
     */
    fun stop(message: Message? = null) {
        stopSession()
        viewDelegate.onSessionStopped(message)
    }

    /**
     * Stops the current session on error.
     * @param error An error that will be shown.
     */
    fun stopWithError(error: TangemError) {
        stopSession()
        if (error !is TangemSdkError.UserCancelled) {
            Log.error { "Finishing with error: ${error.code}" }
            viewDelegate.onError(error)
        } else {
            Log.debug { "User cancelled NFC session" }
        }
    }

    private fun stopSession() {
        Log.session { "Stop session" }
        state = CardSessionState.Inactive
        preflightReadMode = PreflightReadMode.FullCardRead
//        environmentService.saveEnvironmentValues(environment, cardId)
        reader.stopSession()
        scope.cancel()
    }

    fun send(apdu: CommandApdu, callback: CompletionCallback<ResponseApdu>) {
        Log.session { "Send" }
        val subscription = reader.tag.openSubscription()
        scope.launch {
            subscription.consumeAsFlow()
                    .filterNotNull()
                    .map { establishEncryptionIfNeeded() }
                    .map { apdu.encrypt(environment.encryptionMode, environment.encryptionKey) }
                    .map { encryptedApdu -> reader.transceiveApdu(encryptedApdu) }
                    .map { responseApdu -> decrypt(responseApdu) }
                    .catch {
                        if (it is TangemSdkError) {
                            Log.error { "$it" }
                            callback(CompletionResult.Failure(it))
                        }
                    }
                    .collect { result ->
                        when (result) {
                            is CompletionResult.Success -> {
                                subscription.cancel()
                                callback(result)
                            }
                            is CompletionResult.Failure -> {
                                when (result.error) {
                                    is TangemSdkError.TagLost -> Log.session { "Tag lost. Waiting for tag..." }
                                    else -> {
                                        Log.error { "${result.error}" }
                                        subscription.cancel()
                                        callback(result)
                                    }
                                }
                            }
                        }
                    }
        }
    }

    fun pause(error: TangemError? = null) {
        reader.pauseSession()
    }

    private var onTagConnectedAfterResume: VoidCallback? = null

    fun resume(onTagConnected: VoidCallback? = null) {
        onTagConnectedAfterResume = onTagConnected
        reader.resumeSession()
    }

    private suspend fun establishEncryptionIfNeeded(): CompletionResult<Boolean> {
        Log.session { "Try establish encryption" }
        if (environment.encryptionMode == EncryptionMode.None || environment.encryptionKey != null) {
            return CompletionResult.Success(true)
        }

        val encryptionHelper = EncryptionHelper.create(environment.encryptionMode)
                ?: return CompletionResult.Failure(
                    TangemSdkError.CryptoUtilsError("Failed to establish encryption")
                )

        val openSessionCommand = OpenSessionCommand(encryptionHelper.keyA)
        val apdu = openSessionCommand.serialize(environment)

        when (val response = reader.transceiveApdu(apdu)) {
            is CompletionResult.Success -> {
                val result = try {
                    openSessionCommand.deserialize(environment, response.data)
                } catch (error: TangemSdkError) {
                    return CompletionResult.Failure(error)
                }

                val uid = result.uid
                val protocolKey = environment.accessCode.value?.pbkdf2Hash(uid, 50)
                        ?: return CompletionResult.Failure(
                                TangemSdkError.CryptoUtilsError(
                                        "Failed to establish encryption"
                                )
                        )

                val secret = encryptionHelper.generateSecret(result.sessionKeyB)
                val sessionKey = (secret + protocolKey).calculateSha256()
                environment.encryptionKey = sessionKey

                return CompletionResult.Success(true)
            }
            is CompletionResult.Failure -> return CompletionResult.Failure(response.error)
        }
    }

    private fun decrypt(result: CompletionResult<ResponseApdu>): CompletionResult<ResponseApdu> {
        return when (result) {
            is CompletionResult.Success -> {
                try {
                    CompletionResult.Success(result.data.decrypt(environment.encryptionKey))
                } catch (error: TangemSdkError) {
                    CompletionResult.Failure(error)
                }
            }
            is CompletionResult.Failure -> result
        }
    }

    fun requestUserCodeIfNeeded(type: UserCodeType, isFirstAttempt: Boolean, callback: CompletionCallback<Unit>) {
        val userCode = when (type) {
            UserCodeType.AccessCode -> environment.accessCode.value
            UserCodeType.Passcode -> environment.passcode.value
        }
        if (userCode != null) {
            callback(CompletionResult.Success(Unit))
            return
        }
        Log.session { "Request user code of type: $type" }

        val cardId = environment.card?.cardId ?: this.cardId
        val showForgotButton = environment.card?.backupStatus?.isActive ?: false
        val formattedCardId = cardId?.let {
            CardIdFormatter(environment.config.cardIdDisplayFormat).getFormattedCardId(it)
        }

        viewDelegate.requestUserCode(
            type, isFirstAttempt,
            showForgotButton = showForgotButton, cardId = formattedCardId,
        ) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    val code = UserCode(type, result.data)
                    when (type) {
                        UserCodeType.AccessCode -> environment.accessCode = code
                        UserCodeType.Passcode -> environment.passcode = code
                    }
                    callback(CompletionResult.Success(Unit))
                }
                is CompletionResult.Failure -> {
                    if (result.error is TangemSdkError.UserForgotTheCode) {
                        viewDelegate.dismiss()
                        restoreUserCode(type, cardId) { restoreCodeResult ->
                            when (restoreCodeResult) {
                                is CompletionResult.Success -> {
                                    updateEnvironment(type, restoreCodeResult.data)
                                    resetCodesController = null
                                    callback(CompletionResult.Success(Unit))
                                }
                                is CompletionResult.Failure -> {
                                    callback(CompletionResult.Failure(restoreCodeResult.error))
                                }
                            }
                        }
                    } else {
                        callback(CompletionResult.Failure(result.error))
                    }
                }
            }
        }
    }

    private fun updateEnvironment(type: UserCodeType, code: String) {
        val userCode = UserCode(type, code)
        when (type) {
            UserCodeType.AccessCode -> environment.accessCode = userCode
            UserCodeType.Passcode -> environment.passcode = userCode
        }
    }

    fun restoreUserCode(type: UserCodeType, cardId: String?, callback: CompletionCallback<String>) {
        val sessionBuilder = TangemSdk.makeSessionBuilder(
            viewDelegate = viewDelegate,
            secureStorage = secureStorage,
            reader = reader,
            jsonRpcConverter = jsonRpcConverter
        )
        val resetService = ResetPinService(
            sessionBuilder = sessionBuilder,
            stringsLocator = viewDelegate.resetCodesViewDelegate.stringsLocator,
            config = environment.config
        )
        viewDelegate.resetCodesViewDelegate.stopSessionCallback = {
            stopSession()
            callback(CompletionResult.Failure(TangemSdkError.UserCancelled()))
        }
        resetCodesController = ResetCodesController(
            resetService = resetService,
            viewDelegate = viewDelegate.resetCodesViewDelegate
        ).apply {
            cardIdDisplayFormat = environment.config.cardIdDisplayFormat
            start(codeType = type, cardId = cardId, callback = callback)
        }
    }

    enum class CardSessionState {
        Inactive,
        Active
    }
}

typealias SessionStartedCallback = (session: CardSession, error: TangemError?) -> Unit

enum class TagType {
    Nfc,
    Slix
}

class SessionBuilder(
    val viewDelegate: SessionViewDelegate,
    val secureStorage: SecureStorage,
    val reader: CardReader,
    val jsonRpcConverter: JSONRPCConverter,
) {
    fun build(
        config: Config,
        cardId: String? = null,
        initialMessage: Message? = null
    ): CardSession {
        val environment = SessionEnvironment(config, secureStorage)
        return CardSession(
            viewDelegate = viewDelegate,
            environment = environment,
            reader = reader,
            jsonRpcConverter = jsonRpcConverter,
            cardId = cardId,
            initialMessage = initialMessage,
            secureStorage = secureStorage,
        )
    }
}


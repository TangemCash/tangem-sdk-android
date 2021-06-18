package com.tangem

import com.tangem.commands.*
import com.tangem.commands.read.ReadCommand
import com.tangem.common.CompletionResult
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.extensions.calculateSha256
import com.tangem.crypto.EncryptionHelper
import com.tangem.crypto.pbkdf2Hash
import com.tangem.tasks.PreflightReadCapable
import com.tangem.tasks.PreflightReadSettings
import com.tangem.tasks.PreflightReadTask
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Basic interface for running tasks and [com.tangem.commands.Command] in a [CardSession]
 */
interface CardSessionRunnable<T : CommandResponse> {

    val requiresPin2: Boolean

    /**
     * The starting point for custom business logic.
     * Implement this interface and use [TangemSdk.startSessionWithRunnable] to run.
     * @param session run commands in this [CardSession].
     * @param callback trigger the callback to complete the task.
     */
    fun run(session: CardSession, callback: (result: CompletionResult<T>) -> Unit)
}

interface CardSessionPreparable {
    fun prepare(session: CardSession, callback: (result: CompletionResult<Unit>) -> Unit)
}

enum class CardSessionState {
    Inactive,
    Active
}

enum class TagType {
    Nfc,
    Slix
}

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
    private val environmentService: SessionEnvironmentService,
    private val reader: CardReader,
    val viewDelegate: SessionViewDelegate,
    private var cardId: String? = null,
    private var initialMessage: Message? = null
) {

    var connectedTag: TagType? = null

    val environment = environmentService.createEnvironment(cardId)
    val scope = CoroutineScope(Dispatchers.IO) + CoroutineExceptionHandler { _, ex ->
        val sw = StringWriter()
        ex.printStackTrace(PrintWriter(sw))
        Log.error { sw.toString() }
    }

    /**
     * True if some operation is still in progress.
     */
    private var state = CardSessionState.Inactive
    private var needPreflightRead = true
    private var pin2Required = false
    private var preflightReadingSettings: PreflightReadSettings = PreflightReadSettings.ReadCardOnly

    private val tag = this.javaClass.simpleName

    fun setInitialMessage(message: Message?) {
        initialMessage = message
        viewDelegate.setMessage(message)
    }

    /**
     * This method starts a card session, performs preflight [ReadCommand],
     * invokes [CardSessionRunnable.run] and closes the session.
     * @param runnable [CardSessionRunnable] that will be performed in the session.
     * @param callback will be triggered with a [CompletionResult] of a session.
     */
    fun <T : CardSessionRunnable<R>, R : CommandResponse> startWithRunnable(
        runnable: T, callback: (result: CompletionResult<R>) -> Unit
    ) {
        if (state != CardSessionState.Inactive) {
            callback(CompletionResult.Failure(TangemSdkError.Busy()))
            return
        }

        Log.session { "Start card session with runnable" }
        prepareSession(runnable) { prepareResult ->
            when (prepareResult) {
                is CompletionResult.Success -> {
                    start() { session, error ->
                        if (error != null) {
                            callback(CompletionResult.Failure(error))
                            return@start
                        }
                        Log.session { "Start runnable" }
                        runnable.run(this) { result ->
                            Log.session{ "Runnable completed" }
                            when (result) {
                                is CompletionResult.Success -> stop()
                                is CompletionResult.Failure -> stopWithError(result.error)
                            }
                            callback(result)
                        }
                    }
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(prepareResult.error))
            }

        }
    }

    private fun <T : CardSessionRunnable<*>> prepareSession(
        runnable: T, callback: (result: CompletionResult<Unit>) -> Unit
    ) {
        Log.session { "Prepare card session" }
        (runnable as? PreflightReadCapable)?.let {
            needPreflightRead = it.needPreflightRead()
            preflightReadingSettings = it.preflightReadSettings()
        }
        if (runnable is CardSessionPreparable) {
            runnable.prepare(this, callback)
        } else {
            callback(CompletionResult.Success(Unit))
        }
    }

    /**
     * Starts a card session and performs preflight [ReadCommand].
     * @param callback: callback with the card session. Can contain [TangemSdkError] if something goes wrong.
     */
    fun start(callback: (session: CardSession, error: TangemError?) -> Unit) {
        Log.session { "Start card session with delegate" }
        state = CardSessionState.Active
        viewDelegate.onSessionStarted(cardId, initialMessage, environmentService.howToIsEnabled())

        reader.scope = scope
        reader.startSession()

        scope.launch {
            reader.tag.asFlow()
                .filterNotNull()
                .take(1)
                .collect { tagType ->
                    if (tagType == TagType.Nfc && needPreflightRead) {
                        preflightCheck(callback)
                    } else {
                        callback(this@CardSession, null)
                    }
                }
        }

        scope.launch {
            reader.tag.asFlow()
                .collect {
                    if (it == null) {
                        viewDelegate.onTagLost()
                    } else {
                        viewDelegate.onTagConnected()
                    }
                }
        }

        scope.launch {
            reader.tag.asFlow()
                .onCompletion {
                    if (it is CancellationException && it.message == TangemSdkError.UserCancelled().customMessage) {
                        viewDelegate.dismiss()
                        callback(this@CardSession, TangemSdkError.UserCancelled())
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

    private fun preflightCheck(callback: (session: CardSession, error: TangemError?) -> Unit) {
        Log.session { "Start preflight check" }
        PreflightReadTask(preflightReadingSettings, cardId).run(this) { result ->
            when (result) {
                is CompletionResult.Failure -> {
                    val wrongValueType = WrongValueType.fromTangemError(result.error)
                    if (wrongValueType != null) {
                        viewDelegate.onWrongCard(wrongValueType)
                        preflightCheck(callback)
                        return@run
                    }
                    stopWithError(result.error)
                    callback(this, result.error)
                }
                is CompletionResult.Success -> {
                    val receivedCardId = result.data.cardId
                    environment.card = result.data
                    environmentService.updateEnvironment(environment, result.data.cardId)
                    cardId = receivedCardId
                    callback(this, null)
                }
            }
        }
    }

    fun readSlixTag(callback: (result: CompletionResult<ResponseApdu>) -> Unit) {
        reader.readSlixTag(callback)
    }

    /**
     * Stops the current session with the text message.
     * @param message If null, the default message will be shown.
     */
    private fun stop(message: Message? = null) {
        stopSession()
        viewDelegate.onSessionStopped(message)
    }

    /**
     * Stops the current session on error.
     * @param error An error that will be shown.
     */
    private fun stopWithError(error: TangemError) {
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
        preflightReadingSettings = PreflightReadSettings.ReadCardOnly
        environmentService.saveEnvironmentValues(environment, cardId)
        reader.stopSession()
        scope.cancel()
    }

    fun send(apdu: CommandApdu, callback: (result: CompletionResult<ResponseApdu>) -> Unit) {
        Log.session { "Send" }
        val subscription = reader.tag.openSubscription()
        scope.launch {
            subscription.consumeAsFlow()
                .filterNotNull()
                .map { establishEncryptionIfNeeded() }
                .map { apdu.encrypt(environment.encryptionMode, environment.encryptionKey) }
                .map { encryptedApdu -> reader.transceiveApdu(encryptedApdu) }
                .map { responseApdu -> decrypt(responseApdu) }
                .catch { if (it is TangemSdkError) callback(CompletionResult.Failure(it)) }
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
                                    subscription.cancel()
                                    callback(result)
                                }
                            }
                        }
                    }
                }
        }
    }

    fun pause() {
        reader.pauseSession()
    }

    fun resume() {
        reader.resumeSession()
    }

    private suspend fun establishEncryptionIfNeeded(): CompletionResult<Boolean> {
        Log.session { "Try establish encryption" }
        if (environment.encryptionMode == EncryptionMode.NONE || environment.encryptionKey != null) {
            return CompletionResult.Success(true)
        }

        val encryptionHelper = EncryptionHelper.create(environment.encryptionMode)
            ?: return CompletionResult.Success(true)

        val openSesssionCommand = OpenSessionCommand(encryptionHelper.keyA)
        val apdu = openSesssionCommand.serialize(environment)

        val response = reader.transceiveApdu(apdu)
        when (response) {
            is CompletionResult.Success -> {
                val result = try {
                    openSesssionCommand.deserialize(environment, response.data)
                } catch (error: TangemSdkError) {
                    return CompletionResult.Failure(error)
                }
                val uid = result.uid
                val protocolKey = environment.pin1!!.value.pbkdf2Hash(uid, 50)
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
                    return CompletionResult.Failure(error)
                }
            }
            is CompletionResult.Failure -> result
        }
    }
}

package com.tangem.operations.resetcode

import com.tangem.Message
import com.tangem.TangemSdk
import com.tangem.common.CompletionResult
import com.tangem.common.StringsLocator
import com.tangem.common.UserCodeType
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.Config
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.guard
import com.tangem.operations.CommandResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ResetPinService(
    private val stringsLocator: StringsLocator,
    private val config: Config
) {

    private var repo: ResetPinRepo = ResetPinRepo()

    var currentState: State = State.NeedCode
        private set(value) {
            _currentStateFlow.value = value
            field = value
        }

    private var _currentStateFlow: MutableStateFlow<State> =
        MutableStateFlow(currentState)
    val currentStateAsFlow: StateFlow<State>
        get() = _currentStateFlow

    var error: TangemError? = null
        private set(value) {
            _errorFlow.value = value
            field = value
        }

    private var _errorFlow: MutableStateFlow<TangemError?> = MutableStateFlow(null)
    val errorAsFlow: StateFlow<TangemError?>
        get() = _errorFlow

    private val handleErrors = config.handleErrors

    fun setAccessCode(code: String): CompletionResult<Unit> {
        if (handleErrors) {
            if (code.isEmpty()) {
                return CompletionResult.Failure(TangemSdkError.AccessCodeRequired())
            }
            if (code == UserCodeType.AccessCode.defaultValue) {
                return CompletionResult.Failure(TangemSdkError.AccessCodeCannotBeChanged())
            }
        }

        repo = repo.copy(accessCode = code.calculateSha256())
        currentState = currentState.next()
        return CompletionResult.Success(Unit)
    }

    fun setPasscode(code: String): CompletionResult<Unit> {
        if (handleErrors) {
            if (code.isEmpty()) {
                return CompletionResult.Failure(TangemSdkError.PasscodeRequired())
            }
            if (code == UserCodeType.Passcode.defaultValue) {
                return CompletionResult.Failure(TangemSdkError.PasscodeCannotBeChanged())
            }
        }
        repo = repo.copy(passcode = code.calculateSha256())
        return CompletionResult.Success(Unit)
    }


    fun proceed(resetCardId: String? = null) {
        when (currentState) {
            State.NeedScanResetCard -> scanResetPinCard(resetCardId) { handleCompletion(it) }
            State.NeedScanConfirmationCard -> scanConfirmationCard { handleCompletion(it) }
            State.NeedWriteResetCard -> writeResetPinCard { handleCompletion(it) }
            State.Finished -> {
            }
        }
    }

    private fun handleCompletion(result: CompletionResult<Unit>) {
        when (result) {
            is CompletionResult.Success -> currentState = currentState.next()
            is CompletionResult.Failure -> this.error = result.error
        }
    }


    private fun scanResetPinCard(resetCardId: String?, callback: CompletionCallback<Unit>) {
        val pinType = when {
            repo.accessCode != null -> stringsLocator.getString(StringsLocator.ID.pin1)
            repo.passcode != null -> stringsLocator.getString(StringsLocator.ID.pin2)
            else -> {
                callback(CompletionResult.Failure(TangemSdkError.UnknownError()))
                return
            }
        }

        val command = GetResetPinTokenCommand()
        TangemSdk.makeSession(
            config = config,
            cardId = resetCardId,
            initialMessage = Message(
                header = stringsLocator.getString(
                    StringsLocator.ID.reset_codes_scan_first_card,
                    pinType
                )
            )
        ).startWithRunnable(runnable = command) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    repo = repo.copy(resetPinCard = result.data)
                    callback(CompletionResult.Success(Unit))
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }

    private fun scanConfirmationCard(callback: CompletionCallback<Unit>) {
        val resetPinCard = repo.resetPinCard.guard {
            callback(CompletionResult.Failure(TangemSdkError.UnknownError()))
            return
        }
        val command = SignResetPinTokenCommand(resetPinCard)
        TangemSdk.makeSession(
            config = config,
            initialMessage = Message(
                header = stringsLocator.getString(
                    StringsLocator.ID.reset_codes_scan_confirmation_card
                )
            )
        ).startWithRunnable(
            runnable = command
        ) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    repo = repo.copy(confirmationCard = result.data)
                    callback(CompletionResult.Success(Unit))
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }

        }
    }

    private fun writeResetPinCard(callback: CompletionCallback<Unit>) {
        val resetPinCard = repo.resetPinCard.guard {
            callback(CompletionResult.Failure(TangemSdkError.UnknownError()))
            return
        }
        val confirmationCard = repo.confirmationCard.guard {
            callback(CompletionResult.Failure(TangemSdkError.UnknownError()))
            return
        }

        var accessCode = repo.accessCode
        var passcode = repo.passcode

        if (handleErrors) {
            if (!resetPinCard.isAccessCodeSet) {
                accessCode = UserCodeType.AccessCode.defaultValue.calculateSha256()
            }

            if (!resetPinCard.isPasscodeSet) {
                passcode = UserCodeType.Passcode.defaultValue.calculateSha256()
            }
        }

        val accessCodeUnwrapped = accessCode.guard {
            callback(CompletionResult.Failure(TangemSdkError.AccessCodeRequired()))
            return
        }

        val passcodeUnwrapped = passcode.guard {
            callback(CompletionResult.Failure(TangemSdkError.PasscodeRequired()))
            return
        }


        val command = ResetPinTask(confirmationCard, accessCodeUnwrapped, passcodeUnwrapped)
        TangemSdk.makeSession(
            config = config,
            cardId = resetPinCard.cardId,
            initialMessage = Message(
                header = stringsLocator.getString(
                    StringsLocator.ID.reset_codes_scan_to_reset
                )
            )
        ).startWithRunnable(
            runnable = command,
        ) { result ->
            when (result) {
                is CompletionResult.Success -> callback(CompletionResult.Success(Unit))
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }

        }
    }

    enum class State {
        NeedCode {
            override fun next() = NeedScanResetCard
        },
        NeedScanResetCard {
            override fun next() = NeedScanConfirmationCard
        },
        NeedScanConfirmationCard {
            override fun next() = NeedWriteResetCard
        },
        NeedWriteResetCard {
            override fun next() = Finished
        },
        Finished {
            override fun next() = Finished
        },
        ;

        abstract fun next(): State

        fun getMessageTitle(stringsLocator: StringsLocator): String {
            return when (this) {
                NeedScanResetCard, NeedWriteResetCard -> stringsLocator.getString(StringsLocator.ID.reset_codes_message_title_restore)
                NeedScanConfirmationCard -> stringsLocator.getString(StringsLocator.ID.reset_codes_message_title_backup)
                NeedCode -> ""
                Finished -> stringsLocator.getString(StringsLocator.ID.common_success)
            }
        }

        fun getMessageBody(stringsLocator: StringsLocator): String {
            return when (this) {
                NeedScanResetCard -> stringsLocator.getString(StringsLocator.ID.reset_codes_message_body_restore)
                NeedWriteResetCard -> stringsLocator.getString(StringsLocator.ID.reset_codes_message_body_restore_final)
                NeedScanConfirmationCard -> stringsLocator.getString(StringsLocator.ID.reset_codes_message_body_backup)
                NeedCode -> ""
                Finished -> stringsLocator.getString(StringsLocator.ID.reset_codes_success_message)
            }
        }

    }
}


data class ResetPinRepo(
    val confirmationCard: ConfirmationCard? = null,
    val resetPinCard: ResetPinCard? = null,
    val accessCode: ByteArray? = null,
    val passcode: ByteArray? = null,
)

class ResetPinCard(
    val cardId: String,
    val backupKey: ByteArray,
    val attestSignature: ByteArray,
    val token: ByteArray,
    val isAccessCodeSet: Boolean,
    val isPasscodeSet: Boolean,
) : CommandResponse


class ConfirmationCard(
    val cardId: String,
    val backupKey: ByteArray,
    val salt: ByteArray,
    val authorizeSignature: ByteArray,
) : CommandResponse

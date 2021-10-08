package com.tangem.operations.pins

import com.tangem.common.CompletionResult
import com.tangem.common.SuccessResponse
import com.tangem.common.UserCodeType
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.apdu.StatusWord
import com.tangem.common.core.CardSession
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.SessionEnvironment
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag
import com.tangem.operations.Command

class SetUserCodeCommand private constructor() : Command<SuccessResponse>() {

    override fun requiresPasscode(): Boolean = isPasscodeRequire

    internal var shouldRestrictDefaultCodes = true
    internal var isPasscodeRequire = true

    private var codes = mutableMapOf<UserCodeType, UserCodeAction>()

    override fun prepare(session: CardSession, callback: CompletionCallback<Unit>) {
        requestIfNeeded(UserCodeType.AccessCode, session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    requestIfNeeded(UserCodeType.Passcode, session) { result ->
                        when (result) {
                            is CompletionResult.Success -> callback(CompletionResult.Success(Unit))
                            is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
                        }
                    }
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }

    override fun run(session: CardSession, callback: CompletionCallback<SuccessResponse>) {
        if (codes.values.contains(UserCodeAction.Request)) { //If prepare not called e.g. chaining
            val error = getPauseError(session.environment)
            if (error == null) {
                session.pause()
            } else {
                session.pause(error)
            }

            prepare(session) { result ->
                when (result) {
                    is CompletionResult.Success -> {
                        session.resume()
                        runCommand(session, callback)
                    }
                    is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
                }
            }
        } else {
            runCommand(session, callback)
        }
    }

    private fun runCommand(session: CardSession, callback: CompletionCallback<SuccessResponse>) {
        //Restrict default codes except reset command
        if (shouldRestrictDefaultCodes && (!isCodeAllowed(UserCodeType.AccessCode)
                        || !isCodeAllowed(UserCodeType.Passcode))) {
            callback(CompletionResult.Failure(TangemSdkError.PasscodeCannotBeChanged()))
        } else {
            super.run(session, callback)
        }
    }

    private fun isCodeAllowed(type: UserCodeType): Boolean {
        return !codes[type]?.value.contentEquals(type.defaultValue.calculateSha256())
    }

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.Pin, environment.accessCode.value)
        tlvBuilder.append(TlvTag.Pin2, environment.passcode.value)
        tlvBuilder.append(TlvTag.CardId, environment.card?.cardId)
        tlvBuilder.append(TlvTag.NewPin, codes[UserCodeType.AccessCode]?.value ?: environment.accessCode.value)
        tlvBuilder.append(TlvTag.NewPin2, codes[UserCodeType.Passcode]?.value ?: environment.passcode.value)
        tlvBuilder.append(TlvTag.Cvc, environment.cvc)

        return CommandApdu(Instruction.SetPin, tlvBuilder.serialize())
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): SuccessResponse {
        val tlvData = apdu.getTlvData(environment.encryptionKey) ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)
        return SuccessResponse(decoder.decode(TlvTag.CardId))
    }

    private fun getPauseError(environment: SessionEnvironment): TangemSdkError? {
        val filtered = codes.filter { it.value == UserCodeAction.Request }
        val type = filtered.keys.firstOrNull() ?: return null

        return TangemSdkError.from(type, environment)
    }

    private fun requestIfNeeded(type: UserCodeType, session: CardSession, callback: CompletionCallback<Unit>) {
        if (codes[type] != UserCodeAction.Request) {
            callback(CompletionResult.Success(Unit))
            return
        }

        session.viewDelegate.requestUserCodeChange(type) { result ->
            if (result == null) {
                callback(CompletionResult.Failure(TangemSdkError.UserCancelled()))
            } else {
                codes[type] = UserCodeAction.StringValue(result)
                callback(CompletionResult.Success(Unit))
            }
        }
    }

    companion object {
        fun changeAccessCode(accessCode: String?): SetUserCodeCommand {
            return SetUserCodeCommand().apply {
                codes[UserCodeType.AccessCode] = accessCode?.let { UserCodeAction.StringValue(it) }
                        ?: UserCodeAction.Request
                codes[UserCodeType.Passcode] = UserCodeAction.NotChange
            }
        }

        fun changePasscode(passcode: String?): SetUserCodeCommand {
            return SetUserCodeCommand().apply {
                codes[UserCodeType.AccessCode] = UserCodeAction.NotChange
                codes[UserCodeType.Passcode] = passcode?.let { UserCodeAction.StringValue(it) }
                        ?: UserCodeAction.Request
            }
        }

        fun change(accessCode: String?, passcode: String?): SetUserCodeCommand {
            return change(accessCode?.calculateSha256(), passcode?.calculateSha256())
        }

        fun change(accessCode: ByteArray?, passcode: ByteArray?): SetUserCodeCommand {
            return SetUserCodeCommand().apply {
                codes[UserCodeType.AccessCode] = accessCode?.let { UserCodeAction.Value(it) }
                        ?: UserCodeAction.Request
                codes[UserCodeType.Passcode] = passcode?.let { UserCodeAction.Value(it) }
                        ?: UserCodeAction.Request
            }
        }

        fun resetAccessCodeCommand(): SetUserCodeCommand {
            return changeAccessCode(UserCodeType.AccessCode.defaultValue).apply {
                shouldRestrictDefaultCodes = false
            }
        }

        fun resetPasscodeCommand(): SetUserCodeCommand {
            return changePasscode(UserCodeType.Passcode.defaultValue).apply {
                shouldRestrictDefaultCodes = false
            }
        }

        fun resetUserCodes(): SetUserCodeCommand {
            return change(UserCodeType.AccessCode.defaultValue, UserCodeType.Passcode.defaultValue).apply {
                shouldRestrictDefaultCodes = false
            }
        }
    }

    sealed class UserCodeAction(val value: ByteArray?) {
        object Request : UserCodeAction(null)
        class StringValue(value: String) : UserCodeAction(value.calculateSha256())
        class Value(data: ByteArray) : UserCodeAction(data)
        object NotChange : UserCodeAction(null)
    }
}

enum class SetPinStatus {
    PinsNotChanged,
    Pin1Changed,
    Pin2Changed,
    Pin3Changed,
    Pins12Changed,
    Pins13Changed,
    Pins23Changed,
    Pins123Changed,
    ;

    companion object {
        fun fromStatusWord(statusWord: StatusWord): SetPinStatus? {
            return when (statusWord) {
                StatusWord.ProcessCompleted -> PinsNotChanged
                StatusWord.Pin1Changed -> Pin1Changed
                StatusWord.Pin2Changed -> Pin2Changed
                StatusWord.Pins12Changed -> Pins12Changed
                StatusWord.Pin3Changed -> Pin3Changed
                StatusWord.Pins13Changed -> Pins13Changed
                StatusWord.Pins23Changed -> Pins23Changed
                StatusWord.Pins123Changed -> Pins123Changed
                else -> null
            }
        }
    }
}
package com.tangem.operations

import com.tangem.*
import com.tangem.common.*
import com.tangem.common.apdu.*
import com.tangem.common.card.Card
import com.tangem.common.card.EncryptionMode
import com.tangem.common.core.*
import com.tangem.common.extensions.titleFormatted
import com.tangem.common.extensions.toInt
import com.tangem.common.tlv.Tlv
import com.tangem.common.tlv.TlvTag
import okhttp3.internal.toHexString
import java.util.*

/**
 * Basic interface for a parsed response from [Command].
 */
interface CommandResponse

interface ApduSerializable<T : CommandResponse> {
    /**
     * Serializes data into an array of [com.tangem.common.tlv.Tlv],
     * then creates [CommandApdu] with this data.
     * @param environment [SessionEnvironment] of the current card
     * @return command data converted to [CommandApdu] that allows to convert it to [ByteArray]
     * that can be sent to a Tangem card
     */
    fun serialize(environment: SessionEnvironment): CommandApdu

    /**
     * Deserializes data received from a card and stored in [ResponseApdu]
     * into an array of [com.tangem.common.tlv.Tlv]. Then maps it into a [CommandResponse].
     * @param environment [SessionEnvironment] of the current card.
     * @param apdu received data.
     * @return Card response converted to a [CommandResponse] of a type [T]
     */
    fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): T
}

/**
 * Basic class for Tangem card commands
 */
abstract class Command<T : CommandResponse> : ApduSerializable<T>, CardSessionRunnable<T> {

    open fun requiresPasscode(): Boolean = false

    override fun run(session: CardSession, callback: CompletionCallback<T>) {
        Log.command(this)
        transceive(session, callback)
    }

    protected open fun performPreCheck(card: Card): TangemError? = null

    open fun mapError(card: Card?, error: TangemError): TangemError = error

    fun transceive(session: CardSession, callback: CompletionCallback<T>) {
        val card = session.environment.card
        if (preflightReadMode() != PreflightReadMode.None && card == null) {
            callback(CompletionResult.Failure(TangemSdkError.MissingPreflightRead()))
            return
        }
        if (session.environment.config.handleErrors && card != null) {
            performPreCheck(card)?.let { error ->
                callback(CompletionResult.Failure(error))
                return
            }
        }
        if (session.environment.passcode.value == null && requiresPasscode()) {
            requestPin(UserCodeType.Passcode, session, callback)
        } else {
            transceiveInternal(session, callback)
        }
    }

    private fun transceiveInternal(session: CardSession, callback: CompletionCallback<T>) {
        Log.apdu { "C-APDU serialization start".titleFormatted() }
        val apdu = serialize(session.environment)
        Log.apdu { "C-APDU serialization finish".titleFormatted() }
        showMissingSecurityDelay(session)
        transceiveApdu(apdu, session) { result ->
            when (result) {
                is CompletionResult.Failure -> {
                    if (result.error is TangemSdkError.ExtendedLengthNotSupported) {
                        if (session.environment.terminalKeys != null) {
                            session.environment.terminalKeys = null
                            transceiveInternal(session, callback)
                            return@transceiveApdu
                        }
                    }
                    if (session.environment.config.handleErrors) {
                        val mappedError = mapError(session.environment.card, result.error)
                        if (mappedError is TangemSdkError.AccessCodeRequired) {
                            requestPin(UserCodeType.AccessCode, session, callback)
                        } else if (mappedError is TangemSdkError.InvalidParams) {
                            if (requiresPasscode()) {
                                //Addition check for COS v4 and newer to prevent false-positive pin2 request
                                val isPasscodeSet = session.environment.card?.isPasscodeSet == false
                                if (isPasscodeSet && !session.environment.isUserCodeSet(UserCodeType.Passcode)) {
                                    callback(CompletionResult.Failure(mappedError))
                                } else {
                                    requestPin(UserCodeType.Passcode, session, callback)
                                }
                            } else {
                                callback(CompletionResult.Failure(mappedError))
                            }
                        } else {
                            callback(CompletionResult.Failure(mappedError))
                        }
                        return@transceiveApdu
                    }
                    callback(CompletionResult.Failure(result.error))
                }
                is CompletionResult.Success -> {
                    try {
                        Log.apdu { "R-APDU deserialization start".titleFormatted() }
                        val response = deserialize(session.environment, result.data)
                        Log.apdu { "R-APDU deserialization finish".titleFormatted() }
                        callback(CompletionResult.Success(response))
                    } catch (error: TangemSdkError) {
                        callback(CompletionResult.Failure(error))
                    }
                }
            }
        }
    }

    private fun transceiveApdu(apdu: CommandApdu, session: CardSession, callback: CompletionCallback<ResponseApdu>) {
        session.send(apdu) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    val responseApdu = result.data

                    when (responseApdu.statusWord) {
                        StatusWord.ProcessCompleted,
                        StatusWord.Pin1Changed, StatusWord.Pin2Changed, StatusWord.Pins12Changed,
                        StatusWord.Pin3Changed, StatusWord.Pins13Changed, StatusWord.Pins23Changed,
                        StatusWord.Pins123Changed -> {
                            callback(CompletionResult.Success(responseApdu))
                        }
                        StatusWord.NeedPause -> {
                            // NeedPause is returned from the card whenever security delay is triggered.
                            val remainingTime = deserializeSecurityDelay(session.environment, responseApdu)
                            if (remainingTime != null) {
                                session.viewDelegate.onSecurityDelay(
                                        remainingTime,
                                        session.environment.card?.settings?.securityDelay ?: 0
                                )
                            }
                            transceiveApdu(apdu, session, callback)
                        }
                        StatusWord.NeedEncryption -> {
                            when (session.environment.encryptionMode) {
                                EncryptionMode.None -> {
                                    Log.session { "Try change to fast encryption" }
                                    session.environment.encryptionKey = null
                                    session.environment.encryptionMode = EncryptionMode.Fast
                                }
                                EncryptionMode.Fast -> {
                                    Log.session { "Try change to strong encryption" }
                                    session.environment.encryptionKey = null
                                    session.environment.encryptionMode = EncryptionMode.Strong
                                }
                                EncryptionMode.Strong -> {
                                    callback(CompletionResult.Failure(TangemSdkError.NeedEncryption()))
                                    return@send
                                }
                            }
                            transceiveApdu(apdu, session, callback)
                        }
                        StatusWord.Unknown -> {
                            callback(CompletionResult.Failure(TangemSdkError.UnknownStatus(responseApdu.sw.toHexString())))
                        }
                        else -> {
                            val error = responseApdu.statusWord.toTangemSdkError()
                            if (error != null) {
                                callback(CompletionResult.Failure(error))
                            } else {
                                callback(CompletionResult.Failure(TangemSdkError.UnknownError()))
                            }
                        }
                    }
                }
                is CompletionResult.Failure ->
                    if (result.error is TangemSdkError.TagLost) {
                        session.viewDelegate.onTagLost()
                    } else {
                        callback(CompletionResult.Failure(result.error))
                    }
            }
        }
    }

    @Deprecated("Used to fix lack of security delay on cards with firmware version below 1.21")
    private fun showMissingSecurityDelay(session: CardSession) {
        if (!session.environment.enableMissingSecurityDelay) return

        var totalDuration = session.environment.card!!.settings.securityDelay
        totalDuration = if (totalDuration == 0) 0 else totalDuration + (totalDuration / 3)
        session.viewDelegate.onSecurityDelay(SessionEnvironment.missingSecurityDelayCode, totalDuration)
    }

    /**
     * Helper method to parse security delay information received from a card.
     *
     * @return Remaining security delay in milliseconds.
     */
    private fun deserializeSecurityDelay(environment: SessionEnvironment, responseApdu: ResponseApdu): Int? {
        val tlv = responseApdu.getTlvData(environment.encryptionKey)
        return tlv?.find { it.tag == TlvTag.Pause }?.value?.toInt()
    }

    private fun requestPin(type: UserCodeType, session: CardSession, callback: CompletionCallback<T>) {
        session.pause(TangemSdkError.from(type, session.environment))
        val isFirstAttempt = type.isWrongPinEntered(session.environment)
        when (type) {
            UserCodeType.AccessCode -> session.environment.accessCode = UserCode(UserCodeType.AccessCode, null)
            UserCodeType.Passcode -> session.environment.passcode = UserCode(UserCodeType.Passcode, null)
        }

        Log.session { "Request pin of type: $type" }
        session.requestUserCodeIfNeeded(type, isFirstAttempt) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    session.resume()
                    transceiveInternal(session, callback)
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }

    protected fun deserializeApdu(environment: SessionEnvironment, apdu: ResponseApdu): List<Tlv> {
        return apdu.getTlvData(environment.encryptionKey) ?: throw TangemSdkError.DeserializeApduFailed()
    }
}
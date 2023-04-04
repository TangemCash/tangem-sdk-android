package com.tangem.common.apdu

import com.tangem.common.core.TangemSdkError

/**
 * Part of a response from the card, shows the status of the operation
 */
enum class StatusWord(val code: Int) {

    ProcessCompleted(code = 0x9000),
    InvalidParams(code = 0x6A86),
    ErrorProcessingCommand(code = 0x6286),
    InvalidState(code = 0x6985),

    // PinsNotChanged(0x9000) is equal to ProcessCompleted(0x9000)
    Pin1Changed(code = 0x9001),
    Pin2Changed(code = 0x9002),
    Pins12Changed(code = 0x9003),
    Pin3Changed(code = 0x9004),
    Pins13Changed(code = 0x9005),
    Pins23Changed(code = 0x9006),
    Pins123Changed(code = 0x9007),

    InsNotSupported(code = 0x6D00),
    NeedEncryption(code = 0x6982),
    NeedPause(code = 0x9789),

    FileNotFound(code = 0x6A82),
    WalletNotFound(code = 0x6A88),
    InvalidAccessCode(code = 0x6AF1),
    InvalidPasscode(code = 0x6AF2),

    Unknown(code = 0x0000),
    ;

    companion object {
        private val values = values()
        fun byCode(code: Int): StatusWord = values.find { it.code == code } ?: Unknown
    }
}

fun StatusWord.toTangemSdkError(): TangemSdkError? {
    return when (this) {
        StatusWord.ProcessCompleted, StatusWord.Pin1Changed,
        StatusWord.Pin2Changed, StatusWord.Pins12Changed, StatusWord.Pin3Changed,
        StatusWord.Pins13Changed, StatusWord.Pins23Changed, StatusWord.Pins123Changed,
        -> null
        StatusWord.NeedPause -> null
        StatusWord.InvalidParams -> TangemSdkError.InvalidParams()
        StatusWord.ErrorProcessingCommand -> TangemSdkError.ErrorProcessingCommand()
        StatusWord.InvalidState -> TangemSdkError.InvalidState()
        StatusWord.InsNotSupported -> TangemSdkError.InsNotSupported()
        StatusWord.NeedEncryption -> TangemSdkError.NeedEncryption()
        StatusWord.FileNotFound -> TangemSdkError.FileNotFound()
        StatusWord.WalletNotFound -> TangemSdkError.WalletNotFound()
        StatusWord.InvalidAccessCode -> TangemSdkError.AccessCodeRequired()
        StatusWord.InvalidPasscode -> TangemSdkError.PasscodeRequired()
        else -> null
    }
}

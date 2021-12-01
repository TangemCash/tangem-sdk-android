package com.tangem.common.apdu

import com.tangem.common.core.TangemSdkError

/**
 * Part of a response from the card, shows the status of the operation
 */
enum class StatusWord(val code: Int) {

    ProcessCompleted(0x9000),
    InvalidParams(0x6A86),
    ErrorProcessingCommand(0x6286),
    InvalidState(0x6985),

    //PinsNotChanged(0x9000) is equal to ProcessCompleted(0x9000)
    Pin1Changed(0x9001),
    Pin2Changed(0x9002),
    Pins12Changed(0x9003),
    Pin3Changed(0x9004),
    Pins13Changed(0x9005),
    Pins23Changed(0x9006),
    Pins123Changed(0x9007),

    InsNotSupported(0x6D00),
    NeedEncryption(0x6982),
    NeedPause(0x9789),

    FileNotFound(0x6A82),
    WalletNotFound(0x6A88),

    InvalidAccessCode(0x6AF1),
    InvalidPasscode(0x6AF2),

    Unknown(0x0000);

    companion object {
        private val values = values()
        fun byCode(code: Int): StatusWord = values.find { it.code == code } ?: Unknown
    }
}

fun StatusWord.toTangemSdkError(): TangemSdkError? {
    return when (this) {
        StatusWord.ProcessCompleted, StatusWord.Pin1Changed,
        StatusWord.Pin2Changed, StatusWord.Pins12Changed, StatusWord.Pin3Changed,
        StatusWord.Pins13Changed, StatusWord.Pins23Changed, StatusWord.Pins123Changed -> null
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


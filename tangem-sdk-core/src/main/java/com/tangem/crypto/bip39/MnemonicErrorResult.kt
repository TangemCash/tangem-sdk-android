package com.tangem.crypto.bip39

sealed class MnemonicErrorResult {
    object InvalidWordCount : MnemonicErrorResult()
    object InvalidEntropyLength : MnemonicErrorResult()
    object InvalidWordsFile : MnemonicErrorResult()
    object InvalidChecksum : MnemonicErrorResult()
    object MnenmonicCreationFailed : MnemonicErrorResult()
    object NormalizationFailed : MnemonicErrorResult()
    object UnsupportedLanguage : MnemonicErrorResult()
    data class InvalidWords(val words: Set<String>) : MnemonicErrorResult()
}
package com.tangem.crypto.bip39

sealed class ValidateMnemonicResult {

    object Success : ValidateMnemonicResult()
    object InvalidWordCount : ValidateMnemonicResult()
    object InvalidEntropyLength : ValidateMnemonicResult()
    object InvalidWordsFile : ValidateMnemonicResult()
    object InvalidChecksum : ValidateMnemonicResult()
    object MnenmonicCreationFailed : ValidateMnemonicResult()
    object NormalizationFailed : ValidateMnemonicResult()
    object UnsupportedLanguage : ValidateMnemonicResult()
    data class InvalidWords(val words: Set<String>) : ValidateMnemonicResult()
}
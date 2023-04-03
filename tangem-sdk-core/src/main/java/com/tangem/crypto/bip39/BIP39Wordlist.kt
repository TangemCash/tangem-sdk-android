package com.tangem.crypto.bip39

import com.tangem.common.core.TangemSdkError
import java.io.IOException
import java.io.InputStream

class BIP39Wordlist(wordlistStream: InputStream) : Wordlist {

    override val words: List<String>

    init {
        val parsedWordlist = mutableListOf<String>()
        val bufferedReader = wordlistStream.bufferedReader()
        try {
            bufferedReader.lineSequence().forEach {
                parsedWordlist.add(it)
            }
        } catch (e: IOException) {
            throw TangemSdkError.MnemonicException(MnemonicErrorResult.InvalidWordsFile)
        }
        validateDictionarySize(parsedWordlist)
        words = parsedWordlist
    }

    @Throws(TangemSdkError.MnemonicException::class)
    private fun validateDictionarySize(dictionary: List<String>) {
        if (dictionary.size != DICTIONARY_SIZE) {
            throw TangemSdkError.MnemonicException(MnemonicErrorResult.InvalidWordsFile)
        }
    }

    companion object {
        private const val DICTIONARY_SIZE = 2048
    }
}
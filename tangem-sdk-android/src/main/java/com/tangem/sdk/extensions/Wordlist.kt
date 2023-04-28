package com.tangem.sdk.extensions

import android.content.Context
import com.tangem.crypto.bip39.BIP39Wordlist
import com.tangem.crypto.bip39.Wordlist

fun Wordlist.Companion.getWordlist(context: Context): Wordlist {
    return context.assets.open(DICTIONARY_FILE_NAME).use { inputStream ->
        BIP39Wordlist(inputStream)
    }
}

private const val DICTIONARY_FILE_NAME = "mnemonic_dictionary_en.txt"

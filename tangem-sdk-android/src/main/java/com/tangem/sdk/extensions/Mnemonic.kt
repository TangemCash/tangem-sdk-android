package com.tangem.sdk.extensions

import android.content.Context
import com.tangem.crypto.bip39.BIP39Wordlist
import com.tangem.crypto.bip39.DefaultMnemonic
import com.tangem.crypto.bip39.Mnemonic

/**
 * Default init method for [Mnemonic].
 * @param mnemonic BIP39 seed phrase (mnemonic)
 * @param context to open dictionary file
 */
fun Mnemonic.Companion.initDefault(mnemonic: String, context: Context): Mnemonic {
    val wordlist = context.assets.open(DICTIONARY_FILE_NAME).use { inputStream ->
        BIP39Wordlist(inputStream)
    }
    return DefaultMnemonic(mnemonic = mnemonic, wordlist = wordlist)
}

private const val DICTIONARY_FILE_NAME = "mnemonic_dictionary_en.txt"

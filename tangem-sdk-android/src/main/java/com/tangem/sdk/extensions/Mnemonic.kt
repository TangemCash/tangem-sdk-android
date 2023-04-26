package com.tangem.sdk.extensions

import android.content.Context
import com.tangem.crypto.bip39.DefaultMnemonic
import com.tangem.crypto.bip39.Mnemonic
import com.tangem.crypto.bip39.Wordlist

/**
 * Default init method for [Mnemonic].
 * @param mnemonic BIP39 seed phrase (mnemonic)
 * @param context to open dictionary file
 */
fun Mnemonic.Companion.initDefault(mnemonic: String, context: Context): Mnemonic {
    return DefaultMnemonic(mnemonic = mnemonic, wordlist = Wordlist.getWordlist(context))
}

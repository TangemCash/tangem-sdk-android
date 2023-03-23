package com.tangem.crypto.bip39

interface BIP39 {

    fun validate(mnemonicComponents: List<String>): ValidateMnemonicResult

    fun getWordlist(): Wordlist

    fun parse(mnemonicString: String): Wordlist
}
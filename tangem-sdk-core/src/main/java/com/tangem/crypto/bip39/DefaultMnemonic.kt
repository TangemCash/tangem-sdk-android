package com.tangem.crypto.bip39

class DefaultMnemonic : Mnemonic {

    val mnemonicComponents: List<String>
    val wordlist: Wordlist

    @Suppress("UnusedPrivateMember")
    constructor(mnemonic: String) {
        mnemonicComponents = emptyList()
        wordlist = BIP39Wordlist()
    }

    @Suppress("UnusedPrivateMember")
    constructor(entropy: EntropyLength, wordlist: Wordlist) {
        mnemonicComponents = emptyList()
        this.wordlist = wordlist
    }

    override fun generateSeed(passphrase: String): ByteArray {
        TODO("Not yet implemented")
    }
}
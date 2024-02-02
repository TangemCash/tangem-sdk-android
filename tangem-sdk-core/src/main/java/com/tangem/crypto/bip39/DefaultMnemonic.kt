package com.tangem.crypto.bip39

import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError

class DefaultMnemonic : Mnemonic {

    override val mnemonicComponents: List<String>
    override val wordlist: Wordlist

    private val bip39: DefaultBIP39

    @Throws(TangemSdkError.MnemonicException::class)
    constructor(mnemonic: String, wordlist: Wordlist) {
        bip39 = DefaultBIP39(wordlist)
        this.wordlist = bip39.wordlist
        mnemonicComponents = bip39.parse(mnemonic)
    }

    @Throws(TangemSdkError.MnemonicException::class)
    constructor(entropy: EntropyLength, wordlist: Wordlist) {
        bip39 = DefaultBIP39(wordlist)
        this.wordlist = bip39.wordlist
        mnemonicComponents = bip39.generateMnemonic(entropy)
    }

    override fun generateSeed(passphrase: String): CompletionResult<ByteArray> {
        return bip39.generateSeed(mnemonicComponents, passphrase)
    }

    @Throws(TangemSdkError.MnemonicException::class)
    override fun getEntropy(): ByteArray = bip39.getEntropy(mnemonicComponents)
}

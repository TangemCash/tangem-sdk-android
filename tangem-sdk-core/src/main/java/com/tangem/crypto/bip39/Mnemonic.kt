package com.tangem.crypto.bip39

import com.tangem.common.CompletionResult

interface Mnemonic {

    val mnemonicComponents: List<String>
    val wordlist: Wordlist

    /**
     * Generate seed
     *
     * @param passphrase optional by default is empty
     * @return Data The generated deterministic seed according to BIP-39
     */
    fun generateSeed(passphrase: String = ""): CompletionResult<ByteArray>
}

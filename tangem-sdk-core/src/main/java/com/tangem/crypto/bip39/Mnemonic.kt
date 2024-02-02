package com.tangem.crypto.bip39

import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError

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

    /**
     * Returns initial entropy
     *
     * @return Entropy data
     */
    @Throws(TangemSdkError.MnemonicException::class)
    fun getEntropy(): ByteArray

    /**
     * Allows to add extension for default init method in Android module
     */
    companion object
}

package com.tangem.crypto.bip39

import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError

interface BIP39 {

    val wordlist: Wordlist

    /**
     * Parse a mnemonic.
     * @param mnemonicString The mnemonic to parse
     * @return Mnemonic components
     */
    @Throws(TangemSdkError.MnemonicException::class)
    fun parse(mnemonicString: String): List<String>

    /** Generate a mnemonic.
     * @param entropyLength The  entropy length to use. Default is 128 bit.
     * @param wordlist The wordlist to use. Default is english.
     * @return The generated mnemonic split to components
     */
    @Throws(TangemSdkError.MnemonicException::class)
    fun generateMnemonic(
        entropyLength: EntropyLength = EntropyLength.Bits128Length,
        wordlist: Wordlist,
    ): List<String>

    /** Generate a deterministic  seed
     *  @param mnemonicComponents The mnemonic to use
     *  @param passphrase The passphrase to use. Default is no passphrase (empty).
     *  @return The generated seed
     */
    fun generateSeed(mnemonicComponents: List<String>, passphrase: String = ""): CompletionResult<ByteArray>
}
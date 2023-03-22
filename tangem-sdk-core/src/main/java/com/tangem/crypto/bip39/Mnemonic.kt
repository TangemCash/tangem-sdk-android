package com.tangem.crypto.bip39

interface Mnemonic {

    /**
     * Generate seed
     *
     * @param passphrase optional by default is empty
     * @return Data The generated deterministic seed according to BIP-39
     */
    fun generateSeed(passphrase: String = ""): ByteArray
}
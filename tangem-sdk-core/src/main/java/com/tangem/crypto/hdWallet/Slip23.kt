package com.tangem.crypto.hdWallet

import com.tangem.crypto.hdWallet.bip32.ExtendedPrivateKey
import com.tangem.crypto.pbkdf2sha512
import kotlin.experimental.and
import kotlin.experimental.or

// https://github.com/satoshilabs/slips/blob/master/slip-0023.md
// https://github.com/cardano-foundation/CIPs/blob/09d7d8ee1bd64f7e6b20b5a6cae088039dce00cb/CIP-0003/Icarus.md
class Slip23 {
    /**
     * Generate Ikarus master key from mnemonic for ed25519
     * @param entropy: Initial entropy used to create mnemonic
     * @param passphrase Passphrase for mnemonic.  Empty string if not set.
     * @return [ExtendedPrivateKey]
     */
    @Suppress("MagicNumber")
    fun makeIkarusMasterKey(entropy: ByteArray, passphrase: String): ExtendedPrivateKey {
        val passphraseData = passphrase.toByteArray(Charsets.UTF_8)

        val s = passphraseData.pbkdf2sha512(salt = entropy, iterations = 4096, keyByteCount = 96)
        s[0] = s[0] and 0xF8.toByte()
        s[31] = s[31] and 0x1F.toByte() or 0x40.toByte()

        val privateKey = s.sliceArray(0 until 64) // kL + kR
        val chainCode = s.sliceArray(64 until 96)

        return ExtendedPrivateKey(privateKey = privateKey, chainCode = chainCode)
    }
}

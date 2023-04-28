package com.tangem.crypto.bip39

import com.tangem.common.core.TangemSdkError
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.assertThrows
import java.io.BufferedInputStream
import java.io.InputStream

internal class BIP39WordlistTest {

    @Test
    fun testBIP39WordlistInitializationSuccess() {
        val wordlistStream = getInputStreamForFile(TEST_DICTIONARY_FILE_NAME)
        val biP39Wordlist = BIP39Wordlist(wordlistStream)
        assertNotNull(biP39Wordlist)
    }

    @Test
    fun testBIP39WordlistInitializationFail() {
        assertThrows<TangemSdkError.MnemonicException> {
            BIP39Wordlist(BufferedInputStream(ByteArray(10).inputStream()))
        }
    }

    private fun getInputStreamForFile(fileName: String): InputStream {
        return object {}.javaClass.classLoader.getResourceAsStream(fileName)!!
    }

    companion object {
        const val TEST_DICTIONARY_FILE_NAME = "mnemonic/mnemonic_dictionary_en.txt"
    }
}

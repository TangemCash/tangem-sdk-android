package com.tangem.crypto.bip39

import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.common.extensions.toHexString
import com.tangem.crypto.bip39.MnemonicTestData.EXPECTED_12_WORDS_SEED
import com.tangem.crypto.bip39.MnemonicTestData.EXPECTED_15_WORDS_SEED
import com.tangem.crypto.bip39.MnemonicTestData.EXPECTED_18_WORDS_SEED
import com.tangem.crypto.bip39.MnemonicTestData.EXPECTED_21_WORDS_SEED
import com.tangem.crypto.bip39.MnemonicTestData.EXPECTED_24_WORDS_SEED
import com.tangem.crypto.bip39.MnemonicTestData.MNEMONIC_12_WORDS
import com.tangem.crypto.bip39.MnemonicTestData.MNEMONIC_15_WORDS
import com.tangem.crypto.bip39.MnemonicTestData.MNEMONIC_18_WORDS
import com.tangem.crypto.bip39.MnemonicTestData.MNEMONIC_21_WORDS
import com.tangem.crypto.bip39.MnemonicTestData.MNEMONIC_24_WORDS
import com.tangem.crypto.bip39.MnemonicTestData.mnemonic12ParsedComponents
import com.tangem.crypto.bip39.MnemonicTestData.mnemonic15ParsedComponents
import com.tangem.crypto.bip39.MnemonicTestData.mnemonic18ParsedComponents
import com.tangem.crypto.bip39.MnemonicTestData.mnemonic21ParsedComponents
import com.tangem.crypto.bip39.MnemonicTestData.mnemonic24ParsedComponents
import org.junit.Test
import org.junit.jupiter.api.fail
import java.io.InputStream

internal class DefaultBIP39Test {

    @Test
    fun parse_12_words_test() {
        val bip39 = createDefaultBIP39()
        val parsedMnemonic = bip39.parse(MNEMONIC_12_WORDS)
        assert(parsedMnemonic == mnemonic12ParsedComponents)
    }

    @Test
    fun parse_15_words_test() {
        val bip39 = createDefaultBIP39()
        val parsedMnemonic = bip39.parse(MNEMONIC_15_WORDS)
        assert(parsedMnemonic == mnemonic15ParsedComponents)
    }

    @Test
    fun parse_18_words_test() {
        val bip39 = createDefaultBIP39()
        val parsedMnemonic = bip39.parse(MNEMONIC_18_WORDS)
        assert(parsedMnemonic == mnemonic18ParsedComponents)
    }

    @Test
    fun parse_21_words_test() {
        val bip39 = createDefaultBIP39()
        val parsedMnemonic = bip39.parse(MNEMONIC_21_WORDS)
        assert(parsedMnemonic == mnemonic21ParsedComponents)
    }

    @Test
    fun parse_24_words_test() {
        val bip39 = createDefaultBIP39()
        val parsedMnemonic = bip39.parse(MNEMONIC_24_WORDS)
        assert(parsedMnemonic == mnemonic24ParsedComponents)
    }

    @Test
    fun generateMnemonic_128bits_test() {
        val bip39 = createDefaultBIP39()
        val generatedMnemonic = bip39.generateMnemonic(EntropyLength.Bits128Length)
        val wordsCount = 12
        assert(generatedMnemonic.size == wordsCount)
        generatedMnemonic.forEach {
            assert(bip39.wordlist.words.contains(it))
        }
    }

    @Test
    fun generateMnemonic_160bits_test() {
        val bip39 = createDefaultBIP39()
        val generatedMnemonic = bip39.generateMnemonic(EntropyLength.Bits160Length)
        val wordsCount = 15
        assert(generatedMnemonic.size == wordsCount)
        generatedMnemonic.forEach {
            assert(bip39.wordlist.words.contains(it))
        }
    }

    @Test
    fun generateMnemonic_192bits_test() {
        val bip39 = createDefaultBIP39()
        val generatedMnemonic = bip39.generateMnemonic(EntropyLength.Bits192Length)
        val wordsCount = 18
        assert(generatedMnemonic.size == wordsCount)
        generatedMnemonic.forEach {
            assert(bip39.wordlist.words.contains(it))
        }
    }

    @Test
    fun generateMnemonic_224bits_test() {
        val bip39 = createDefaultBIP39()
        val generatedMnemonic = bip39.generateMnemonic(EntropyLength.Bits224Length)
        val wordsCount = 21
        assert(generatedMnemonic.size == wordsCount)
        generatedMnemonic.forEach {
            assert(bip39.wordlist.words.contains(it))
        }
    }

    @Test
    fun generateMnemonic_256bits_test() {
        val bip39 = createDefaultBIP39()
        val generatedMnemonic = bip39.generateMnemonic(EntropyLength.Bits256Length)
        val wordsCount = 24
        assert(generatedMnemonic.size == wordsCount)
        generatedMnemonic.forEach {
            assert(bip39.wordlist.words.contains(it))
        }
    }

    @Test
    fun generateSeed_12_words_test() {
        val bip39 = createDefaultBIP39()
        val generatedSeedResult = bip39.generateSeed(mnemonic12ParsedComponents)
        generatedSeedResult.doOnSuccess {
            val hexSeed = it.toHexString().lowercase()
            assert(hexSeed == EXPECTED_12_WORDS_SEED)
        }.doOnFailure { fail(it) }
    }

    @Test
    fun generateSeed_15_words_test() {
        val bip39 = createDefaultBIP39()
        val generatedSeedResult = bip39.generateSeed(mnemonic15ParsedComponents)
        generatedSeedResult.doOnSuccess {
            val hexSeed = it.toHexString().lowercase()
            assert(hexSeed == EXPECTED_15_WORDS_SEED)
        }.doOnFailure { fail(it) }
    }

    @Test
    fun generateSeed_18_words_test() {
        val bip39 = createDefaultBIP39()
        val generatedSeedResult = bip39.generateSeed(mnemonic18ParsedComponents)
        generatedSeedResult.doOnSuccess {
            val hexSeed = it.toHexString().lowercase()
            assert(hexSeed == EXPECTED_18_WORDS_SEED)
        }.doOnFailure { fail(it) }
    }

    @Test
    fun generateSeed_21_words_test() {
        val bip39 = createDefaultBIP39()
        val generatedSeedResult = bip39.generateSeed(mnemonic21ParsedComponents)
        generatedSeedResult.doOnSuccess {
            val hexSeed = it.toHexString().lowercase()
            assert(hexSeed == EXPECTED_21_WORDS_SEED)
        }.doOnFailure { fail(it) }
    }

    @Test
    fun generateSeed_24_words_test() {
        val bip39 = createDefaultBIP39()
        val generatedSeedResult = bip39.generateSeed(mnemonic24ParsedComponents)
        generatedSeedResult.doOnSuccess {
            val hexSeed = it.toHexString().lowercase()
            assert(hexSeed == EXPECTED_24_WORDS_SEED)
        }.doOnFailure { fail(it) }
    }

    private fun createDefaultBIP39(): DefaultBIP39 {
        val wordlistStream = getInputStreamForFile(BIP39WordlistTest.TEST_DICTIONARY_FILE_NAME)
        val biP39Wordlist = BIP39Wordlist(wordlistStream)
        return DefaultBIP39(biP39Wordlist)
    }

    private fun getInputStreamForFile(fileName: String): InputStream {
        return object {}.javaClass.classLoader.getResourceAsStream(fileName)!!
    }
}

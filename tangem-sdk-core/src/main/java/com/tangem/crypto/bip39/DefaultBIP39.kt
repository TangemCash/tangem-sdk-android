package com.tangem.crypto.bip39

import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.binaryToByteArray
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.leadingZeroPadding
import com.tangem.common.extensions.toBits
import com.tangem.crypto.CryptoUtils
import com.tangem.crypto.pbkdf2sha512
import java.text.Normalizer

internal class DefaultBIP39(override val wordlist: Wordlist) : BIP39 {

    override fun parse(mnemonicString: String): List<String> {
        val regex = Regex(pattern = "\\p{L}+")
        val matches = regex.findAll(mnemonicString)
        val components = matches.map { result ->
            result.value.trim().lowercase()
        }.toList()
        validate(components)
        return components.toList()
    }

    override fun generateMnemonic(entropyLength: EntropyLength): List<String> {
        // reminder of div by 32 should be 0
        if (entropyLength.count % DIV_ARG != 0) {
            throw TangemSdkError.MnemonicException(MnemonicErrorResult.MnenmonicCreationFailed)
        }

        val entropyBytesCount = entropyLength.toBytes()
        val entropyData = CryptoUtils.generateRandomBytes(length = entropyBytesCount)
        return generateMnemonic(entropyData = entropyData, wordlist = wordlist)
    }

    override fun generateSeed(mnemonicComponents: List<String>, passphrase: String): CompletionResult<ByteArray> {
        try {
            validate(mnemonicComponents)
        } catch (e: TangemSdkError.MnemonicException) {
            return CompletionResult.Failure(e)
        }
        val mnemonicString = convertToMnemonicString(mnemonicComponents)
        val normalizedMnemonic = try {
            normalizedData(mnemonicString)
        } catch (e: TangemSdkError.MnemonicException) {
            return CompletionResult.Failure(e)
        }
        val normalizedSalt = try {
            normalizedData(SEED_SALT_PREFIX + passphrase)
        } catch (e: TangemSdkError.MnemonicException) {
            return CompletionResult.Failure(e)
        }
        return CompletionResult.Success(normalizedMnemonic.pbkdf2sha512(salt = normalizedSalt, iterations = 2048))
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Throws(TangemSdkError.MnemonicException::class)
    private fun validate(mnemonicComponents: List<String>) {
        // Validate words count
        if (mnemonicComponents.isEmpty()) {
            throw TangemSdkError.MnemonicException(MnemonicErrorResult.InvalidWordCount)
        }

        val entropyLength = try {
            EntropyLength.create(mnemonicComponents.size)
        } catch (e: IllegalStateException) {
            throw TangemSdkError.MnemonicException(MnemonicErrorResult.InvalidWordCount)
        }

        // Validate wordlist by the first word
        val wordlistWords = try {
            getWordlist(mnemonicComponents[0]).words
        } catch (e: TangemSdkError.MnemonicException) {
            throw TangemSdkError.MnemonicException(MnemonicErrorResult.UnsupportedLanguage)
        }

        // Validate all the words
        val invalidWords = mutableSetOf<String>()

        // Generate an indices array inplace
        val concatenatedBits = StringBuilder()

        mnemonicComponents.forEach { word ->
            val wordIndex = wordlistWords.indexOfFirst { it == word }
            if (wordIndex == -1) {
                invalidWords.add(word)
            } else {
                val indexBits = Integer.toBinaryString(wordIndex).leadingZeroPadding(newLength = 11)
                concatenatedBits.append(indexBits)
            }
        }

        if (invalidWords.isNotEmpty()) {
            throw TangemSdkError.MnemonicException(MnemonicErrorResult.InvalidWords(invalidWords))
        }

        // Validate checksum
        val checksumBitsCount = mnemonicComponents.size / CHECKSUM_BITS_COUNT_DIVIDER
        if (checksumBitsCount != entropyLength.checksumBitsCount()) {
            throw TangemSdkError.MnemonicException(MnemonicErrorResult.InvalidChecksum)
        }

        val entropyBitsCount = concatenatedBits.length - checksumBitsCount
        val entropyBits = concatenatedBits.take(entropyBitsCount)
        val checksumBits = concatenatedBits.takeLast(checksumBitsCount)

        val entropyData = entropyBits.toString().binaryToByteArray()
            ?: throw TangemSdkError.MnemonicException(MnemonicErrorResult.InvalidChecksum)

        val calculatedChecksumBits = entropyData
            .calculateSha256()
            .toBits()
            .take(entropyLength.checksumBitsCount())
            .joinToString("")

        if (calculatedChecksumBits != checksumBits) {
            throw TangemSdkError.MnemonicException(MnemonicErrorResult.InvalidChecksum)
        }
    }

    @Throws(TangemSdkError.MnemonicException::class)
    private fun getWordlist(word: String): Wordlist {
        if (wordlist.words.contains(word)) {
            return wordlist
        }
        throw TangemSdkError.MnemonicException(MnemonicErrorResult.InvalidWordsFile)
    }

    private fun generateMnemonic(entropyData: ByteArray, wordlist: Wordlist): List<String> {
        val entropyLength = try {
            EntropyLength.create(entropyData = entropyData)
        } catch (e: IllegalArgumentException) {
            throw TangemSdkError.MnemonicException(MnemonicErrorResult.InvalidEntropyLength)
        }

        // https://github.com/bitcoin/bips/blob/master/bip-0039.mediawiki more detail
        // calculate checksum
        val entropyHashBits = entropyData.calculateSha256().toBits()
        // prefix of entropy bits that are checksum of entropyData
        val entropyChecksumBits = entropyHashBits.take(entropyLength.checksumBitsCount())

        // converts entropyData to bits array ["0", "1", ...]
        val entropyBits = entropyData.toBits()
        // sequence of entropy bits and checksum of entropy bits
        val concatenatedBits = entropyBits + entropyChecksumBits
        // divide sequence of bits to chunks by 11 bits
        val bitIndices = concatenatedBits.chunked(size = 11)
        // converts chunks to Ints that are indices of words in dictionary
        val indices = bitIndices.map {
            it.joinToString(separator = "").toInt(2)
        }

        if (indices.size != entropyLength.wordCount()) {
            throw TangemSdkError.MnemonicException(MnemonicErrorResult.MnenmonicCreationFailed)
        }

        val allWords = wordlist.words
        val maxWordIndex = allWords.size

        val words = indices.map { index ->
            if (index > maxWordIndex) {
                throw TangemSdkError.MnemonicException(MnemonicErrorResult.MnenmonicCreationFailed)
            }
            allWords[index]
        }

        return words
    }

    /** Convert mnemonic components to a single string, split by spaces
     * @param mnemonicComponents Mnemonic components to use
     * @return The mnemonic string
     */
    private fun convertToMnemonicString(mnemonicComponents: List<String>): String {
        return mnemonicComponents.joinToString(" ")
    }

    @Throws(TangemSdkError.MnemonicException::class)
    private fun normalizedData(string: String): ByteArray {
        try {
            val normalizedString = Normalizer.normalize(string, Normalizer.Form.NFKD)
            return normalizedString.toByteArray(Charsets.UTF_8)
        } catch (e: Exception) {
            throw TangemSdkError.MnemonicException(MnemonicErrorResult.NormalizationFailed)
        }
    }

    companion object {
        private const val SEED_SALT_PREFIX = "mnemonic"
        private const val DIV_ARG = 32
        private const val CHECKSUM_BITS_COUNT_DIVIDER = 3
    }
}

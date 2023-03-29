package com.tangem.crypto.bip39

sealed class EntropyLength(val count: Int) {
    object Bits128Length : EntropyLength(BITS_COUNT_128)
    object Bits160Length : EntropyLength(BITS_COUNT_160)
    object Bits192Length : EntropyLength(BITS_COUNT_192)
    object Bits224Length : EntropyLength(BITS_COUNT_224)
    object Bits256Length : EntropyLength(BITS_COUNT_256)

    fun wordCount(): Int {
        return when (this) {
            Bits128Length -> WORDS_COUNT_FOR_BITS_128
            Bits160Length -> WORDS_COUNT_FOR_BITS_160
            Bits192Length -> WORDS_COUNT_FOR_BITS_192
            Bits224Length -> WORDS_COUNT_FOR_BITS_224
            Bits256Length -> WORDS_COUNT_FOR_BITS_256
        }
    }

    fun checksumBitsCount(): Int {
        return this.count / CHECKSUM_BITS_COUNT_DIVIDER
    }

    fun toBytes(): Int {
        return this.count / BYTE_SIZE
    }

    companion object {
        private const val BITS_COUNT_128 = 128
        private const val BITS_COUNT_160 = 160
        private const val BITS_COUNT_192 = 192
        private const val BITS_COUNT_224 = 224
        private const val BITS_COUNT_256 = 256
        private const val WORDS_COUNT_FOR_BITS_128 = 12
        private const val WORDS_COUNT_FOR_BITS_160 = 15
        private const val WORDS_COUNT_FOR_BITS_192 = 18
        private const val WORDS_COUNT_FOR_BITS_224 = 21
        private const val WORDS_COUNT_FOR_BITS_256 = 24
        private const val CHECKSUM_BITS_COUNT_DIVIDER = 32
        private const val BYTE_SIZE = 8

        @kotlin.jvm.Throws(IllegalStateException::class)
        fun create(length: Int): EntropyLength {
            return when (length) {
                WORDS_COUNT_FOR_BITS_128 -> Bits128Length
                WORDS_COUNT_FOR_BITS_160 -> Bits160Length
                WORDS_COUNT_FOR_BITS_192 -> Bits192Length
                WORDS_COUNT_FOR_BITS_224 -> Bits224Length
                WORDS_COUNT_FOR_BITS_256 -> Bits256Length
                else -> error("no such EntropyLength")
            }
        }

        @kotlin.jvm.Throws(IllegalStateException::class)
        fun create(entropyData: ByteArray): EntropyLength {
            return when (entropyData.count() * BYTE_SIZE) {
                BITS_COUNT_128 -> Bits128Length
                BITS_COUNT_160 -> Bits160Length
                BITS_COUNT_192 -> Bits192Length
                BITS_COUNT_224 -> Bits224Length
                BITS_COUNT_256 -> Bits256Length
                else -> error("no such EntropyLength")
            }
        }
    }
}
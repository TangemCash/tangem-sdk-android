package com.tangem.crypto

import com.tangem.common.extensions.hexToBytes

object WIF {
    fun encodeToWIFCompressed(privateKey: ByteArray, networkType: NetworkType): String {
        val extended = networkType.prefix + privateKey + Constants.compressedSuffix
        return extended.encodeToBase58WithChecksum()
    }

    fun decodeWIFCompressed(string: String): ByteArray? {
        val decoded = try {
            string.decodeBase58WithChecksum()
        } catch (e: Exception) {
            return null
        }
        var data = decoded.copyOfRange(1, decoded.size)

        if (!string.startsWith(Constants.uncompressedMainnetPrefix) &&
            !string.startsWith(Constants.uncompressedTestnetPrefix) &&
            data.last() == Constants.compressedSuffix[0]
        ) {
            data = data.copyOfRange(0, data.size - 1)
        }

        return data
    }
}

private object Constants {
    val prefixMainnet = "80".hexToBytes()
    val prefixTestnet = "EF".hexToBytes()
    val compressedSuffix = "01".hexToBytes()
    const val uncompressedMainnetPrefix = "5"
    const val uncompressedTestnetPrefix = "9"
}

private val NetworkType.prefix: ByteArray
    get() = when (this) {
        NetworkType.Mainnet -> Constants.prefixMainnet
        NetworkType.Testnet -> Constants.prefixTestnet
    }

package com.tangem.operations.read

/**
 * Created by Anton Zhilenkov on 26/03/2021.
 *
 * Available modes for reading card information
 * Note: This modes available for cards with COS v.4.0 and higher
 */
enum class ReadMode(val rawValue: Int) {
    Card(0x01),
    Wallet(0x02),
    WalletsList(0x03);

    companion object {
        private val values = values()
        fun byRawValue(rawValue: Int): ReadMode? = values.find { it.rawValue == rawValue }
    }
}
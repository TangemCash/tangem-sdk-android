@file:Suppress("unused")

package com.tangem.jvm

import com.tangem.Log
import com.tangem.TangemSdk
import com.tangem.TangemSdkLogger
import com.tangem.common.biometric.DummyBiometricManager
import com.tangem.common.core.Config
import com.tangem.common.services.InMemoryStorage
import com.tangem.crypto.bip39.BIP39Wordlist
import com.tangem.crypto.bip39.Wordlist
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun TangemSdk.init(verbose: Boolean = false, indexOfTerminal: Int? = null): TangemSdk? {
    setLogger(verbose)
    val reader = ReaderFactory().create(indexOfTerminal) ?: return null
    return TangemSdk(
        reader = reader,
        viewDelegate = LoggingSessionDelegate(),
        biometricManager = DummyBiometricManager(),
        secureStorage = InMemoryStorage(),
        config = Config(),
        wordlist = createDefaultWordlist(),
    )
}

private fun setLogger(verbose: Boolean) {
    Log.addLogger(
        object : TangemSdkLogger {
            private val dateFormatter: DateFormat = SimpleDateFormat("HH:mm:ss:SSS", Locale.getDefault())
            private val tag = "Tangem SDK"

            override fun log(message: () -> String, level: Log.Level) {
                val prefixDelimiter = if (level.prefix.isEmpty()) "" else ": "
                val logMessage = "${dateFormatter.format(Date())}: ${level.prefix}$prefixDelimiter${message()}"

                println("$tag: $logMessage")
            }
        },
    )
}

private fun createDefaultWordlist(): Wordlist {
    Thread.currentThread().contextClassLoader.getResourceAsStream(DICTIONARY_FILE_NAME).use { wordListInputStream ->
        return BIP39Wordlist(wordListInputStream)
    }
}

private const val DICTIONARY_FILE_NAME = "mnemonic/mnemonic_dictionary_en.txt"

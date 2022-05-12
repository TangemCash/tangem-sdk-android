package com.tangem.jvm

import com.tangem.Log
import com.tangem.TangemSdk
import com.tangem.TangemSdkImpl
import com.tangem.TangemSdkLogger
import com.tangem.common.core.Config
import com.tangem.common.services.secure.UnsafeInMemoryStorage
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

fun TangemSdk.Companion.init(verbose: Boolean = false, indexOfTerminal: Int? = null): TangemSdk? {
    setLogger(verbose)
    val reader = ReaderFactory().create(indexOfTerminal) ?: return null
    return TangemSdkImpl(reader, LoggingSessionDelegate(), UnsafeInMemoryStorage(), Config())
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
            }
    )
}
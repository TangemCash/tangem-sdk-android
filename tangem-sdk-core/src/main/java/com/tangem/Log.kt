package com.tangem

import com.tangem.common.extensions.titleFormatted

interface TangemSdkLogger {
    fun log(message: () -> String, level: Log.Level)
}

object Log {

    private val loggers: MutableList<TangemSdkLogger> = mutableListOf()

    fun command(any: Any, message: (() -> String)? = null) {
        val commandName = any::class.java.simpleName
        logInternal(
            {
                val messageBody = "run: $commandName${message?.invoke() ?: ""}"
                messageBody.titleFormatted(maxLength = 80)
            },
            Level.Command,
        )
    }

    fun tlv(message: () -> String) {
        logInternal(message, Level.Tlv)
    }

    fun apdu(message: () -> String) {
        logInternal(message, Level.Apdu)
    }

    fun apduCommand(message: () -> String) {
        logInternal(message, Level.ApduCommand)
    }

    fun session(message: () -> String) {
        logInternal(message, Level.Session)
    }

    fun nfc(message: () -> String) {
        logInternal(message, Level.Nfc)
    }

    fun warning(message: () -> String) {
        logInternal(message, Level.Warning)
    }

    fun error(message: () -> String) {
        logInternal(message, Level.Error)
    }

    fun debug(message: () -> String) {
        logInternal(message, Level.Debug)
    }

    fun network(message: () -> String) {
        logInternal(message, Level.Network)
    }

    fun view(message: () -> String) {
        logInternal(message, Level.View)
    }

    fun info(message: () -> String) {
        logInternal(message, Level.Info)
    }

    fun biometric(message: () -> String) {
        logInternal(message, Level.Biometric)
    }

    private fun logInternal(message: () -> String, level: Level) {
        if (loggers.isEmpty()) return

        loggers.forEach { it.log(message, level) }
    }

    fun addLogger(logger: TangemSdkLogger) {
        loggers.add(logger)
    }

    fun removeLogger(logger: TangemSdkLogger) {
        loggers.remove(logger)
    }

    enum class Level(val prefix: String) {
        Info("Info: "),
        Debug("Debug: "),
        Warning(""),
        Error(""),
        Network("Network: "),
        View("ViewDelegate: "),
        Session("CardSession: "),
        Command(""),
        ApduCommand(""),
        Apdu(""),
        Nfc("NFCReader: "),
        Tlv(""),
        Biometric("Biometric: "),
    }

    enum class Config(val levels: List<Level>) {
        Release(listOf(Level.Error)),
        Debug(listOf(Level.Warning, Level.Error)),
        Verbose(Level.values().toList()),
    }
}

interface LogFormat {
    fun format(message: () -> String, level: Log.Level): String

    class StairsFormatter(
        private val stepSpace: String = "    ",
        private val stepLength: Map<Log.Level, Int> = defaultStepLength(),
    ) : LogFormat {

        override fun format(message: () -> String, level: Log.Level): String {
            val stepSpace = stepLength[level]?.let { stepSpace.repeat(it) } ?: ""
            return "$stepSpace${level.prefix}${message()}"
        }

        companion object {
            @Suppress("MagicNumber")
            fun defaultStepLength(): Map<Log.Level, Int> {
                return mapOf(
                    Log.Level.Session to 1,
                    Log.Level.Nfc to 2,
                    Log.Level.Apdu to 2,
                    Log.Level.ApduCommand to 4,
                    Log.Level.Tlv to 5,
                )
            }
        }
    }
}

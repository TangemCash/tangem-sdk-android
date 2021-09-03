package com.tangem

import com.tangem.common.extensions.titleFormatted

object Log {

    private val loggers: MutableList<TangemSdkLogger> = mutableListOf()

    fun command(any: Any, message: (() -> String)? = null) {
        val commandName = any::class.java.simpleName
        val message = message ?: { "Send" }
        logInternal({ "$commandName: ${message()}".titleFormatted() }, Level.Command)
    }

    fun tlv(message: () -> String) {
        logInternal(message, Level.Tlv)
    }

    fun apdu(message: () -> String) {
        logInternal(message, Level.Apdu)
    }

    fun apduCommand(message: () -> String) {
        logInternal({ message().titleFormatted("-") }, Level.Apdu)
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
        Tlv(""),
        Apdu(""),
        Nfc("NFCReader"),
        Command(""),
        Session("CardSession"),
        View("ViewDelegate"),
        Network(""),
        Debug("Debug"),
        Info("Info"),
        Warning(""),
        Error(""),
    }

    enum class Config(val levels: List<Level>) {
        Release(listOf(Level.Error)),
        Debug(listOf(Level.Warning, Level.Error)),
        Verbose(Level.values().toList()),
    }
}

interface TangemSdkLogger {
    fun log(message: () -> String, level: Log.Level)
}
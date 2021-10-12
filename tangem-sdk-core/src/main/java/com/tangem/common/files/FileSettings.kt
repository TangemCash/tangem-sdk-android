package com.tangem.common.files

/**
 * Created by Anton Zhilenkov on 20/07/2021.
 */
enum class FileSettings(val rawValue: Int) {
    Public(0x0001),
    Private(0x0000);

    companion object {
        private val values = values()
        fun byRawValue(rawValue: Int): FileSettings? = values.find { it.rawValue == rawValue }
    }
}

data class FileSettingsChange(
    val fileIndex: Int,
    val settings: FileSettings
)
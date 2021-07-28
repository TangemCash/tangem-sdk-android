package com.tangem.common.files

enum class FileDataMode(val rawValue: Int) {
    InitiateWritingFile(0x01),
    WriteFile(0x02),
    ConfirmWritingFile(0x03),
    DeleteFile(0x05),
    ChangeFileSettings(0x06),
    ReadFileHash(0x01);

    companion object {
        private val values = values()
        fun byRawValue(rawValue: Int): FileDataMode? = values.find { it.rawValue == rawValue }
    }
}
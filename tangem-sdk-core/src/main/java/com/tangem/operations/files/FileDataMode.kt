package com.tangem.operations.files

enum class FileDataMode(val rawValue: Int) {
    InitiateWritingFile(rawValue = 0x01),
    WriteFile(rawValue = 0x02),
    ConfirmWritingFile(rawValue = 0x03),
    DeleteFile(rawValue = 0x05),
    ChangeFileSettings(rawValue = 0x06),
    ReadFileHash(rawValue = 0x01),
    ;

    companion object {
        private val values = values()
        fun byRawValue(rawValue: Int): FileDataMode? = values.find { it.rawValue == rawValue }
    }
}

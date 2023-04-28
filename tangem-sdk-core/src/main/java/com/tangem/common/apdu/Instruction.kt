package com.tangem.common.apdu

/**
 * Instruction code that determines the type of the command that is sent to the Tangem card.
 * It is used in the construction of [com.tangem.common.apdu.CommandApdu].
 */
enum class Instruction(var code: Int) {
    Unknown(code = 0x00),
    Personalize(code = 0xF1),
    Read(code = 0xF2),
    AttestCardKey(code = 0xF3),
    AttestCardUniqueness(code = 0xF4),
    AttestCardFirmware(code = 0xF5),
    WriteIssuerData(code = 0xF6),
    ReadIssuerData(code = 0xF7),
    CreateWallet(code = 0xF8),
    AttestWalletKey(code = 0xF9),
    SetPin(code = 0xFA),
    Sign(code = 0xFB),
    PurgeWallet(code = 0xFC),
    Activate(code = 0xFE),
    OpenSession(code = 0xFF),
    WriteUserData(code = 0xE0),
    ReadUserData(code = 0xE1),
    Depersonalize(code = 0xE3),
    WriteFileData(code = 0xD0),
    ReadFileData(code = 0xD1),
    StartPrimaryCardLinking(code = 0xE8),
    StartBackupCardLinking(code = 0xE9),
    LinkBackupCards(code = 0xEA),
    ReadBackupData(code = 0xEB),
    LinkPrimaryCard(code = 0xEC),
    WriteBackupData(code = 0xED),
    ManageFileOwners(code = 0xD2),
    Authorize(code = 0xD3),
    BackupReset(code = 0xEE),
    GenerateOTP(code = 0xE2),
    GetEntropy(code = 0xE7),
    SetUserSettings(code = 0xD5),
    ;

    companion object {
        private val values = values()
        fun byCode(code: Int): Instruction = values.find { it.code == code } ?: Unknown
    }
}

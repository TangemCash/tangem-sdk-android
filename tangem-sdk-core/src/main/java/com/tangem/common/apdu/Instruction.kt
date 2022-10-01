package com.tangem.common.apdu

/**
 * Instruction code that determines the type of the command that is sent to the Tangem card.
 * It is used in the construction of [com.tangem.common.apdu.CommandApdu].
 */
enum class Instruction(var code: Int) {
    Unknown(0x00),
    Personalize(0xF1),
    Read(0xF2),
    AttestCardKey(0xF3),
    AttestCardUniqueness(0xF4),
    AttestCardFirmware(0xF5),
    WriteIssuerData(0xF6),
    ReadIssuerData(0xF7),
    CreateWallet(0xF8),
    AttestWalletKey(0xF9),
    SetPin(0xFA),
    Sign(0xFB),
    PurgeWallet(0xFC),
    Activate(0xFE),
    OpenSession(0xFF),
    WriteUserData(0xE0),
    ReadUserData(0xE1),
    Depersonalize(0xE3),
    WriteFileData(0xD0),
    ReadFileData(0xD1),
    StartPrimaryCardLinking(0xE8),
    StartBackupCardLinking(0xE9),
    LinkBackupCards(0xEA),
    ReadBackupData(0xEB),
    LinkPrimaryCard(0xEC),
    WriteBackupData(0xED),
    ManageFileOwners(0xD2),
    Authorize(0xD3),
    BackupReset(0xEE),
    GenerateOTP(0xE2),
    ;

    companion object {
        private val values = values()
        fun byCode(code: Int): Instruction = values.find { it.code == code } ?: Unknown
    }
}
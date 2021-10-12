package com.tangem.common.tlv

/**
 * Contains all possible value types that value for [TlvTag] can contain.
 */
enum class TlvValueType {
    HexString,
    HexStringToHash,
    Utf8String,
    Uint8,
    Uint16,
    Uint32,
    BoolValue,
    ByteArray,
    EllipticCurve,
    DateTime,
    ProductMask,
    SettingsMask,
    Status,
    SigningMethod,
    InteractionMode,
    FileDataMode,
    FileSettings,
    DerivationPath
}

/**
 * Contains all TLV tags, with their code and descriptive name.
 */
enum class TlvTag(val code: Int) {
    Unknown(0x00),
    CardId(0x01),
    Status(0x02),
    CardPublicKey(0x03),
    CardSignature(0x04),
    CurveId(0x05),
    HashAlgID(0x06),
    SigningMethod(0x07),
    MaxSignatures(0x08),
    PauseBeforePin2(0x09),
    SettingsMask(0x0A),
    CardData(0x0C),
    NdefData(0x0D),
    CreateWalletAtPersonalize(0x0E),
    Health(0x0F),

    Pin(0x10),
    Pin2(0x11),
    NewPin(0x12),
    NewPin2(0x13),
    NewPinHash(0x14),
    NewPin2Hash(0x15),
    Challenge(0x16),
    Salt(0x17),
    ValidationCounter(0x18),
    Cvc(0x19),

    SessionKeyA(0x1A),
    SessionKeyB(0x1B),
    Pause(0x1C),
    NewPin3(0x1E),
    CrExKey(0x1F),

    Uid(0x0B),

    ManufacturerName(0x20),
    CardIDManufacturerSignature(0x86),

    IssuerDataPublicKey(0x30),
    IssuerTransactionPublicKey(0x31),
    IssuerData(0x32),
    IssuerDataSignature(0x33),
    IssuerTransactionSignature(0x34),
    IssuerDataCounter(0x35),
    AcquirerPublicKey(0x37),

    Size(0x25),
    InteractionMode(0x23),
    Offset(0x24),

    IsActivated(0x3A),
    ActivationSeed(0x3B),
    ResetPin(0x36),

    CodePageAddress(0x40),
    CodePageCount(0x41),
    CodeHash(0x42),

    TransactionOutHash(0x50),
    TransactionOutHashSize(0x51),
    TransactionOutRaw(0x52),
    PinIsDefault(0x5A),
    Pin2IsDefault(0x59),

    WalletPublicKey(0x60),
    WalletSignature(0x61),
    WalletRemainingSignatures(0x62),
    WalletSignedHashes(0x63),
    CheckWalletCounter(0x64),
    WalletIndex(0x65),
    WalletsCount(0x66),
    WalletData(0x67),
    CardWallet(0x68),

    WalletHDPath(0x6A),
    WalletHDChain(0x6B),

    Firmware(0x80),
    BatchId(0x81),
    ManufactureDateTime(0x82),
    IssuerName(0x83),
    BlockchainName(0x84),
    ManufacturerPublicKey(0x85),
    CardIdManufacturerSignature(0x86),

    ProductMask(0x8A),
    PaymentFlowVersion(0x54),

    TokenSymbol(0xA0),
    TokenContractAddress(0xA1),
    TokenDecimal(0xA2),
    TokenName(0xA3),
    Denomination(0xC0),
    ValidatedBalance(0xC1),
    LastSignDate(0xC2),
    DenominationText(0xC3),

    TerminalIsLinked(0x58),
    TerminalPublicKey(0x5C),
    TerminalTransactionSignature(0x57),

    UserData(0x2A),
    UserProtectedData(0x2B),
    UserCounter(0x2C),
    UserProtectedCounter(0x2D),

    WriteFileMode(0x23),
    FileIndex(0x26),
    FileSettings(0x27),

    FileName(0x70),
    FileData(0x71),
    ;

    /**
     * @return [TlvValueType] associated with a [TlvTag]
     */
    fun valueType(): TlvValueType {
        return when (this) {
            CardId, BatchId -> TlvValueType.HexString
            ManufacturerName, Firmware, IssuerName, BlockchainName, TokenSymbol, TokenName, TokenContractAddress,
            FileName -> TlvValueType.Utf8String
            CurveId -> TlvValueType.EllipticCurve
            PauseBeforePin2, WalletRemainingSignatures, WalletSignedHashes, Health, TokenDecimal,
            Offset, Size -> TlvValueType.Uint16
            FileIndex, WalletIndex, WalletsCount, CheckWalletCounter -> TlvValueType.Uint8
            MaxSignatures, UserCounter, UserProtectedCounter, IssuerDataCounter -> TlvValueType.Uint32
            IsActivated, TerminalIsLinked, CreateWalletAtPersonalize, Pin2IsDefault -> TlvValueType.BoolValue
            ManufactureDateTime -> TlvValueType.DateTime
            ProductMask -> TlvValueType.ProductMask
            SettingsMask -> TlvValueType.SettingsMask
            Status -> TlvValueType.Status
            SigningMethod -> TlvValueType.SigningMethod
            InteractionMode -> TlvValueType.InteractionMode
            WriteFileMode -> TlvValueType.FileDataMode
            FileSettings -> TlvValueType.FileSettings
            WalletHDPath -> TlvValueType.DerivationPath
            else -> TlvValueType.ByteArray
        }
    }

    companion object {
        private val values = values()
        fun byCode(code: Int): TlvTag = values.find { it.code == code } ?: Unknown
    }
}



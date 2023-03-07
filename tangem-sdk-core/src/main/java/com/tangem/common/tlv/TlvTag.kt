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
    DerivationPath,
    BackupStatus
}

/**
 * Contains all TLV tags, with their code and descriptive name.
 */
enum class TlvTag(val code: Int) {
    Unknown(code = 0x00),
    CardId(code = 0x01),
    Status(code = 0x02),
    CardPublicKey(code = 0x03),
    CardSignature(code = 0x04),
    CurveId(code = 0x05),
    HashAlgID(code = 0x06),
    SigningMethod(code = 0x07),
    MaxSignatures(code = 0x08),
    PauseBeforePin2(code = 0x09),
    SettingsMask(code = 0x0A),
    CardData(code = 0x0C),
    NdefData(code = 0x0D),
    CreateWalletAtPersonalize(code = 0x0E),
    Health(code = 0x0F),

    Pin(code = 0x10),
    Pin2(code = 0x11),
    NewPin(code = 0x12),
    NewPin2(code = 0x13),
    PublicKeyChallenge(code = 0x14),
    PublicKeySalt(code = 0x15),
    Challenge(code = 0x16),
    Salt(code = 0x17),
    ValidationCounter(code = 0x18),
    Cvc(code = 0x19),

    SessionKeyA(code = 0x1A),
    SessionKeyB(code = 0x1B),
    Pause(code = 0x1C),
    NewPin3(code = 0x1E),
    CrExKey(code = 0x1F),

    Uid(code = 0x0B),

    ManufacturerName(code = 0x20),
    CardIDManufacturerSignature(code = 0x86),

    IssuerPublicKey(code = 0x30),
    IssuerTransactionPublicKey(code = 0x31),
    IssuerData(code = 0x32),
    IssuerDataSignature(code = 0x33),
    IssuerTransactionSignature(code = 0x34),
    IssuerDataCounter(code = 0x35),
    AcquirerPublicKey(code = 0x37),

    Size(code = 0x25),
    InteractionMode(code = 0x23),
    Offset(code = 0x24),

    IsActivated(code = 0x3A),
    ActivationSeed(code = 0x3B),
    ResetPin(code = 0x36),

    CodePageAddress(code = 0x40),
    CodePageCount(code = 0x41),
    CodeHash(code = 0x42),

    TransactionOutHash(code = 0x50),
    TransactionOutHashSize(code = 0x51),
    TransactionOutRaw(code = 0x52),
    Certificate(code = 0x55),
    PinIsDefault(code = 0x5A),
    Pin2IsDefault(code = 0x59),

    WalletPublicKey(code = 0x60),
    WalletSignature(code = 0x61),
    WalletRemainingSignatures(code = 0x62),
    WalletSignedHashes(code = 0x63),
    CheckWalletCounter(code = 0x64),
    WalletIndex(code = 0x65),
    WalletsCount(code = 0x66),
    WalletData(code = 0x67),
    CardWallet(code = 0x68),

    WalletHDPath(code = 0x6A),
    WalletHDChain(code = 0x6B),

    Firmware(code = 0x80),
    BatchId(code = 0x81),
    ManufactureDateTime(code = 0x82),
    IssuerName(code = 0x83),
    BlockchainName(code = 0x84),
    ManufacturerPublicKey(code = 0x85),
    CardIdManufacturerSignature(code = 0x86),

    ProductMask(code = 0x8A),
    PaymentFlowVersion(code = 0x54),

    TokenSymbol(code = 0xA0),
    TokenContractAddress(code = 0xA1),
    TokenDecimal(code = 0xA2),
    TokenName(code = 0xA3),
    Denomination(code = 0xC0),
    ValidatedBalance(code = 0xC1),
    LastSignDate(code = 0xC2),
    DenominationText(code = 0xC3),

    TerminalIsLinked(code = 0x58),
    TerminalPublicKey(code = 0x5C),
    TerminalTransactionSignature(code = 0x57),

    UserData(code = 0x2A),
    UserProtectedData(code = 0x2B),
    UserCounter(code = 0x2C),
    UserProtectedCounter(code = 0x2D),

    FileIndex(code = 0x26),
    FileSettings(code = 0x27),

    FileTypeName(code = 0x70),
    FileData(code = 0x71),
    FileSignature(code = 0x73),
    FileCounter(code = 0x74),
    FileOwnerIndex(code = 0x75),

    BackupStatus(code = 0xD0),
    BackupCount(code = 0xD1),
    PrimaryCardLinkingKey(code = 0xD2),
    BackupCardLinkingKey(code = 0xD3),
    BackupCardLink(code = 0xD4),
    BackupAttestSignature(code = 0xD5)
    ;

    /**
     * @return [TlvValueType] associated with a [TlvTag]
     */
    @Suppress("ComplexMethod")
    fun valueType(): TlvValueType {
        return when (this) {
            CardId, BatchId -> TlvValueType.HexString
            ManufacturerName,
            Firmware,
            IssuerName,
            BlockchainName,
            TokenSymbol,
            TokenName,
            TokenContractAddress,
            FileTypeName,
            -> TlvValueType.Utf8String
            CurveId -> TlvValueType.EllipticCurve
            PauseBeforePin2,
            WalletRemainingSignatures,
            WalletSignedHashes,
            Health,
            TokenDecimal,
            Offset,
            Size,
            -> TlvValueType.Uint16
            FileIndex, WalletIndex, FileOwnerIndex, WalletsCount, CheckWalletCounter, FileCounter, BackupCount ->
                TlvValueType.Uint8
            MaxSignatures, UserCounter, UserProtectedCounter, IssuerDataCounter -> TlvValueType.Uint32
            IsActivated, TerminalIsLinked, CreateWalletAtPersonalize,
            PinIsDefault, Pin2IsDefault,
            -> TlvValueType.BoolValue
            ManufactureDateTime -> TlvValueType.DateTime
            ProductMask -> TlvValueType.ProductMask
            SettingsMask -> TlvValueType.SettingsMask
            Status -> TlvValueType.Status
            SigningMethod -> TlvValueType.SigningMethod
            InteractionMode -> TlvValueType.InteractionMode
            WalletHDPath -> TlvValueType.DerivationPath
            BackupStatus -> TlvValueType.BackupStatus
            else -> TlvValueType.ByteArray
        }
    }

    companion object {
        private val values = values()
        fun byCode(code: Int): TlvTag = values.find { it.code == code } ?: Unknown
    }
}

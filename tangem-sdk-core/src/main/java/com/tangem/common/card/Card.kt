package com.tangem.common.card

import com.squareup.moshi.JsonClass
import com.tangem.common.BaseMask
import com.tangem.common.Mask
import com.tangem.operations.CommandResponse
import com.tangem.operations.attestation.Attestation
import com.tangem.operations.read.ReadCommand
import java.util.Date

/**
 * Response for [ReadCommand]. Contains detailed card information.
 */
@JsonClass(generateAdapter = true)
data class Card internal constructor(
    /**
     * Unique Tangem card ID number.
     */
    val cardId: String,

    /**
     * Tangem internal manufacturing batch ID.
     */
    val batchId: String,

    /**
     * Public key that is used to authenticate the card against manufacturer’s database.
     * It is generated one time during card manufacturing.
     */
    val cardPublicKey: ByteArray,

    /**
     * Version of Tangem COS.
     */
    val firmwareVersion: FirmwareVersion,

    /**
     * Information about manufacturer.
     */
    val manufacturer: Manufacturer,

    /**
     * Information about issuer
     */
    val issuer: Issuer,

    /**
     * Card setting, that were set during the personalization process
     */
    val settings: Settings,

    /**
     * Card settings that were set during the personalization process and can be changed by user directly
     */
    val userSettings: UserSettings,

    /**
     * When this value is `current`, it means that the application is linked to the card,
     * and COS will not enforce security delay if `SignCommand` will be called
     * with `TlvTag.TerminalTransactionSignature` parameter containing a correct signature of raw data
     * to be signed made with `TlvTag.TerminalPublicKey`.
     * */
    val linkedTerminalStatus: LinkedTerminalStatus,

    /**
     * Access code (aka PIN1) is set.
     */
    val isAccessCodeSet: Boolean,

    /**
     * COS v. 4.33 and higher - always available
     * COS v. 1.19 and lower - always unavailable
     * COS  v > 1.19 &&  v < 4.33 - available only if `isResettingUserCodesAllowed` set to true
     */
    val isPasscodeSet: Boolean?,

    /**
     * Array of ellipctic curves, supported by this card. Only wallets with these curves can be created.
     */
    val supportedCurves: List<EllipticCurve>,

    /**
     * Wallets, created on the card, that can be used for signature
     */
    val wallets: List<CardWallet>,

    /**
     * Card's attestation report
     */
    val attestation: Attestation = Attestation.empty,

    /**
     *  Any non-zero value indicates that the card experiences some hardware problems.
     *  User should withdraw the value to other blockchain wallet as soon as possible.
     *  Non-zero Health tag will also appear in responses of all other commands.
     */
    @Transient
    internal var health: Int? = null,

    /**
     *  Remaining number of `SignCommand` operations before the wallet will stop signing transactions.
     *  Note: This counter were deprecated for cards with COS 4.0 and higher
     */
    @Transient
    internal var remainingSignatures: Int? = null,

    val backupStatus: BackupStatus? = null,
) : CommandResponse {

    fun setWallets(newWallets: List<CardWallet>): Card {
        val sortedWallets = newWallets.toMutableList().apply { sortBy { it.index } }
        return this.copy(wallets = sortedWallets.toList())
    }

    fun wallet(publicKey: ByteArray): CardWallet? = wallets.firstOrNull { it.publicKey.contentEquals(publicKey) }

    fun addWallet(wallet: CardWallet): Card {
        val sortedWallets = wallets.toMutableList().apply {
            add(wallet)
            sortBy { it.index }
        }
        return this.copy(wallets = sortedWallets.toList())
    }

    fun removeWallet(publicKey: ByteArray): Card {
        val wallet = wallet(publicKey) ?: return this
        return setWallets(wallets.toMutableList().apply { remove(wallet) })
    }

    fun updateWallet(wallet: CardWallet): Card {
        val mutableWallets = wallets.toMutableList()
        val foundIndex = mutableWallets.indexOfFirst { it.index == wallet.index }
        return if (foundIndex != -1) {
            mutableWallets[foundIndex] = wallet
            this.copy(wallets = mutableWallets.toList())
        } else {
            this
        }
    }

    data class Manufacturer(
        /**
         * Card manufacturer name.
         */
        val name: String,

        /**
         * Timestamp of manufacturing.
         */
        val manufactureDate: Date,

        /**
         * Signature of CardId with manufacturer’s private key. COS 1.21+
         */
        val signature: ByteArray?,
    )

    data class Issuer(
        /**
         * Name of the issuer.
         */
        val name: String,

        /**
         * Public key that is used by the card issuer to sign IssuerData field.
         */
        val publicKey: ByteArray,
    )

    enum class LinkedTerminalStatus {
        Current,
        Other,
        None,
    }

    /**
     * Status of the card and its wallet.
     */
    enum class Status(val code: Int) {
        NotPersonalized(code = 0),
        Empty(code = 1),
        Loaded(code = 2),
        Purged(code = 3),
        ;

        companion object {
            private val values = values()
            fun byCode(code: Int): Status? = values.find { it.code == code }
        }
    }

    sealed class BackupStatus {
        object NoBackup : BackupStatus()
        data class CardLinked(val cardsCount: Int) : BackupStatus()
        data class Active(val cardsCount: Int) : BackupStatus()

        val isActive: Boolean
            get() = this is Active || this is CardLinked

        val linkedCardsCount: Int
            get() = when (this) {
                is Active -> cardsCount
                is CardLinked -> cardsCount
                NoBackup -> 0
            }

        fun toRawStatus(): BackupRawStatus {
            return when (this) {
                NoBackup -> BackupRawStatus.NoBackup
                is CardLinked -> BackupRawStatus.CardLinked
                is Active -> BackupRawStatus.Active
            }
        }

        companion object {
            fun from(rawStatus: BackupRawStatus, cardsCount: Int? = null): BackupStatus? {
                return when (rawStatus) {
                    BackupRawStatus.NoBackup -> NoBackup
                    BackupRawStatus.CardLinked -> cardsCount?.let {
                        CardLinked(cardsCount)
                    }
                    BackupRawStatus.Active -> cardsCount?.let {
                        Active(cardsCount)
                    }
                }
            }
        }
    }

    /**
     * Status of backup
     */
    enum class BackupRawStatus(val code: Int) {
        NoBackup(0),
        CardLinked(1),
        Active(2),
        ;

        companion object {
            private val values = values()
            fun byCode(code: Int): BackupRawStatus? = values.find { it.code == code }
        }
    }

    data class Settings internal constructor(
        /**
         * Delay in milliseconds before executing a command that affects any sensitive data or wallets on the card
         */
        val securityDelay: Int,

        /**
         * Maximum number of wallets that can be created for this card
         */
        val maxWalletsCount: Int,

        /**
         * Is allowed to change access code
         */
        val isSettingAccessCodeAllowed: Boolean,

        /**
         * Is  allowed to change passcode
         */
        val isSettingPasscodeAllowed: Boolean,

        /**
         * Is allowed to remove access code
         */
        val isRemovingUserCodesAllowed: Boolean,

        /**
         * Is LinkedTerminal feature enabled
         */
        val isLinkedTerminalEnabled: Boolean,

        /**
         * Is backup feature available
         */
        val isBackupAllowed: Boolean,

        /**
         * Is allowed to import  keys. COS. v6+
         */
        val isKeysImportAllowed: Boolean,

        /**
         * All  encryption modes supported by the card
         */
        val supportedEncryptionModes: List<EncryptionMode>,

        /**
         * Is allowed to write files
         */
        val isFilesAllowed: Boolean,

        /**
         * Is allowed to use hd wallet
         */
        val isHDWalletAllowed: Boolean,

        /**
         * Is allowed to delete wallet. COS before v4
         * Default value used only for Moshi
         */
        @Transient
        internal val isPermanentWallet: Boolean = false,

        /**
         * Is overwriting issuer extra data restricted
         * Default value used only for Moshi
         */
        @Transient
        internal val isOverwritingIssuerExtraDataRestricted: Boolean = false,

        /**
         * Card's default signing methods according personalization.
         * Default value used only for Moshi
         */
        @Transient
        internal val defaultSigningMethods: SigningMethod? = null,

        /**
         * Card's default curve according personalization.
         * Default value used only for Moshi
         */
        @Transient
        internal val defaultCurve: EllipticCurve? = null,

        /**
         * Default value used only for Moshi
         */
        @Transient
        internal val isIssuerDataProtectedAgainstReplay: Boolean = false,

        /**
         * Default value used only for Moshi
         */
        @Transient
        internal val isSelectBlockchainAllowed: Boolean = false,
    ) {

        internal constructor(
            securityDelay: Int,
            maxWalletsCount: Int,
            mask: SettingsMask,
            defaultSigningMethods: SigningMethod? = null,
            defaultCurve: EllipticCurve? = null,
        ) : this(
            securityDelay = securityDelay,
            maxWalletsCount = maxWalletsCount,
            isSettingAccessCodeAllowed = mask.contains(SettingsMask.Code.AllowSetPIN1),
            isSettingPasscodeAllowed = mask.contains(SettingsMask.Code.AllowSetPIN2),
            isRemovingUserCodesAllowed = !mask.contains(SettingsMask.Code.ProhibitDefaultPIN1),
            isLinkedTerminalEnabled = mask.contains(SettingsMask.Code.SkipSecurityDelayIfValidatedByLinkedTerminal),
            isHDWalletAllowed = mask.contains(SettingsMask.Code.AllowHDWallets),
            isBackupAllowed = mask.contains(SettingsMask.Code.AllowBackup),
            isKeysImportAllowed = mask.contains(SettingsMask.Code.AllowKeysImport),
            supportedEncryptionModes = createEncryptionModes(mask),
            isPermanentWallet = mask.contains(SettingsMask.Code.PermanentWallet),
            isOverwritingIssuerExtraDataRestricted = mask.contains(SettingsMask.Code.RestrictOverwriteIssuerExtraData),
            defaultSigningMethods = defaultSigningMethods,
            defaultCurve = defaultCurve,
            isIssuerDataProtectedAgainstReplay = mask.contains(SettingsMask.Code.ProtectIssuerDataAgainstReplay),
            isSelectBlockchainAllowed = mask.contains(SettingsMask.Code.AllowSelectBlockchain),
            isFilesAllowed = !mask.contains(SettingsMask.Code.DisableFiles),
        )

        fun updated(mask: SettingsMask): Settings {
            return Settings(
                securityDelay = this.securityDelay,
                maxWalletsCount = this.maxWalletsCount,
                mask = mask,
                defaultSigningMethods = this.defaultSigningMethods,
                defaultCurve = this.defaultCurve,
            )
        }

        companion object {
            private fun createEncryptionModes(mask: SettingsMask): List<EncryptionMode> {
                val modes = mutableListOf(EncryptionMode.Strong)
                if (mask.contains(SettingsMask.Code.AllowFastEncryption)) modes.add(EncryptionMode.Fast)
                if (mask.contains(SettingsMask.Code.AllowUnencrypted)) modes.add(EncryptionMode.None)
                return modes.toList()
            }
        }
    }

    class SettingsMask(override val rawValue: Int) : BaseMask() {

        override val values: List<Code> = Code.values().toList()

        fun toWalletSettingsMask(): CardWallet.SettingsMask = CardWallet.SettingsMask(rawValue)

        enum class Code(override val value: Int) : Mask.Code {
            IsReusable(value = 0x0001),
            UseActivation(value = 0x0002),
            PermanentWallet(value = 0x0004),
            UseBlock(value = 0x0008),
            AllowSetPIN1(value = 0x0010),
            AllowSetPIN2(value = 0x0020),
            UseCvc(value = 0x0040),
            ProhibitDefaultPIN1(value = 0x0080),
            UseOneCommandAtTime(value = 0x0100),
            UseNDEF(value = 0x0200),
            UseDynamicNDEF(value = 0x0400),
            SmartSecurityDelay(value = 0x0800),
            AllowUnencrypted(value = 0x1000),
            AllowFastEncryption(value = 0x2000),
            ProtectIssuerDataAgainstReplay(value = 0x4000),
            RestrictOverwriteIssuerExtraData(value = 0x00100000),
            AllowSelectBlockchain(value = 0x8000),
            DisablePrecomputedNDEF(value = 0x00010000),
            SkipSecurityDelayIfValidatedByLinkedTerminal(value = 0x00080000),
            SkipCheckPIN2CVCIfValidatedByIssuer(value = 0x00040000),
            SkipSecurityDelayIfValidatedByIssuer(value = 0x00020000),
            DisableIssuerData(value = 0x01000000),
            DisableUserData(value = 0x02000000),
            DisableFiles(value = 0x04000000),
            AllowHDWallets(value = 0x00200000),
            AllowBackup(value = 0x00400000),
            AllowKeysImport(value = 0x00800000),
        }
    }
}

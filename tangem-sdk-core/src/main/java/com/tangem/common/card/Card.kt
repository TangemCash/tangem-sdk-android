package com.tangem.common.card

import com.squareup.moshi.JsonClass
import com.tangem.common.BaseMask
import com.tangem.common.Mask
import com.tangem.operations.CommandResponse
import com.tangem.operations.attestation.Attestation
import com.tangem.operations.read.ReadCommand
import java.util.*

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
     * When this value is `current`, it means that the application is linked to the card,
     * and COS will not enforce security delay if `SignCommand` will be called
     * with `TlvTag.TerminalTransactionSignature` parameter containing a correct signature of raw data
     * to be signed made with `TlvTag.TerminalPublicKey`.
     * */
    val linkedTerminalStatus: LinkedTerminalStatus,

    /**
     * PIN2 (aka Passcode) is set.
     * Available only for cards with COS v. 4.0 and higher.
     */
    val isPasscodeSet: Boolean?,

        //TODO: isAccessCodeSet

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
        val signature: ByteArray?
    )

    data class Issuer(
        /**
         * Name of the issuer.
         */
        val name: String,

        /**
         * Public key that is used by the card issuer to sign IssuerData field.
         */
        val publicKey: ByteArray
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
        NotPersonalized(0),
        Empty(1),
        Loaded(2),
        Purged(3);

        companion object {
            private val values = values()
            fun byCode(code: Int): Status? = values.find { it.code == code }
        }
    }

    class Settings internal constructor(
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
        val isRemovingAccessCodeAllowed: Boolean,

        /**
         * Is LinkedTerminal feature enabled
         */
        val isLinkedTerminalEnabled: Boolean,

        /**
         * All  encryption modes supported by the card
         */
        val supportedEncryptionModes: List<EncryptionMode>,

        /**
         * Is allowed to delete wallet. COS before v4
         */
        internal val isPermanentWallet: Boolean,

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
            defaultCurve: EllipticCurve? = null
        ) : this(
                securityDelay,
                maxWalletsCount,
                mask.contains(SettingsMask.Code.AllowSetPIN1),
                mask.contains(SettingsMask.Code.AllowSetPIN2),
                mask.contains(SettingsMask.Code.ProhibitDefaultPIN1),
                mask.contains(SettingsMask.Code.SkipSecurityDelayIfValidatedByLinkedTerminal),
                createEncryptionModes(mask),
                mask.contains(SettingsMask.Code.PermanentWallet),
                mask.contains(SettingsMask.Code.RestrictOverwriteIssuerExtraData),
                defaultSigningMethods,
                defaultCurve,
                mask.contains(SettingsMask.Code.ProtectIssuerDataAgainstReplay),
                mask.contains(SettingsMask.Code.AllowSelectBlockchain),
        )

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
            IsReusable(0x0001),
            UseActivation(0x0002),
            PermanentWallet(0x0004),
            UseBlock(0x0008),
            AllowSetPIN1(0x0010),
            AllowSetPIN2(0x0020),
            UseCvc(0x0040),
            ProhibitDefaultPIN1(0x0080),
            UseOneCommandAtTime(0x0100),
            UseNDEF(0x0200),
            UseDynamicNDEF(0x0400),
            SmartSecurityDelay(0x0800),
            AllowUnencrypted(0x1000),
            AllowFastEncryption(0x2000),
            ProtectIssuerDataAgainstReplay(0x4000),
            RestrictOverwriteIssuerExtraData(0x00100000),
            AllowSelectBlockchain(0x8000),
            DisablePrecomputedNDEF(0x00010000),
            SkipSecurityDelayIfValidatedByLinkedTerminal(0x00080000),
            SkipCheckPIN2CVCIfValidatedByIssuer(0x00040000),
            SkipSecurityDelayIfValidatedByIssuer(0x00020000),
            DisableIssuerData(0x01000000),
            DisableUserData(0x02000000),
            DisableFiles(0x04000000),
            AllowHDWallets(0x00200000),
            AllowBackup(0x00400000),
        }
    }
}

data class CardWallet(
    /**
     *  Wallet's public key.
     */
    val publicKey: ByteArray,

    /**
     * Optional chain code for BIP32 derivation.
     */
    val chainCode: ByteArray?,

    /**
     *  Elliptic curve used for all wallet key operations.
     */
    val curve: EllipticCurve,

    /**
     *  Wallet's settings
     */
    val settings: Settings,

    /**
     * Total number of signed hashes returned by the wallet since its creation
     * COS 1.16+
     */
    val totalSignedHashes: Int?,

    /**
     * Remaining number of `Sign` operations before the wallet will stop signing any data.
     * Note: This counter were deprecated for cards with COS 4.0 and higher
     */
    val remainingSignatures: Int?,

    /**
     *  Index of the wallet in the card storage
     */
    val index: Int
) {
    /**
     * Status of the wallet.
     */
    enum class Status(val code: Int) {
        /**
         * Wallet not created
         */
        Empty(1),

        /**
         * Wallet created and can be used for signing
         */
        Loaded(2),

        /**
         * Wallet was purged and can't be recreated or used for signing
         */
        Purged(3);

        companion object {
            fun byCode(code: Int): Status? {
                return values().find { it.code == code }
            }
        }
    }

    data class Settings internal constructor(
        /**
         * If true, erasing the wallet will be prohibited
         */
        val isPermanent: Boolean
    ) {
        internal constructor(
            mask: SettingsMask
        ) : this(mask.contains(SettingsMask.Code.IsPermanent))
    }

    class SettingsMask(override var rawValue: Int) : BaseMask() {

        override val values: List<Code> = Code.values().toList()

        enum class Code(override val value: Int) : Mask.Code {
            IsReusable(0x0001),
            IsPermanent(0x0004)
        }
    }
}
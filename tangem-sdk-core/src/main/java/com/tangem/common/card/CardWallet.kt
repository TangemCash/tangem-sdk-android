package com.tangem.common.card

import com.squareup.moshi.JsonClass
import com.tangem.common.BaseMask
import com.tangem.common.Mask
import com.tangem.common.card.CardWallet.Status.Companion.initExtendedPublicKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey

@JsonClass(generateAdapter = true)
data class CardWallet(
    /**
     * Wallet's public key.
     * For [EllipticCurve.Secp256k1], the key can be compressed or uncompressed.
     * Use [com.tangem.crypto.Secp256k1Key] for any conversions.
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
    val index: Int,

    /**
     *  Has this key been imported to a card. E.g. from seed phrase
     */
    val isImported: Boolean,

    /**
     *  Shows whether this wallet has a backup
     */
    val hasBackup: Boolean,

    /**
     * Derived keys according to [com.tangem.common.core.Config.defaultDerivationPaths]
     */
    val derivedKeys: Map<DerivationPath, ExtendedPublicKey> = emptyMap(),

) {

    val extendedPublicKey: ExtendedPublicKey?
        get() = initExtendedPublicKey(publicKey, chainCode)

    /**
     * Status of the wallet.
     */
    enum class Status(val code: Int) {
        /**
         * Wallet not created
         */
        Empty(code = 1),

        /**
         * Wallet created and can be used for signing
         */
        Loaded(code = 2),

        /**
         * Wallet was purged and can't be recreated or used for signing
         */
        Purged(code = 3),

        /**
         * Empty wallet created because of error during backup
         */
        EmptyBackedUp(code = 0x81),

        /**
         * Wallet created and can be used for signing, backup data read
         */
        BackedUp(code = 0x82),

        /**
         *Wallet was purged and can't be recreated or used for signing, but backup data read and wallet can be usable on backup card
         */
        BackedUpAndPurged(code = 0x83),

        /**
         * Wallet was imported
         */
        Imported(code = 0x42),

        /**
         * Wallet was imported and backed up
         */
        BackedUpImported(code = 0xC2),
        ;

        val isBackedUp: Boolean
            get() = when (this) {
                BackedUp, BackedUpAndPurged, BackedUpImported -> true
                else -> false
            }

        val isImported: Boolean
            get() = when (this) {
                Imported, BackedUpImported -> true
                else -> false
            }

        val isAvailable: Boolean
            get() = when (this) {
                Empty, Purged, BackedUpAndPurged, EmptyBackedUp -> false
                else -> true
            }

        companion object {
            private val values = values()
            fun byCode(code: Int): Status? {
                return values.find { it.code.toByte() == code.toByte() }
            }

            fun initExtendedPublicKey(publicKey: ByteArray, chainCode: ByteArray?): ExtendedPublicKey? {
                val chainCode = chainCode ?: return null

                return ExtendedPublicKey(publicKey, chainCode)
            }
        }
    }

    data class Settings(
        /**
         * If true, erasing the wallet will be prohibited
         */
        val isPermanent: Boolean,
    ) {
        internal constructor(
            mask: SettingsMask,
        ) : this(mask.contains(SettingsMask.Code.IsPermanent))
    }

    class SettingsMask(override var rawValue: Int) : BaseMask() {

        override val values: List<Code> = Code.values().toList()

        enum class Code(override val value: Int) : Mask.Code {
            IsReusable(value = 0x0001),
            IsPermanent(value = 0x0004),
        }
    }
}

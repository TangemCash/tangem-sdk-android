package com.tangem.sdk.extensions

import android.content.Context
import androidx.annotation.StringRes
import com.tangem.common.core.TangemSdkError
import com.tangem.sdk.R

/**
 * Model to store localized error description
 */
data class TangemSdkErrorDescription(
    val resId: Int? = null,
    val args: List<Type> = emptyList(),
) {
    sealed class Type {

        abstract val value: Any

        data class StringType(override val value: String) : Type()

        data class StringResId(@StringRes override val value: Int) : Type()
    }
}

/**
 * Extension function for [TangemSdkError] to get localized description resources
 */
@Suppress("CyclomaticComplexMethod", "LongMethod")
fun TangemSdkError.localizedDescriptionRes(): TangemSdkErrorDescription {
    return when (this) {
        is TangemSdkError.EncodingFailedTypeMismatch,
        is TangemSdkError.EncodingFailed,
        is TangemSdkError.DecodingFailedMissingTag,
        is TangemSdkError.DecodingFailedTypeMismatch,
        is TangemSdkError.DecodingFailed,
        is TangemSdkError.CryptoUtilsError,
        is TangemSdkError.NetworkError,
        is TangemSdkError.ExceptionError,
        is TangemSdkError.HDWalletDisabled,
        is TangemSdkError.FileSettingsUnsupported,
        is TangemSdkError.FilesDisabled,
        is TangemSdkError.FilesIsEmpty,
        is TangemSdkError.Underlying,
        is TangemSdkError.AccessCodeOrPasscodeRequired,
        is TangemSdkError.BackupFailedCardNotLinked,
        is TangemSdkError.BackupServiceInvalidState,
        is TangemSdkError.CertificateSignatureRequired,
        is TangemSdkError.EmptyBackupCards,
        is TangemSdkError.MissingPrimaryAttestSignature,
        is TangemSdkError.MissingPrimaryCard,
        is TangemSdkError.NoBackupCardForIndex,
        is TangemSdkError.NoBackupDataForCard,
        is TangemSdkError.ResetBackupFailedHasBackedUpWallets,
        is TangemSdkError.ResetPinNoCardsToReset,
        is TangemSdkError.TooMuchBackupCards,
        is TangemSdkError.UserForgotTheCode,
        is TangemSdkError.AuthenticationFailed,
        is TangemSdkError.AuthenticationUnavailable,
        is TangemSdkError.AuthenticationLockout,
        is TangemSdkError.AuthenticationPermanentLockout,
        is TangemSdkError.AuthenticationCanceled,
        is TangemSdkError.AuthenticationAlreadyInProgress,
        is TangemSdkError.KeyGenerationException,
        is TangemSdkError.MnemonicException,
        is TangemSdkError.KeysImportDisabled,
        is TangemSdkError.WalletAlreadyCreated,
        is TangemSdkError.KeystoreInvalidated,
        is TangemSdkError.UnsupportedCurve,
        is TangemSdkError.InvalidParams,
        is TangemSdkError.SerializeCommandError,
        is TangemSdkError.DeserializeApduFailed,
        is TangemSdkError.UnknownStatus,
        is TangemSdkError.ErrorProcessingCommand,
        is TangemSdkError.InvalidState,
        is TangemSdkError.InsNotSupported,
        is TangemSdkError.NeedEncryption,
        is TangemSdkError.UnknownError,
        is TangemSdkError.MissingPreflightRead,
        is TangemSdkError.AlreadyPersonalized,
        is TangemSdkError.Busy,
        is TangemSdkError.CardError,
        is TangemSdkError.CardWithMaxZeroWallets,
        is TangemSdkError.OverwritingDataIsProhibited,
        is TangemSdkError.DataCannotBeWritten,
        is TangemSdkError.DataSizeTooLarge,
        is TangemSdkError.EmptyHashes,
        is TangemSdkError.InvalidResponse,
        is TangemSdkError.MissingCounter,
        is TangemSdkError.MaxNumberOfWalletsCreated,
        is TangemSdkError.NotPersonalized,
        is TangemSdkError.TagLost,
        is TangemSdkError.VerificationFailed,
        is TangemSdkError.WalletCannotBeCreated,
        is TangemSdkError.UnsupportedWalletConfig,
        is TangemSdkError.WalletIsNotCreated,
        is TangemSdkError.WalletIsPurged,
        is TangemSdkError.HashSizeMustBeEqual,
        is TangemSdkError.SignHashesNotAvailable,
        is TangemSdkError.CardVerificationFailed,
        is TangemSdkError.NonHardenedDerivationNotSupported,
        -> TangemSdkErrorDescription()

        is TangemSdkError.BackupFailedEmptyWallets,
        is TangemSdkError.BackupFailedHDWalletSettings,
        is TangemSdkError.BackupFailedNotEnoughCurves,
        is TangemSdkError.BackupFailedNotEnoughWallets,
        is TangemSdkError.BackupFailedWrongIssuer,
        is TangemSdkError.BackupNotAllowed,
        is TangemSdkError.BackupFailedFirmware,
        is TangemSdkError.BackupFailedIncompatibleBatch,
        is TangemSdkError.BackupFailedIncompatibleFirmware,
        is TangemSdkError.BackupFailedKeysImportSettings,

        -> TangemSdkErrorDescription(
            resId = R.string.error_backup_wrong_card,
            args = listOf(TangemSdkErrorDescription.Type.StringType(code.toString())),
        )

        is TangemSdkError.BackupFailedAlreadyCreated -> {
            TangemSdkErrorDescription(resId = R.string.error_backup_failed_already_created)
        }

        is TangemSdkError.NoActiveBackup -> {
            TangemSdkErrorDescription(resId = R.string.error_no_active_backup)
        }

        is TangemSdkError.ExtendedLengthNotSupported ->
            TangemSdkErrorDescription(resId = R.string.error_extended_apdu_not_supported)

        is TangemSdkError.AlreadyCreated ->
            TangemSdkErrorDescription(resId = R.string.error_already_created)

        is TangemSdkError.PurgeWalletProhibited ->
            TangemSdkErrorDescription(resId = R.string.error_purge_prohibited)

        is TangemSdkError.AccessCodeCannotBeChanged -> TangemSdkErrorDescription(
            resId = R.string.error_pin_cannot_be_changed_format,
            args = listOf(TangemSdkErrorDescription.Type.StringResId(R.string.pin1)),
        )

        is TangemSdkError.PasscodeCannotBeChanged -> TangemSdkErrorDescription(
            resId = R.string.error_pin_cannot_be_changed_format,
            args = listOf(TangemSdkErrorDescription.Type.StringResId(R.string.pin2)),
        )

        is TangemSdkError.WrongPasscode -> TangemSdkErrorDescription(
            resId = R.string.error_wrong_pin_format,
            args = listOf(TangemSdkErrorDescription.Type.StringResId(R.string.pin2)),
        )

        is TangemSdkError.WrongAccessCode -> TangemSdkErrorDescription(
            resId = R.string.error_wrong_pin_format,
            args = listOf(TangemSdkErrorDescription.Type.StringResId(R.string.pin1)),
        )

        is TangemSdkError.BackupCardAlreadyAdded,
        is TangemSdkError.BackupCardRequired,
        -> TangemSdkErrorDescription(resId = R.string.error_backup_card_already_added)

        is TangemSdkError.AccessCodeCannotBeDefault ->
            TangemSdkErrorDescription(
                resId = R.string.error_pin_cannot_be_default_format,
                args = listOf(TangemSdkErrorDescription.Type.StringResId(R.string.pin1)),
            )

        is TangemSdkError.NoRemainingSignatures ->
            TangemSdkErrorDescription(resId = R.string.error_no_remaining_signatures)

        is TangemSdkError.NotActivated ->
            TangemSdkErrorDescription(resId = R.string.error_not_activated)

        is TangemSdkError.UserCancelled ->
            TangemSdkErrorDescription(resId = R.string.error_user_cancelled)

        is TangemSdkError.WrongCardNumber -> TangemSdkErrorDescription(
            resId = R.string.error_wrong_card_number_with_card_id,
            args = listOf(TangemSdkErrorDescription.Type.StringType(cardId)),
        )

        is TangemSdkError.WrongCardType ->
            TangemSdkErrorDescription(resId = R.string.error_wrong_card_type)

        is TangemSdkError.NotSupportedFirmwareVersion ->
            TangemSdkErrorDescription(resId = R.string.error_old_firmware)

        is TangemSdkError.WalletNotFound ->
            TangemSdkErrorDescription(resId = R.string.wallet_not_found)

        is TangemSdkError.BackupFailedNotEmptyWallets ->
            TangemSdkErrorDescription(resId = R.string.error_backup_not_empty_wallets)

        is TangemSdkError.ResetPinWrongCard ->
            TangemSdkErrorDescription(
                resId = R.string.error_reset_wrong_card,
                args = listOf(TangemSdkErrorDescription.Type.StringType(code.toString())),
            )

        is TangemSdkError.UserCodeRecoveryDisabled ->
            TangemSdkErrorDescription(resId = R.string.error_user_code_recovery_disabled)

        is TangemSdkError.IssuerSignatureLoadingFailed ->
            TangemSdkErrorDescription(resId = R.string.issuer_signature_loading_failed)

        is TangemSdkError.FileNotFound -> TangemSdkErrorDescription(resId = R.string.error_file_not_found)

        is TangemSdkError.AccessCodeRequired -> TangemSdkErrorDescription(
            resId = R.string.error_pin_required_format,
            args = listOf(TangemSdkErrorDescription.Type.StringResId(R.string.pin1)),
        )

        is TangemSdkError.PasscodeRequired -> TangemSdkErrorDescription(
            resId = R.string.error_pin_required_format,
            args = listOf(TangemSdkErrorDescription.Type.StringResId(R.string.pin2)),
        )
    }
}

/**
 * Extension function for [TangemSdkError] to get localized description
 */
@Suppress("LongMethod", "ComplexMethod")
fun TangemSdkError.localizedDescription(context: Context): String {
    val resId = localizedDescriptionRes().resId
    val args = localizedDescriptionRes().args
    return if (resId == null) {
        context.getString(R.string.generic_error_code, code.toString())
    } else {
        if (args.isNotEmpty()) {
            val argsArray = args.map {
                when (it) {
                    is TangemSdkErrorDescription.Type.StringResId -> context.getString(it.value)
                    else -> it.value
                }
            }.toTypedArray()
            context.getString(resId, *argsArray)
        } else {
            context.getString(resId)
        }
    }
}

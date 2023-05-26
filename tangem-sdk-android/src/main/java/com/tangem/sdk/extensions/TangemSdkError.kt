package com.tangem.sdk.extensions

import android.content.Context
import com.tangem.common.core.TangemSdkError
import com.tangem.sdk.R

@Suppress("LongMethod", "ComplexMethod")
fun TangemSdkError.localizedDescription(context: Context): String {
    val resId = when (this) {
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
        is TangemSdkError.NoActiveBackup,
        is TangemSdkError.NoBackupCardForIndex,
        is TangemSdkError.NoBackupDataForCard,
        is TangemSdkError.ResetBackupFailedHasBackedUpWallets,
        is TangemSdkError.ResetPinNoCardsToReset,
        is TangemSdkError.TooMuchBackupCards,
        is TangemSdkError.UserForgotTheCode,
        is TangemSdkError.BiometricsAuthenticationFailed,
        is TangemSdkError.BiometricsUnavailable,
        is TangemSdkError.BiometricsAuthenticationLockout,
        is TangemSdkError.BiometricsAuthenticationPermanentLockout,
        is TangemSdkError.UserCanceledBiometricsAuthentication,
        is TangemSdkError.BiometricCryptographyOperationFailed,
        is TangemSdkError.InvalidBiometricCryptographyKey,
        is TangemSdkError.KeyGenerationException,
        is TangemSdkError.MnemonicException,
        is TangemSdkError.KeysImportDisabled,
        is TangemSdkError.WalletAlreadyCreated,
        is TangemSdkError.BiometricCryptographyKeyInvalidated,
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
        -> null

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
        is TangemSdkError.BackupFailedAlreadyCreated,
        -> R.string.error_backup_wrong_card

        is TangemSdkError.ExtendedLengthNotSupported -> R.string.error_extended_apdu_not_supported
        is TangemSdkError.AlreadyCreated -> R.string.error_already_created
        is TangemSdkError.PurgeWalletProhibited -> R.string.error_purge_prohibited
        is TangemSdkError.AccessCodeCannotBeChanged,
        is TangemSdkError.PasscodeCannotBeChanged,
        -> R.string.error_pin_cannot_be_changed_format

        is TangemSdkError.WrongPasscode,
        is TangemSdkError.WrongAccessCode,
        -> R.string.error_wrong_pin_format

        is TangemSdkError.BackupCardAlreadyAdded,
        is TangemSdkError.BackupCardRequired,
        -> R.string.error_backup_card_already_added

        is TangemSdkError.AccessCodeCannotBeDefault -> R.string.error_pin_cannot_be_default_format
        is TangemSdkError.NoRemainingSignatures -> R.string.error_no_remaining_signatures
        is TangemSdkError.NotActivated -> R.string.error_not_activated
        is TangemSdkError.UserCancelled -> R.string.error_user_cancelled
        is TangemSdkError.WrongCardNumber -> R.string.error_wrong_card_number_with_card_id
        is TangemSdkError.WrongCardType -> R.string.error_wrong_card_type
        is TangemSdkError.NotSupportedFirmwareVersion -> R.string.error_old_firmware
        is TangemSdkError.WalletNotFound -> R.string.wallet_not_found
        is TangemSdkError.BackupFailedNotEmptyWallets -> R.string.error_backup_not_empty_wallets
        is TangemSdkError.ResetPinWrongCard -> R.string.error_reset_wrong_card
        is TangemSdkError.UserCodeRecoveryDisabled -> R.string.error_user_code_recovery_disabled
        is TangemSdkError.IssuerSignatureLoadingFailed -> R.string.issuer_signature_loading_failed
        is TangemSdkError.FileNotFound -> R.string.error_file_not_found
        is TangemSdkError.AccessCodeRequired, is TangemSdkError.PasscodeRequired -> R.string.error_pin_required_format
    }

    return if (resId == null) {
        context.getString(R.string.generic_error_code, code.toString())
    } else {
        when (this) {
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
            is TangemSdkError.BackupFailedAlreadyCreated,
            is TangemSdkError.ResetPinWrongCard,
            -> context.getString(resId, code.toString())

            is TangemSdkError.AccessCodeCannotBeChanged,
            is TangemSdkError.AccessCodeCannotBeDefault,
            is TangemSdkError.WrongAccessCode,
            -> context.getString(resId, context.getString(R.string.pin1))

            is TangemSdkError.PasscodeCannotBeChanged,
            is TangemSdkError.WrongPasscode,
            -> context.getString(resId, context.getString(R.string.pin2))

            is TangemSdkError.WrongCardNumber -> context.getString(resId, cardId)

            else -> context.getString(resId)
        }
    }
}

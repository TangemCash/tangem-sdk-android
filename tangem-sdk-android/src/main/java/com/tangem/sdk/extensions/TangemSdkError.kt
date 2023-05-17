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
        is TangemSdkError.IssuerSignatureLoadingFailed,
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
        is TangemSdkError.UserCodeRecoveryDisabled,
        is TangemSdkError.BiometricCryptographyKeyInvalidated,
        is TangemSdkError.UnsupportedCurve,
        is TangemSdkError.InvalidParams,
        -> null

        is TangemSdkError.SerializeCommandError,
        is TangemSdkError.DeserializeApduFailed,
        is TangemSdkError.UnknownStatus,
        is TangemSdkError.ErrorProcessingCommand,
        is TangemSdkError.InvalidState,
        is TangemSdkError.InsNotSupported,
        is TangemSdkError.NeedEncryption,
        is TangemSdkError.FileNotFound,
        is TangemSdkError.AccessCodeRequired,
        is TangemSdkError.PasscodeRequired,
        is TangemSdkError.UnknownError,
        is TangemSdkError.MissingPreflightRead,
        -> R.string.error_operation

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

        is TangemSdkError.TagLost -> R.string.error_tag_lost
        is TangemSdkError.ExtendedLengthNotSupported -> R.string.error_extended_apdu_not_supported
        is TangemSdkError.AlreadyPersonalized -> R.string.error_already_personalized
        is TangemSdkError.CannotBeDepersonalized -> R.string.error_cannot_be_depersonalized
        is TangemSdkError.AlreadyCreated -> R.string.error_already_created
        is TangemSdkError.PurgeWalletProhibited -> R.string.error_purge_prohibited
        is TangemSdkError.AccessCodeCannotBeChanged,
        is TangemSdkError.PasscodeCannotBeChanged,
        -> R.string.error_pin_cannot_be_changed_format

        is TangemSdkError.HashSizeMustBeEqual,
        is TangemSdkError.SignHashesNotAvailable,
        is TangemSdkError.TooManyHashesInOneTransaction,
        -> R.string.error_cannot_be_signed

        is TangemSdkError.OverwritingDataIsProhibited,
        is TangemSdkError.DataCannotBeWritten,
        -> R.string.error_data_cannot_be_written

        is TangemSdkError.WrongPasscode,
        is TangemSdkError.WrongAccessCode,
        -> R.string.error_wrong_pin_format

        is TangemSdkError.BackupCardAlreadyAdded,
        is TangemSdkError.BackupCardRequired,
        -> R.string.error_backup_card_already_added

        is TangemSdkError.AccessCodeCannotBeDefault -> R.string.error_pin_cannot_be_default_format
        is TangemSdkError.NoRemainingSignatures -> R.string.error_no_remaining_signatures
        is TangemSdkError.EmptyHashes -> R.string.error_empty_hashes
        is TangemSdkError.WalletIsNotCreated -> R.string.error_wallet_is_not_created
        is TangemSdkError.WalletIsPurged -> R.string.error_wallet_is_purged
        is TangemSdkError.NotPersonalized -> R.string.error_not_personalized
        is TangemSdkError.NotActivated -> R.string.error_not_activated
        is TangemSdkError.VerificationFailed -> R.string.error_verification_failed
        is TangemSdkError.DataSizeTooLarge -> R.string.error_data_size_too_large
        is TangemSdkError.ExtendedDataSizeTooLarge -> R.string.error_data_size_too_large_extended
        is TangemSdkError.MissingCounter -> R.string.error_missing_counter
        is TangemSdkError.MissingIssuerPubicKey -> R.string.error_missing_issuer_public_key
        is TangemSdkError.CardVerificationFailed -> R.string.error_card_verification_failed
        is TangemSdkError.UserCancelled -> R.string.error_user_cancelled
        is TangemSdkError.Busy -> R.string.error_busy
        is TangemSdkError.WrongCardNumber -> R.string.error_wrong_card_number
        is TangemSdkError.WrongCardType -> R.string.error_wrong_card_type
        is TangemSdkError.CardError -> R.string.error_card_error
        is TangemSdkError.InvalidResponse -> R.string.error_invalid_response
        is TangemSdkError.NotSupportedFirmwareVersion -> R.string.error_old_firmware
        is TangemSdkError.MaxNumberOfWalletsCreated -> R.string.error_no_space_for_new_wallet
        is TangemSdkError.CardReadWrongWallet -> R.string.error_card_read_wrong_wallet
        is TangemSdkError.WalletNotFound -> R.string.wallet_not_found
        is TangemSdkError.CardWithMaxZeroWallets -> R.string.error_card_with_max_zero_wallets
        is TangemSdkError.WalletError -> R.string.error_wallet_error
        is TangemSdkError.UnsupportedWalletConfig -> R.string.error_wallet_index_not_correct
        is TangemSdkError.WalletCannotBeCreated -> R.string.error_wallet_cannot_be_created
        is TangemSdkError.BackupFailedNotEmptyWallets -> R.string.error_backup_not_empty_wallets
        is TangemSdkError.ResetPinWrongCard -> R.string.error_reset_wrong_card
        is TangemSdkError.WrongInteractionMode -> R.string.error_wrong_interaction_mode
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

            else -> context.getString(resId)
        }
    }
}

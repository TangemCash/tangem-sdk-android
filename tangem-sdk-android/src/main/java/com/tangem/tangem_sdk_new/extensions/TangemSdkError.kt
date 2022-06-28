package com.tangem.tangem_sdk_new.extensions

import android.content.Context
import com.tangem.common.core.TangemSdkError
import com.tangem.tangem_sdk_new.R

fun TangemSdkError.localizedDescription(context: Context): String {
    return when (this) {
        is TangemSdkError.TagLost ->
            context.getString(R.string.error_tag_lost)
        is TangemSdkError.ExtendedLengthNotSupported ->
            context.getString(R.string.error_extended_apdu_not_supported)
        is TangemSdkError.SerializeCommandError ->
            context.getString(R.string.error_operation)
        is TangemSdkError.DeserializeApduFailed ->
            context.getString(R.string.error_operation)
        is TangemSdkError.UnknownStatus ->
            context.getString(R.string.error_operation)
        is TangemSdkError.ErrorProcessingCommand ->
            context.getString(R.string.error_operation)
        is TangemSdkError.InvalidState ->
            context.getString(R.string.error_operation)
        is TangemSdkError.InsNotSupported ->
            context.getString(R.string.error_operation)
        is TangemSdkError.InvalidParams ->
            context.getString(R.string.error_operation)
        is TangemSdkError.NeedEncryption ->
            context.getString(R.string.error_operation)
        is TangemSdkError.FileNotFound ->
            context.getString(R.string.error_operation)
        is TangemSdkError.AlreadyPersonalized ->
            context.getString(R.string.error_already_personalized)
        is TangemSdkError.CannotBeDepersonalized ->
            context.getString(R.string.error_cannot_be_depersonalized)
        is TangemSdkError.AccessCodeRequired ->
            context.getString(R.string.error_operation)
        is TangemSdkError.AlreadyCreated ->
            context.getString(R.string.error_already_created)
        is TangemSdkError.PurgeWalletProhibited ->
            context.getString(R.string.error_purge_prohibited)
        is TangemSdkError.AccessCodeCannotBeChanged ->
            context.getString(
                R.string.error_pin_cannot_be_changed_format,
                context.getString(R.string.pin1)
            )
        is TangemSdkError.PasscodeCannotBeChanged ->
            context.getString(
                R.string.error_pin_cannot_be_changed_format,
                context.getString(R.string.pin2)
            )
        is TangemSdkError.AccessCodeCannotBeDefault ->
            context.getString(
                R.string.error_pin_cannot_be_default_format,
                context.getString(R.string.pin1)
            )
        is TangemSdkError.NoRemainingSignatures ->
            context.getString(R.string.error_no_remaining_signatures)
        is TangemSdkError.EmptyHashes ->
            context.getString(R.string.error_empty_hashes)
        is TangemSdkError.HashSizeMustBeEqual ->
            context.getString(R.string.error_cannot_be_signed)
        is TangemSdkError.WalletIsNotCreated ->
            context.getString(R.string.error_wallet_is_not_created)
        is TangemSdkError.WalletIsPurged ->
            context.getString(R.string.error_wallet_is_purged)
        is TangemSdkError.SignHashesNotAvailable ->
            context.getString(R.string.error_cannot_be_signed)
        is TangemSdkError.TooManyHashesInOneTransaction ->
            context.getString(R.string.error_cannot_be_signed)
        is TangemSdkError.NotPersonalized ->
            context.getString(R.string.error_not_personalized)
        is TangemSdkError.NotActivated ->
            context.getString(R.string.error_not_activated)
        is TangemSdkError.PasscodeRequired ->
            context.getString(R.string.error_operation)
        is TangemSdkError.VerificationFailed ->
            context.getString(R.string.error_verification_failed)
        is TangemSdkError.DataSizeTooLarge ->
            context.getString(R.string.error_data_size_too_large)
        is TangemSdkError.ExtendedDataSizeTooLarge ->
            context.getString(R.string.error_data_size_too_large_extended)
        is TangemSdkError.MissingCounter ->
            context.getString(R.string.error_missing_counter)
        is TangemSdkError.OverwritingDataIsProhibited ->
            context.getString(R.string.error_data_cannot_be_written)
        is TangemSdkError.DataCannotBeWritten ->
            context.getString(R.string.error_data_cannot_be_written)
        is TangemSdkError.MissingIssuerPubicKey ->
            context.getString(R.string.error_missing_issuer_public_key)
        is TangemSdkError.CardVerificationFailed ->
            context.getString(R.string.error_card_verification_failed)
        is TangemSdkError.UnknownError ->
            context.getString(R.string.error_operation)
        is TangemSdkError.UserCancelled ->
            context.getString(R.string.error_user_cancelled)
        is TangemSdkError.Busy ->
            context.getString(R.string.error_busy)
        is TangemSdkError.MissingPreflightRead ->
            context.getString(R.string.error_operation)
        is TangemSdkError.WrongCardNumber ->
            context.getString(R.string.error_wrong_card_number)
        is TangemSdkError.WrongCardType ->
            context.getString(R.string.error_wrong_card_type)
        is TangemSdkError.CardError ->
            context.getString(R.string.error_card_error)
        is TangemSdkError.InvalidResponse ->
            context.getString(R.string.error_invalid_response)
        is TangemSdkError.NotSupportedFirmwareVersion ->
            context.getString(R.string.error_old_firmware)
        is TangemSdkError.MaxNumberOfWalletsCreated ->
            context.getString(R.string.error_no_space_for_new_wallet)
        is TangemSdkError.CardReadWrongWallet ->
            context.getString(R.string.error_card_read_wrong_wallet)
        is TangemSdkError.UnsupportedCurve ->
            context.getString(R.string.error_wallet_index_exceeds_max_value)
        is TangemSdkError.WalletNotFound ->
            context.getString(R.string.error_wallet_not_found)
        is TangemSdkError.WrongAccessCode ->
            context.getString(R.string.error_wrong_pin1)
        is TangemSdkError.WrongPasscode ->
            context.getString(R.string.error_wrong_pin2)
        is TangemSdkError.CardWithMaxZeroWallets ->
            context.getString(R.string.error_card_with_max_zero_wallets)
        is TangemSdkError.WalletError ->
            context.getString(R.string.error_wallet_error)
        is TangemSdkError.UnsupportedWalletConfig ->
            context.getString(R.string.error_wallet_index_not_correct)
        is TangemSdkError.WalletCannotBeCreated ->
            context.getString(R.string.error_wallet_cannot_be_created)
        is TangemSdkError.BackupCardAlreadyAdded ->
            context.getString(R.string.error_backup_card_already_added)
        is TangemSdkError.BackupCardRequired ->
            context.getString(R.string.error_backup_card_already_added)
        is TangemSdkError.BackupFailedEmptyWallets ->
            context.getString(R.string.error_backup_wrong_card)
        is TangemSdkError.BackupFailedHDWalletSettings ->
            context.getString(R.string.error_backup_wrong_card)
        is TangemSdkError.BackupFailedNotEmptyWallets ->
            context.getString(R.string.error_backup_not_empty_wallets)
        is TangemSdkError.BackupFailedNotEnoughCurves ->
            context.getString(R.string.error_backup_wrong_card)
        is TangemSdkError.BackupFailedNotEnoughWallets ->
            context.getString(R.string.error_backup_wrong_card)
        is TangemSdkError.BackupFailedWrongIssuer ->
            context.getString(R.string.error_backup_wrong_card)
        is TangemSdkError.BackupNotAllowed ->
            context.getString(R.string.error_backup_wrong_card)
        is TangemSdkError.BackupFailedFirmware ->
            context.getString(R.string.error_backup_wrong_card)
        is TangemSdkError.BackupFailedIncompatibleBatch ->
            context.getString(R.string.error_backup_wrong_card)
        is TangemSdkError.ResetPinWrongCard ->
            context.getString(R.string.error_reset_wrong_card)
        is TangemSdkError.WrongInteractionMode ->
            context.getString(R.string.error_wrong_interaction_mode)
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
        is TangemSdkError.BackupFailedCardNotLinked,
        is TangemSdkError.BackupServiceInvalidState,
        is TangemSdkError.AccessCodeOrPasscodeRequired,
        is TangemSdkError.CertificateSignatureRequired,
        is TangemSdkError.EmptyBackupCards,
        is TangemSdkError.MissingPrimaryAttestSignature,
        is TangemSdkError.MissingPrimaryCard,
        is TangemSdkError.NoActiveBackup,
        is TangemSdkError.NoBackupCardForIndex,
        is TangemSdkError.NoBackupDataForCard,
        is TangemSdkError.ResetBackupFailedHasBackupedWallets,
        is TangemSdkError.ResetPinNoCardsToReset,
        is TangemSdkError.TooMuchBackupCards,
        is TangemSdkError.IssuerSignatureLoadingFailed,
        is TangemSdkError.UserForgotTheCode -> context.getString(R.string.error_operation)
    }
}

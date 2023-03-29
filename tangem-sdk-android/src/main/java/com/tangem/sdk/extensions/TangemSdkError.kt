package com.tangem.sdk.extensions

import android.content.Context
import com.tangem.common.core.TangemSdkError
import com.tangem.sdk.R

@Suppress("LongMethod", "ComplexMethod")
fun TangemSdkError.localizedDescription(context: Context): String {
    val resId = when (this) {
        is TangemSdkError.EncodingFailedTypeMismatch, is TangemSdkError.EncodingFailed,
        is TangemSdkError.DecodingFailedMissingTag, is TangemSdkError.DecodingFailedTypeMismatch,
        is TangemSdkError.DecodingFailed, is TangemSdkError.CryptoUtilsError, is TangemSdkError.NetworkError,
        is TangemSdkError.ExceptionError, is TangemSdkError.HDWalletDisabled, is TangemSdkError.FileSettingsUnsupported,
        is TangemSdkError.FilesDisabled, is TangemSdkError.FilesIsEmpty,
        -> null
        is TangemSdkError.TagLost -> R.string.error_tag_lost
        is TangemSdkError.ExtendedLengthNotSupported -> R.string.error_extended_apdu_not_supported
        is TangemSdkError.SerializeCommandError -> R.string.error_operation
        is TangemSdkError.DeserializeApduFailed -> R.string.error_operation
        is TangemSdkError.UnknownStatus -> R.string.error_operation
        is TangemSdkError.ErrorProcessingCommand -> R.string.error_operation
        is TangemSdkError.InvalidState -> R.string.error_operation
        is TangemSdkError.InsNotSupported -> R.string.error_operation
        is TangemSdkError.InvalidParams -> R.string.error_operation
        is TangemSdkError.NeedEncryption -> R.string.error_operation
        is TangemSdkError.FileNotFound -> R.string.error_operation
        is TangemSdkError.AlreadyPersonalized -> R.string.error_already_personalized
        is TangemSdkError.CannotBeDepersonalized -> R.string.error_cannot_be_depersonalized
        is TangemSdkError.AccessCodeRequired -> R.string.error_operation
        is TangemSdkError.AlreadyCreated -> R.string.error_already_created
        is TangemSdkError.PurgeWalletProhibited -> R.string.error_purge_prohibited
        is TangemSdkError.AccessCodeCannotBeChanged -> R.string.error_pin_cannot_be_changed_format
        is TangemSdkError.PasscodeCannotBeChanged -> R.string.error_pin_cannot_be_changed_format
        is TangemSdkError.AccessCodeCannotBeDefault -> R.string.error_pin_cannot_be_default_format
        is TangemSdkError.NoRemainingSignatures -> R.string.error_no_remaining_signatures
        is TangemSdkError.EmptyHashes -> R.string.error_empty_hashes
        is TangemSdkError.HashSizeMustBeEqual -> R.string.error_cannot_be_signed
        is TangemSdkError.WalletIsNotCreated -> R.string.error_wallet_is_not_created
        is TangemSdkError.WalletIsPurged -> R.string.error_wallet_is_purged
        is TangemSdkError.SignHashesNotAvailable -> R.string.error_cannot_be_signed
        is TangemSdkError.TooManyHashesInOneTransaction -> R.string.error_cannot_be_signed
        is TangemSdkError.NotPersonalized -> R.string.error_not_personalized
        is TangemSdkError.NotActivated -> R.string.error_not_activated
        is TangemSdkError.PasscodeRequired -> R.string.error_operation
        is TangemSdkError.VerificationFailed -> R.string.error_verification_failed
        is TangemSdkError.DataSizeTooLarge -> R.string.error_data_size_too_large
        is TangemSdkError.ExtendedDataSizeTooLarge -> R.string.error_data_size_too_large_extended
        is TangemSdkError.MissingCounter -> R.string.error_missing_counter
        is TangemSdkError.OverwritingDataIsProhibited -> R.string.error_data_cannot_be_written
        is TangemSdkError.DataCannotBeWritten -> R.string.error_data_cannot_be_written
        is TangemSdkError.MissingIssuerPubicKey -> R.string.error_missing_issuer_public_key
        is TangemSdkError.CardVerificationFailed -> R.string.error_card_verification_failed
        is TangemSdkError.UnknownError -> R.string.error_operation
        is TangemSdkError.UserCancelled -> R.string.error_user_cancelled
        is TangemSdkError.Busy -> R.string.error_busy
        is TangemSdkError.MissingPreflightRead -> R.string.error_operation
        is TangemSdkError.WrongCardNumber -> R.string.error_wrong_card_number
        is TangemSdkError.WrongCardType -> R.string.error_wrong_card_type
        is TangemSdkError.CardError -> R.string.error_card_error
        is TangemSdkError.InvalidResponse -> R.string.error_invalid_response
        is TangemSdkError.NotSupportedFirmwareVersion -> R.string.error_old_firmware
        is TangemSdkError.MaxNumberOfWalletsCreated -> R.string.error_no_space_for_new_wallet
        is TangemSdkError.CardReadWrongWallet -> R.string.error_card_read_wrong_wallet
        is TangemSdkError.UnsupportedCurve -> R.string.generic_error_code
        is TangemSdkError.WalletNotFound -> R.string.wallet_not_found
        is TangemSdkError.WrongAccessCode -> R.string.error_wrong_pin_format
        is TangemSdkError.WrongPasscode -> R.string.error_wrong_pin_format
        is TangemSdkError.CardWithMaxZeroWallets -> R.string.error_card_with_max_zero_wallets
        is TangemSdkError.WalletError -> R.string.error_wallet_error
        is TangemSdkError.UnsupportedWalletConfig -> R.string.error_wallet_index_not_correct
        is TangemSdkError.WalletCannotBeCreated -> R.string.error_wallet_cannot_be_created
        is TangemSdkError.AccessCodeOrPasscodeRequired -> null
        is TangemSdkError.BackupCardAlreadyAdded -> R.string.error_backup_card_already_added
        is TangemSdkError.BackupCardRequired -> R.string.error_backup_card_already_added
        is TangemSdkError.BackupFailedCardNotLinked -> null
        is TangemSdkError.BackupFailedEmptyWallets -> R.string.error_backup_wrong_card
        is TangemSdkError.BackupFailedHDWalletSettings -> R.string.error_backup_wrong_card
        is TangemSdkError.BackupFailedNotEmptyWallets -> R.string.error_backup_not_empty_wallets
        is TangemSdkError.BackupFailedNotEnoughCurves -> R.string.error_backup_wrong_card
        is TangemSdkError.BackupFailedNotEnoughWallets -> R.string.error_backup_wrong_card
        is TangemSdkError.BackupFailedWrongIssuer -> R.string.error_backup_wrong_card
        is TangemSdkError.BackupNotAllowed -> R.string.error_backup_wrong_card
        is TangemSdkError.BackupServiceInvalidState -> null
        is TangemSdkError.BackupFailedFirmware -> R.string.error_backup_wrong_card
        is TangemSdkError.BackupFailedIncompatibleBatch -> R.string.error_backup_wrong_card
        is TangemSdkError.CertificateSignatureRequired -> null
        is TangemSdkError.EmptyBackupCards -> null
        is TangemSdkError.MissingPrimaryAttestSignature -> null
        is TangemSdkError.MissingPrimaryCard -> null
        is TangemSdkError.NoActiveBackup -> null
        is TangemSdkError.NoBackupCardForIndex -> null
        is TangemSdkError.NoBackupDataForCard -> null
        is TangemSdkError.ResetBackupFailedHasBackupedWallets -> null
        is TangemSdkError.ResetPinNoCardsToReset -> null
        is TangemSdkError.ResetPinWrongCard -> R.string.error_reset_wrong_card
        is TangemSdkError.TooMuchBackupCards -> null
        is TangemSdkError.WrongInteractionMode -> R.string.error_wrong_interaction_mode
        is TangemSdkError.IssuerSignatureLoadingFailed -> null
        is TangemSdkError.UserForgotTheCode -> null
        is TangemSdkError.BiometricsAuthenticationFailed -> null
        is TangemSdkError.BiometricsUnavailable -> null
        is TangemSdkError.BiometricsAuthenticationLockout -> null
        is TangemSdkError.BiometricsAuthenticationPermanentLockout -> null
        is TangemSdkError.UserCanceledBiometricsAuthentication -> null
        is TangemSdkError.EncryptionOperationFailed -> null
        is TangemSdkError.InvalidEncryptionKey -> null
        is TangemSdkError.KeyGenerationException -> null
        is TangemSdkError.MnemonicException -> null
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

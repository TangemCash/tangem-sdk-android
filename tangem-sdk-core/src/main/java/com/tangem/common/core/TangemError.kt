package com.tangem.common.core

import com.tangem.common.UserCodeType
import com.tangem.common.apdu.StatusWord
import com.tangem.common.card.Card
import com.tangem.crypto.bip39.MnemonicErrorResult
import com.tangem.operations.ScanTask
import com.tangem.operations.read.ReadCommand

/**
 * An interface for any error that may occur when performing Tangem SDK tasks.
 */
abstract class TangemError(
    val code: Int,
) : Exception(code.toString()) {
    override val cause: Throwable? = null
    abstract var customMessage: String
    abstract val messageResId: Int?
    open val silent = false
}

/**
 * An error class that represent typical errors that may occur when performing Tangem SDK tasks.
 * Errors are propagated back to the caller in callbacks.
 */
sealed class TangemSdkError(code: Int) : TangemError(code) {

    override var customMessage: String = code.toString()
    override val messageResId: Int? = null

    override fun toString(): String {
        return if (code.toString() == customMessage) {
            "$code: ${this::class.java.simpleName}"
        } else {
            customMessage
        }
    }

    /**
     * This error is returned when Android  NFC reader loses a tag
     * (e.g. a user detaches card from the phone's NFC module) while the NFC session is in progress.
     */
    class TagLost : TangemSdkError(code = 10001)

    /**
     * This error is returned when NFC driver on an Android device does not support sending more than 261 bytes.
     */
    class ExtendedLengthNotSupported : TangemSdkError(code = 10002)

    class SerializeCommandError : TangemSdkError(code = 20001)
    class DeserializeApduFailed : TangemSdkError(code = 20002)
    class EncodingFailedTypeMismatch(override var customMessage: String) : TangemSdkError(code = 20003)
    class EncodingFailed(override var customMessage: String) : TangemSdkError(code = 20004)
    class DecodingFailedMissingTag(override var customMessage: String) : TangemSdkError(code = 20005)
    class DecodingFailedTypeMismatch(override var customMessage: String) : TangemSdkError(code = 20006)
    class DecodingFailed(override var customMessage: String) : TangemSdkError(code = 20007)
    class InvalidResponse : TangemSdkError(code = 20008)

    /**
     * This error is returned when unknown [StatusWord] is received from a card.
     */
    class UnknownStatus(val statusWord: String) : TangemSdkError(code = 30001)

    /**
     * This error is returned when a card's reply is [StatusWord.ErrorProcessingCommand].
     * The card sends this status in case of internal card error.
     */
    class ErrorProcessingCommand : TangemSdkError(code = 30002)

    /**
     * This error is returned when a card's reply is [StatusWord.InvalidState].
     * The card sends this status when command can not be executed in the current state of a card.
     */
    class InvalidState : TangemSdkError(code = 30003)

    /**
     * This error is returned when a card's reply is [StatusWord.InsNotSupported].
     * The card sends this status when the card cannot process the [com.tangem.common.apdu.Instruction].
     */
    class InsNotSupported : TangemSdkError(code = 30004)

    /**
     * This error is returned when a card's reply is [StatusWord.InvalidParams].
     * The card sends this status when there are wrong or not sufficient parameters in TLV request,
     * or wrong PIN1/PIN2.
     * The error may be caused, for example, by wrong parameters of the [Task], [CommandSerializer],
     * mapping or serialization errors.
     */
    class InvalidParams : TangemSdkError(code = 30005)

    /**
     * This error is returned when a card's reply is [StatusWord.NeedEncryption]
     * and the encryption was not established by TangemSdk.
     */
    class NeedEncryption : TangemSdkError(code = 30006)
    class FileNotFound : TangemSdkError(code = 30007)
    class WalletNotFound : TangemSdkError(code = 30008)

    // General Errors
    class NotPersonalized : TangemSdkError(code = 40001)
    class NotActivated : TangemSdkError(code = 40002)
    class WalletIsPurged : TangemSdkError(code = 40003)
    class PasscodeRequired : TangemSdkError(code = 40004)

    /**
     * This error is returned when a [Task] checks unsuccessfully either
     * a card's ability to sign with its private key, or the validity of issuer data.
     */
    class VerificationFailed : TangemSdkError(code = 40005)
    class DataSizeTooLarge : TangemSdkError(code = 40006)

    /**
     * This error is returned when [ReadIssuerDataTask] or [ReadIssuerExtraDataTask] expects a counter
     * (when the card's requires it), but the counter is missing.
     */
    class MissingCounter : TangemSdkError(code = 40007)
    class OverwritingDataIsProhibited : TangemSdkError(code = 40008)
    class DataCannotBeWritten : TangemSdkError(code = 40009)
    class CardVerificationFailed : TangemSdkError(code = 40011)

    /**
     * User entered wrong Access Code
     */
    class WrongAccessCode : TangemSdkError(code = 40012)

    /**
     * User entered wrong Passcode
     */
    class WrongPasscode : TangemSdkError(code = 40013)

    // Personalization Errors
    class AlreadyPersonalized : TangemSdkError(code = 40101)

    // Read Errors
    class AccessCodeRequired : TangemSdkError(code = 40401)
    class NonHardenedDerivationNotSupported : TangemSdkError(code = 40402)
    class WalletCannotBeCreated : TangemSdkError(code = 40403)
    class CardWithMaxZeroWallets : TangemSdkError(code = 40404)
    class WalletAlreadyCreated : TangemSdkError(code = 40405)

    // CreateWallet Errors
    class AlreadyCreated : TangemSdkError(code = 40501)
    class UnsupportedCurve : TangemSdkError(code = 40502)
    class MaxNumberOfWalletsCreated : TangemSdkError(code = 40503)
    class UnsupportedWalletConfig : TangemSdkError(code = 40504)

    // PurgeWallet Errors
    class PurgeWalletProhibited : TangemSdkError(code = 40601)

    // SetPin Errors
    class AccessCodeCannotBeChanged : TangemSdkError(code = 40801)
    class PasscodeCannotBeChanged : TangemSdkError(code = 40802)
    class AccessCodeCannotBeDefault : TangemSdkError(code = 40803)

    // Sign Errors
    class NoRemainingSignatures : TangemSdkError(code = 40901)

    /**
     * This error is returned when a [com.tangem.operations.SignCommand]
     * receives only empty hashes for signature.
     */
    class EmptyHashes : TangemSdkError(code = 40902)

    /**
     * This error is returned when a [com.tangem.operations.SignCommand]
     * receives hashes of different lengths for signature.
     */
    class HashSizeMustBeEqual : TangemSdkError(code = 40903)
    class WalletIsNotCreated : TangemSdkError(code = 40904)
    class SignHashesNotAvailable : TangemSdkError(code = 40905)

    class FileSettingsUnsupported : TangemSdkError(code = 42000)
    class FilesIsEmpty : TangemSdkError(code = 42001)
    class FilesDisabled : TangemSdkError(code = 42002)

    // Backup errors
    class BackupFailedCardNotLinked : TangemSdkError(code = 41201)
    class BackupNotAllowed : TangemSdkError(code = 41202)
    class BackupCardAlreadyAdded : TangemSdkError(code = 41203)
    class MissingPrimaryCard : TangemSdkError(code = 41204)
    class MissingPrimaryAttestSignature : TangemSdkError(code = 41205)
    class TooMuchBackupCards : TangemSdkError(code = 41206)
    class BackupCardRequired : TangemSdkError(code = 41207)
    class NoBackupDataForCard : TangemSdkError(code = 41208)
    class BackupFailedEmptyWallets : TangemSdkError(code = 41209)
    class BackupFailedNotEmptyWallets(val cardId: String) : TangemSdkError(code = 41210)
    class CertificateSignatureRequired : TangemSdkError(code = 41211)
    class AccessCodeOrPasscodeRequired : TangemSdkError(code = 41212)
    class NoActiveBackup : TangemSdkError(code = 41220)
    class ResetBackupFailedHasBackedUpWallets : TangemSdkError(code = 41221)
    class BackupServiceInvalidState : TangemSdkError(code = 4122)
    class NoBackupCardForIndex : TangemSdkError(code = 41223)
    class EmptyBackupCards : TangemSdkError(code = 41224)
    class BackupFailedWrongIssuer : TangemSdkError(code = 41225)
    class BackupFailedHDWalletSettings : TangemSdkError(code = 41226)
    class BackupFailedNotEnoughCurves : TangemSdkError(code = 41227)
    class BackupFailedNotEnoughWallets : TangemSdkError(code = 41228)
    class IssuerSignatureLoadingFailed : TangemSdkError(code = 41229)
    class BackupFailedFirmware : TangemSdkError(code = 41230)
    class BackupFailedIncompatibleBatch : TangemSdkError(code = 41231)
    class BackupFailedIncompatibleFirmware : TangemSdkError(code = 41232)
    class BackupFailedKeysImportSettings : TangemSdkError(code = 41233)
    class BackupFailedAlreadyCreated : TangemSdkError(code = 41234)

    class ResetPinNoCardsToReset : TangemSdkError(code = 41300)

    @Suppress("MagicNumber")
    class ResetPinWrongCard(internalCode: Int? = null) : TangemSdkError(code = internalCode ?: 41301)

    class HDWalletDisabled : TangemSdkError(code = 42003)
    class KeysImportDisabled : TangemSdkError(code = 42004)
    class UserCodeRecoveryDisabled : TangemSdkError(code = 42005)

    // SDK Errors
    class ExceptionError(override val cause: Throwable?) : TangemSdkError(code = 50000) {
        override var customMessage: String = cause?.localizedMessage ?: "empty"
    }

    class UnknownError : TangemSdkError(code = 50001)

    /**
     * This error is returned when a user manually closes NFC Reading Bottom Sheet Dialog.
     */
    class UserCancelled : TangemSdkError(code = 50002) {
        override val silent: Boolean = true
    }

    /**
     * This error is returned when [com.tangem.TangemSdk] was called with a new [Task],
     * while a previous [Task] is still in progress.
     */
    class Busy : TangemSdkError(code = 50003)

    /**
     * This error is returned when a task (such as [ScanTask]) requires that [ReadCommand]
     * is executed before performing other commands.
     */
    class MissingPreflightRead : TangemSdkError(code = 50004)

    /**
     * This error is returned when a [Task] expects a user to use a particular card,
     * but the user tries to use a different card.
     */
    class WrongCardNumber(val cardId: String) : TangemSdkError(code = 50005)

    /**
     * This error is returned when a user scans a card of a [com.tangem.common.extensions.CardType]
     * that is not specified in [Config.cardFilter].
     */
    class WrongCardType(customMessage: String? = null) : TangemSdkError(code = 50006) {
        override var customMessage: String = customMessage ?: "50006"
    }

    /**
     * This error is returned when a [ScanTask] returns a [Card] without some of the essential fields.
     */
    class CardError : TangemSdkError(code = 50007)

    /**
     * This error is returned when the [Command] requires a different firmware version than that of the card.
     */
    class NotSupportedFirmwareVersion : TangemSdkError(code = 50008)

    class CryptoUtilsError(override var customMessage: String) : TangemSdkError(code = 50011)

    class Underlying(override var customMessage: String) : TangemSdkError(code = 50012)

    class UserForgotTheCode : TangemSdkError(code = 50013)

    class NetworkError(override var customMessage: String) : TangemSdkError(code = 50014)

    class AuthenticationUnavailable : TangemSdkError(code = 50015) {
        override var customMessage: String = "Biometrics feature unavailable: $code"
    }

    /**
     * Generic authentication error.
     *
     * @param errorCode Operation error code.
     * For android see [androidx.biometric.BiometricPrompt] errors.
     * @param customMessage Operation error message.
     *
     * @see AuthenticationLockout
     * @see AuthenticationPermanentLockout
     * @see AuthenticationCanceled
     */
    class AuthenticationFailed(
        val errorCode: Int,
        override var customMessage: String,
    ) : TangemSdkError(code = 50016)

    /**
     * The operation was canceled because the API is locked out due to too many attempts. This
     * occurs after 5 failed attempts, and lasts for 30 seconds
     */
    class AuthenticationLockout : TangemSdkError(code = 50017)

    /**
     * The operation was canceled because [AuthenticationLockout] occurred too many times. Authentication is disabled
     * until the user unlocks with their device credential (i.e. PIN,
     * pattern, or password).
     */
    class AuthenticationPermanentLockout : TangemSdkError(code = 50018)

    class AuthenticationCanceled : TangemSdkError(code = 50019) {
        override val silent: Boolean = true
    }

    class AuthenticationAlreadyInProgress : TangemSdkError(code = 50025) {
        override val silent: Boolean = true
    }

    /**
     * The cryptography operation with authentication failed with the key permanently invalidated exception
     * e.g. new biometric enrollment
     *
     * @see CryptographyOperationFailed
     * @see InvalidCryptographyKey
     * */
    class KeystoreInvalidated(override val cause: Throwable?) : TangemSdkError(code = 50024)

    class KeyGenerationException(override var customMessage: String) : TangemSdkError(code = 50022)

    class MnemonicException(val mnemonicResult: MnemonicErrorResult) : TangemSdkError(code = 50023)

    class AuthenticationNotInitialized : TangemSdkError(code = 50026)

    /**
     * Get error according to the pin type
     * @param userCodeType: Specific user code type
     * @param environment: optional environment. If set, a more specific error will be returned based on previous
     * code attempts during the session
     */
    companion object {
        fun from(userCodeType: UserCodeType, environment: SessionEnvironment?): TangemSdkError {
            val isCodeSet = environment?.isUserCodeSet(userCodeType) == true

            return when (userCodeType) {
                UserCodeType.AccessCode -> if (isCodeSet) WrongAccessCode() else AccessCodeRequired()
                UserCodeType.Passcode -> if (isCodeSet) WrongPasscode() else PasscodeRequired()
            }
        }
    }
}

fun Exception.toTangemSdkError(): TangemSdkError {
    if (this is TangemSdkError) return this
    return TangemSdkError.Underlying(this.localizedMessage)
}

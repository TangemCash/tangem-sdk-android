package com.tangem.common.core

import com.tangem.common.UserCodeType
import com.tangem.common.apdu.StatusWord
import com.tangem.common.card.Card
import com.tangem.operations.ScanTask
import com.tangem.operations.read.ReadCommand

/**
 * An interface for any error that may occur when performing Tangem SDK tasks.
 */
interface TangemError {
    val code: Int
    var customMessage: String
    val messageResId: Int?
}

/**
 * An error class that represent typical errors that may occur when performing Tangem SDK tasks.
 * Errors are propagated back to the caller in callbacks.
 */
sealed class TangemSdkError(final override val code: Int) : Exception(code.toString()),
    TangemError {

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
    class TagLost : TangemSdkError(10001)

    /**
     * This error is returned when NFC driver on an Android device does not support sending more than 261 bytes.
     */
    class ExtendedLengthNotSupported : TangemSdkError(10002)


    class SerializeCommandError : TangemSdkError(20001)
    class DeserializeApduFailed : TangemSdkError(20002)
    class EncodingFailedTypeMismatch(override var customMessage: String) : TangemSdkError(20003)
    class EncodingFailed(override var customMessage: String) : TangemSdkError(20004)
    class DecodingFailedMissingTag(override var customMessage: String) : TangemSdkError(20005)
    class DecodingFailedTypeMismatch(override var customMessage: String) : TangemSdkError(20006)
    class DecodingFailed(override var customMessage: String) : TangemSdkError(20007)
    class InvalidResponse : TangemSdkError(20008)

    /**
     * This error is returned when unknown [StatusWord] is received from a card.
     */
    class UnknownStatus(val statusWord: String) : TangemSdkError(30001)

    /**
     * This error is returned when a card's reply is [StatusWord.ErrorProcessingCommand].
     * The card sends this status in case of internal card error.
     */
    class ErrorProcessingCommand : TangemSdkError(30002)

    /**
     * This error is returned when a card's reply is [StatusWord.InvalidState].
     * The card sends this status when command can not be executed in the current state of a card.
     */
    class InvalidState : TangemSdkError(30003)

    /**
     * This error is returned when a card's reply is [StatusWord.InsNotSupported].
     * The card sends this status when the card cannot process the [com.tangem.common.apdu.Instruction].
     */
    class InsNotSupported : TangemSdkError(30004)

    /**
     * This error is returned when a card's reply is [StatusWord.InvalidParams].
     * The card sends this status when there are wrong or not sufficient parameters in TLV request,
     * or wrong PIN1/PIN2.
     * The error may be caused, for example, by wrong parameters of the [Task], [CommandSerializer],
     * mapping or serialization errors.
     */
    class InvalidParams : TangemSdkError(30005)

    /**
     * This error is returned when a card's reply is [StatusWord.NeedEncryption]
     * and the encryption was not established by TangemSdk.
     */
    class NeedEncryption : TangemSdkError(30006)
    class FileNotFound : TangemSdkError(30007)
    class WalletNotFound : TangemSdkError(30008)

    //Personalization Errors
    class AlreadyPersonalized : TangemSdkError(40101)

    //Depersonalization Errors
    class CannotBeDepersonalized : TangemSdkError(40201)

    //Read Errors
    class AccessCodeRequired : TangemSdkError(40401)
    class CardReadWrongWallet : TangemSdkError(40402)
    class WalletCannotBeCreated : TangemSdkError(40403)
    class CardWithMaxZeroWallets : TangemSdkError(40404)

    //CreateWallet Errors
    class AlreadyCreated : TangemSdkError(40501)
    class UnsupportedCurve : TangemSdkError(40502)
    class MaxNumberOfWalletsCreated : TangemSdkError(40503)
    class UnsupportedWalletConfig : TangemSdkError(40504)

    //PurgeWallet Errors
    class PurgeWalletProhibited : TangemSdkError(40601)

    //SetPin Errors
    class AccessCodeCannotBeChanged : TangemSdkError(40801)
    class PasscodeCannotBeChanged : TangemSdkError(40802)
    class AccessCodeCannotBeDefault : TangemSdkError(40803)

    //Sign Errors
    class NoRemainingSignatures : TangemSdkError(40901)

    /**
     * This error is returned when a [com.tangem.operations.SignCommand]
     * receives only empty hashes for signature.
     */
    class EmptyHashes : TangemSdkError(40902)

    /**
     * This error is returned when a [com.tangem.operations.SignCommand]
     * receives hashes of different lengths for signature.
     */
    class HashSizeMustBeEqual : TangemSdkError(40903)
    class WalletIsNotCreated : TangemSdkError(40904)
    class SignHashesNotAvailable : TangemSdkError(40905)

    /**
     * Tangem cards can sign currently up to 10 hashes during one [com.tangem.operations.SignCommand].
     * This error is returned when a [com.tangem.operations.SignCommand] receives more than 10 hashes to sign.
     */
    class TooManyHashesInOneTransaction : TangemSdkError(40906)

    //Write Extra Issuer Data Errors
    class ExtendedDataSizeTooLarge : TangemSdkError(41101)

    // Backup errors
    class BackupFailedCardNotLinked : TangemSdkError(41201)
    class BackupNotAllowed : TangemSdkError(41202)
    class BackupCardAlreadyAdded : TangemSdkError(41203)
    class MissingPrimaryCard : TangemSdkError(41204)
    class MissingPrimaryAttestSignature : TangemSdkError(41205)
    class TooMuchBackupCards : TangemSdkError(41206)
    class BackupCardRequired : TangemSdkError(41207)
    class NoBackupDataForCard : TangemSdkError(41208)
    class BackupFailedEmptyWallets : TangemSdkError(41209)
    class BackupFailedNotEmptyWallets : TangemSdkError(4121)
    class CertificateSignatureRequired : TangemSdkError(41211)
    class AccessCodeOrPasscodeRequired : TangemSdkError(41212)
    class NoActiveBackup : TangemSdkError(41220)
    class ResetBackupFailedHasBackupedWallets : TangemSdkError(41221)
    class BackupServiceInvalidState : TangemSdkError(4122)
    class NoBackupCardForIndex : TangemSdkError(41223)
    class EmptyBackupCards : TangemSdkError(41224)
    class BackupFailedWrongIssuer : TangemSdkError(41225)
    class BackupFailedHDWalletSettings : TangemSdkError(41226)
    class BackupFailedNotEnoughCurves : TangemSdkError(41227)
    class BackupFailedNotEnoughWallets : TangemSdkError(41228)

    class ResetPinNoCardsToReset : TangemSdkError(41300)
    class ResetPinWrongCard : TangemSdkError(41301)

    //General Errors
    class NotPersonalized : TangemSdkError(40001)
    class NotActivated : TangemSdkError(40002)
    class WalletIsPurged : TangemSdkError(40003)
    class PasscodeRequired : TangemSdkError(40004)

    /**
     * This error is returned when a [Task] checks unsuccessfully either
     * a card's ability to sign with its private key, or the validity of issuer data.
     */
    class VerificationFailed : TangemSdkError(40005)
    class DataSizeTooLarge : TangemSdkError(40006)

    /**
     * This error is returned when [ReadIssuerDataTask] or [ReadIssuerExtraDataTask] expects a counter
     * (when the card's requires it), but the counter is missing.
     */
    class MissingCounter : TangemSdkError(40007)
    class OverwritingDataIsProhibited : TangemSdkError(40008)
    class DataCannotBeWritten : TangemSdkError(40009)
    class MissingIssuerPubicKey : TangemSdkError(40010)
    class CardVerificationFailed : TangemSdkError(40011)

    /**
     * User entered wrong Access Code
     */
    class WrongAccessCode : TangemSdkError(40012)

    /**
     * User entered wrong Passcode
     */
    class WrongPasscode : TangemSdkError(40013)


    //SDK Errors
    class ExceptionError(val throwable: Throwable?) : TangemSdkError(50000) {
        override var customMessage: String = throwable?.localizedMessage ?: "empty"
    }

    class UnknownError : TangemSdkError(50001)

    /**
     * This error is returned when a user manually closes NFC Reading Bottom Sheet Dialog.
     */
    class UserCancelled : TangemSdkError(50002)

    /**
     * This error is returned when [com.tangem.TangemSdk] was called with a new [Task],
     * while a previous [Task] is still in progress.
     */
    class Busy : TangemSdkError(50003)

    /**
     * This error is returned when a task (such as [ScanTask]) requires that [ReadCommand]
     * is executed before performing other commands.
     */
    class MissingPreflightRead : TangemSdkError(50004)

    /**
     * This error is returned when a [Task] expects a user to use a particular card,
     * but the user tries to use a different card.
     */
    class WrongCardNumber : TangemSdkError(50005)

    /**
     * This error is returned when a user scans a card of a [com.tangem.common.extensions.CardType]
     * that is not specified in [Config.cardFilter].
     */
    class WrongCardType : TangemSdkError(50006)

    /**
     * This error is returned when a [ScanTask] returns a [Card] without some of the essential fields.
     */
    class CardError : TangemSdkError(50007)

    /**
     * This error is returned when the [Command] requires a different firmware version than that of the card.
     */
    class NotSupportedFirmwareVersion : TangemSdkError(50008)

    /**
     * This error is returned when the scanned wallet doesn't have some essential fields.
     */
    class WalletError : TangemSdkError(50009)

//    class FailedToGenerateRandomSequence : TangemSdkError(50010)

    class CryptoUtilsError(override var customMessage: String) : TangemSdkError(50011)

//    class Underlying : TangemSdkError(50012)

    class NetworkError(override var customMessage: String) : TangemSdkError(50013)

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

package com.tangem

import com.tangem.commands.*
import com.tangem.commands.common.card.Card
import com.tangem.commands.common.jsonRpc.*
import com.tangem.commands.file.*
import com.tangem.commands.personalization.DepersonalizeCommand
import com.tangem.commands.personalization.DepersonalizeResponse
import com.tangem.commands.personalization.PersonalizeCommand
import com.tangem.commands.personalization.entities.Acquirer
import com.tangem.commands.personalization.entities.CardConfig
import com.tangem.commands.personalization.entities.Issuer
import com.tangem.commands.personalization.entities.Manufacturer
import com.tangem.commands.verification.VerifyCardCommand
import com.tangem.commands.verification.VerifyCardResponse
import com.tangem.commands.wallet.*
import com.tangem.common.CardValuesStorage
import com.tangem.common.CompletionResult
import com.tangem.common.TerminalKeysService
import com.tangem.common.files.FileHashData
import com.tangem.common.files.FileHashHelper
import com.tangem.crypto.CryptoUtils
import com.tangem.tasks.CreateWalletTask
import com.tangem.tasks.ScanTask
import com.tangem.tasks.file.*
import kotlin.reflect.KClass

/**
 * The main interface of Tangem SDK that allows your app to communicate with Tangem cards.
 *
 * @property reader is an interface that is responsible for NFC connection and
 * transfer of data to and from the Tangem Card.
 * Its default implementation, NfcCardReader, is in our tangem-sdk module.
 * @property viewDelegate An interface that allows interaction with users and shows relevant UI.
 * Its default implementation, DefaultCardSessionViewDelegate, is in our tangem-sdk module.
 * @property config allows to change a number of parameters for communication with Tangem cards.
 * Do not change the default values unless you know what you are doing.
 * @param  terminalKeysService allows to retrieve saved terminal keys.
 */
class TangemSdk(
    private val reader: CardReader,
    private val viewDelegate: SessionViewDelegate,
    var config: Config = Config(),
    cardValuesStorage: CardValuesStorage,
    terminalKeysService: TerminalKeysService? = null
) {

    private var cardSession: CardSession? = null
    private val environmentService = SessionEnvironmentService(config, terminalKeysService, cardValuesStorage)

    private val jsonRpcConverter: JSONRPCConverter by lazy { JSONRPCConverter.shared() }

    init {
        CryptoUtils.initCrypto()
    }

    /**
     * This method launches a [ScanTask] on a new thread.
     *
     * To start using any card, you first need to read it using the scanCard() method.
     * This method launches an NFC session, and once it’s connected with the card,
     * it obtains the card data. Optionally, if the card contains a wallet (private and public key pair),
     * it proves that the wallet owns a private key that corresponds to a public one.
     *
     * Note: `WalletIndex` available for cards with COS v.4.0 or higher.
     * @param walletIndex: Pointer to wallet which data should be read.
     * if not specified - wallet at default index will be read. See `WalletIndex` for more info.
     * @param initialMessage: A custom description that shows at the beginning of the NFC session.
     * If null, default message will be used.
     * * @param callback is triggered on the completion of the [ScanTask] and provides card response
     * in the form of [Card] if the task was performed successfully or [TangemSdkError] in case of an error.
     */
    fun scanCard(
        initialMessage: Message? = null,
        callback: (result: CompletionResult<Card>) -> Unit
    ) {
        startSessionWithRunnable(ScanTask(), null, initialMessage, callback)
    }

    /**
     * This method allows you to sign one hash and will return a corresponding signature.
     * Please note that Tangem cards usually protect the signing with a security delay
     * that may last up to 45 seconds, depending on a card.
     * It is for `SessionViewDelegate` to notify users of security delay.
     *
     * @param hash: Transaction hash for sign by card.
     * @param walletPublicKey: Public key of wallet that should sign hash.
     * @param cardId: CID, Unique Tangem card ID number
     * @param initialMessage: A custom description that shows at the beginning of the NFC session.
     * default message will be used
     * @param callback: is triggered on the completion of the [SignCommand] and provides response
     * in the form of single signed hash [ByteArray] if the task was performed successfully
     * or [TangemSdkError] in case of an error.
     */
    fun sign(
        hash: ByteArray,
        walletPublicKey: ByteArray,
        cardId: String? = null,
        initialMessage: Message? = null,
        callback: (result: CompletionResult<ByteArray>) -> Unit
    ) {
        sign(arrayOf(hash), walletPublicKey, cardId, initialMessage) { result ->
            when (result) {
                is CompletionResult.Success -> callback(CompletionResult.Success(result.data[0]))
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }

    /**
     * This method launches a [SignCommand] on a new thread.
     *
     * It allows you to sign one or multiple hashes.
     * Simultaneous signing of array of hashes in a single [SignCommand] is required to support
     * Bitcoin-type multi-input blockchains (UTXO).
     * The [SignCommand] will return a corresponding array of signatures.
     *
     * Please note that Tangem cards usually protect the signing with a security delay
     * that may last up to 45 seconds, depending on a card.
     * It is for [SessionViewDelegate] to notify users of security delay.
     *
     * Note: `WalletIndex` available for cards with COS v.4.0 or higher.
     * @param hashes: Array of transaction hashes. It can be from one or up to ten hashes of the same length.
     * @param walletPublicKey: Public key of the wallet that should sign hashes.
     * If not specified - wallet at default index will sign hashes. See `WalletIndex` for more info.
     * @param cardId: CID, Unique Tangem card ID number
     * @param initialMessage: A custom description that shows at the beginning of the NFC session.
     * If null, default message will be used.
     * @param callback: is triggered on the completion of the [SignCommand] and provides response
     * in the form of list of signed hashes [List<ByteArray>] if the task was performed successfully
     * or [TangemSdkError] in case of an error.
     */
    fun sign(
        hashes: Array<ByteArray>,
        walletPublicKey: ByteArray,
        cardId: String? = null,
        initialMessage: Message? = null,
        callback: (result: CompletionResult<List<ByteArray>>) -> Unit
    ) {
        val walletIndex = WalletIndex.PublicKey(walletPublicKey)
        startSessionWithRunnable(SignCommand(hashes, walletIndex), cardId, initialMessage) { result ->
            when (result) {
                is CompletionResult.Success -> callback(CompletionResult.Success(result.data.signatures))
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }

    /**
     * This method launches a [ReadIssuerDataCommand] on a new thread.
     *
     * This command returns 512-byte Issuer Data field and its issuer’s signature.
     * Issuer Data is never changed or parsed from within the Tangem COS. The issuer defines purpose of use,
     * format and payload of Issuer Data. For example, this field may contain information about
     * wallet balance signed by the issuer or additional issuer’s attestation data.
     *
     * @param cardId: CID, Unique Tangem card ID number.
     * @param initialMessage: A custom description that shows at the beginning of the NFC session.
     * If null, default message will be used.
     * @param callback: is triggered on the completion of the [ReadIssuerDataCommand] and provides
     * card response in the form of [ReadIssuerDataResponse] if the task was performed successfully
     * or [TangemSdkError] in case of an error.
     */
    fun readIssuerData(
        cardId: String? = null,
        initialMessage: Message? = null,
        callback: (result: CompletionResult<ReadIssuerDataResponse>) -> Unit
    ) {
        startSessionWithRunnable(ReadIssuerDataCommand(config.issuerPublicKey), cardId, initialMessage, callback)
    }

    /**
     * This method launches a [WriteIssuerDataCommand] on a new thread.
     *
     * This command writes 512-byte Issuer Data field and its issuer’s signature.
     * Issuer Data is never changed or parsed from within the Tangem COS. The issuer defines purpose of use,
     * format and payload of Issuer Data. For example, this field may contain information about
     * wallet balance signed by the issuer or additional issuer’s attestation data.
     *
     * @param cardId: CID, Unique Tangem card ID number.
     * @param issuerData: Data provided by issuer.
     * @param issuerDataSignature: Issuer’s signature of [issuerData] with Issuer Data Private Key.
     * @param issuerDataCounter: An optional counter that protect issuer data against replay attack.
     * @param initialMessage: A custom description that shows at the beginning of the NFC session.
     * If null, default message will be used.
     * @param callback: is triggered on the completion of the [WriteIssuerDataCommand] and provides
     * card response in the form of [WriteIssuerDataResponse] if the task was performed successfully
     * or [TangemSdkError] in case of an error.
     */
    fun writeIssuerData(
        cardId: String? = null,
        issuerData: ByteArray,
        issuerDataSignature: ByteArray,
        issuerDataCounter: Int? = null,
        initialMessage: Message? = null,
        callback: (result: CompletionResult<WriteIssuerDataResponse>) -> Unit
    ) {
        val command = WriteIssuerDataCommand(
            issuerData,
            issuerDataSignature,
            issuerDataCounter,
            config.issuerPublicKey
        )
        startSessionWithRunnable(command, cardId, initialMessage, callback)
    }

    /**
     * This method launches a [ReadIssuerExtraDataCommand] on a new thread.
     *
     * This command retrieves Issuer Extra Data field and its issuer’s signature.
     * Issuer Extra Data is never changed or parsed from within the Tangem COS. The issuer defines purpose of use,
     * format and payload of Issuer Data. . For example, this field may contain photo or
     * biometric information for ID card product. Because of the large size of Issuer_Extra_Data,
     * a series of these commands have to be executed to read the entire Issuer_Extra_Data.
     *
     * @param cardId: CID, Unique Tangem card ID number.
     * @param initialMessage: A custom description that shows at the beginning of the NFC session.
     * If null, default message will be used.
     * @param callback: is triggered on the completion of the [ReadIssuerExtraDataCommand] and provides
     * card response in the form of [ReadIssuerExtraDataResponse] if the task was performed successfully
     * or [TangemSdkError] in case of an error.
     */
    @Deprecated("Not supported in cards with firmware version 3.29 and above, use readFiles instead",
        replaceWith = ReplaceWith("this.readFiles"),
        level = DeprecationLevel.WARNING)
    fun readIssuerExtraData(
        cardId: String? = null,
        initialMessage: Message? = null,
        callback: (result: CompletionResult<ReadIssuerExtraDataResponse>) -> Unit
    ) {
        startSessionWithRunnable(ReadIssuerExtraDataCommand(config.issuerPublicKey), cardId, initialMessage, callback)
    }

    /**
     * This method launches a [WriteIssuerExtraDataCommand] on a new thread.
     *
     * This command writes Issuer Extra Data field and its issuer’s signature.
     * Issuer Extra Data is never changed or parsed from within the Tangem COS.
     * The issuer defines purpose of use, format and payload of Issuer Data.
     * For example, this field may contain a photo or biometric information for ID card products.
     * Because of the large size of IssuerExtraData, a series of these commands have to be executed
     * to write entire IssuerExtraData.
     *
     * @param cardId: CID, Unique Tangem card ID number.
     * @param issuerData: Data provided by issuer.
     * @param startingSignature: Issuer’s signature with Issuer Data Private Key of [cardId],
     * [issuerDataCounter] (if flags Protect_Issuer_Data_Against_Replay and
     * Restrict_Overwrite_Issuer_Extra_Data are set in [SettingsMask]) and size of [issuerData].
     * @param finalizingSignature: Issuer’s signature with Issuer Data Private Key of [cardId],
     * [issuerData] and [issuerDataCounter] (the latter one only if flags Protect_Issuer_Data_Against_Replay
     * andRestrict_Overwrite_Issuer_Extra_Data are set in [SettingsMask]).
     * @param issuerDataCounter: An optional counter that protect issuer data against replay attack.
     * @param initialMessage: A custom description that shows at the beginning of the NFC session.
     * If null, default message will be used.
     * @param callback: is triggered on the completion of the [WriteIssuerExtraDataCommand] and provides
     * card response in the form of [WriteIssuerDataResponse] if the task was performed successfully
     * or [TangemSdkError] in case of an error.
     */
    @Deprecated("Not supported in cards with firmware version 3.29 and above, use writeFilesData instead",
        ReplaceWith("this.writeFiles"), DeprecationLevel.WARNING)
    fun writeIssuerExtraData(
        cardId: String? = null,
        issuerData: ByteArray,
        startingSignature: ByteArray,
        finalizingSignature: ByteArray,
        issuerDataCounter: Int? = null,
        initialMessage: Message? = null,
        callback: (result: CompletionResult<WriteIssuerDataResponse>) -> Unit
    ) {
        val command = WriteIssuerExtraDataCommand(
            issuerData,
            startingSignature, finalizingSignature,
            issuerDataCounter,
            config.issuerPublicKey
        )
        startSessionWithRunnable(command, cardId, initialMessage, callback)
    }

    /**
     * This method launches a [WriteFilesTask] on a new thread.
     *
     * This task allows to write multiple files to a card. Files can be signed by Issuer
     * (specified on card during personalization) - [DataToWrite.DataProtectedBySignature] or
     * files can be written using PIN2 (Passcode) - [DataToWrite.DataProtectedByPasscode].
     *
     * @param files: files to be written.
     * @param cardId: CID, Unique Tangem card ID number.
     * @param initialMessage: A custom description that shows at the beginning of the NFC session.
     * If null, default message will be used.
     * @param callback: is triggered on the completion of the [WriteFilesTask] and provides
     * card response in the form of [WriteFileResponse] if the task was performed successfully
     * or [TangemSdkError] in case of an error.
     */
    fun writeFiles(
        files: List<DataToWrite>,
        cardId: String? = null,
        initialMessage: Message? = null,
        callback: (result: CompletionResult<WriteFilesResponse>) -> Unit
    ) {
        startSessionWithRunnable(WriteFilesTask(files), cardId, initialMessage, callback)
    }

    /**
     * This method launches a [ReadFilesTask] on a new thread.
     *
     * This task allows to read multiple files from a card. If the files are private,
     * then Passcode (PIN2) is required to read the files.
     *
     * @param readPrivateFiles: if set to true, then the task will read private files,
     * for which it requires PIN2. Otherwise only public files can be read.
     * @param indices: indices of files to be read. If not provided, the task will read and return
     * all files from a card that satisfy the access level condition (either only public or private and public).
     * @param cardId: CID, Unique Tangem card ID number.
     * @param initialMessage: A custom description that shows at the beginning of the NFC session.
     * If null, default message will be used.
     * @param callback: is triggered on the completion of the [ReadFilesTask] and provides
     * card response in the form of [ReadFilesResponse] if the task was performed successfully
     * or [TangemSdkError] in case of an error.
     */
    fun readFiles(
        readPrivateFiles: Boolean = false,
        indices: List<Int>? = null,
        cardId: String? = null,
        initialMessage: Message? = null,
        callback: (result: CompletionResult<ReadFilesResponse>) -> Unit
    ) {
        val task = ReadFilesTask(readPrivateFiles, indices)
        startSessionWithRunnable(task, cardId, initialMessage, callback)
    }

    /**
     * This method launches a [ChangeFilesSettingsTask] on a new thread.
     *
     * This task allows to change settings of multiple files written to the card with [WriteFileCommand].
     * Passcode (PIN2) is required for this operation.
     * [FileSettings] change access level to a file - it can be [FileSettings.Private],
     * accessible only with PIN2, or [FileSettings.Public], accessible without PIN2
     *
     * @param changes: contains list of [FileSettingsChange] -
     * indices of files that are to be changed and desired settings.
     * @param cardId: CID, Unique Tangem card ID number.
     * @param initialMessage: A custom description that shows at the beginning of the NFC session.
     * If null, default message will be used.
     * @param callback: is triggered on the completion of the [ChangeFilesSettingsTask] and provides
     * card response in the form of [ChangeFileSettingsResponse] if the task was performed successfully
     * or [TangemSdkError] in case of an error.
     */
    fun changeFilesSettings(
        changes: List<FileSettingsChange>,
        cardId: String? = null,
        initialMessage: Message? = null,
        callback: (result: CompletionResult<ChangeFileSettingsResponse>) -> Unit
    ) {
        val task = ChangeFilesSettingsTask(changes)
        startSessionWithRunnable(task, cardId, initialMessage, callback)
    }

    /**
     * This method launches a [DeleteFilesTask] on a new thread.
     *
     * This task allows to delete multiple or all files written to the card with [WriteFileCommand].
     * Passcode (PIN2) is required to delete the files.
     *
     * @param indices: indices of files to be deleted. If [indices] are not provided,
     * then all files will be deleted.
     * @param cardId: CID, Unique Tangem card ID number.
     * @param initialMessage: A custom description that shows at the beginning of the NFC session.
     * If null, default message will be used.
     * @param callback: is triggered on the completion of the [DeleteFilesTask] and provides
     * card response in the form of [DeleteFileResponse] if the task was performed successfully
     * or [TangemSdkError] in case of an error.
     */
    fun deleteFiles(
        indices: List<Int>? = null,
        cardId: String? = null,
        initialMessage: Message? = null,
        callback: (result: CompletionResult<DeleteFileResponse>) -> Unit
    ) {
        startSessionWithRunnable(DeleteFilesTask(indices), cardId, initialMessage, callback)
    }

    /**
     * Creates hashes and signatures for [com.tangem.commands.file.DataToWrite.DataProtectedBySignature]
     * @param cardId: CID, Unique Tangem card ID number.
     * @param fileData: File data that will be written on card
     * @param fileCounter: A counter that protects issuer data against replay attack.
     * @param privateKey: Optional private key that will be used for signing files hashes.
     * If it is provided, then [FileHashData] will contain signed file signatures.
     * @return [FileHashData] with hashes to sign and signatures if [privateKey] was provided.
     */
    fun prepareHashes(
        cardId: String,
        fileData: ByteArray,
        fileCounter: Int,
        privateKey: ByteArray? = null
    ): FileHashData {
        return FileHashHelper.prepareHashes(cardId, fileData, fileCounter, privateKey)
    }

    /**
     * This method launches a [ReadUserDataCommand] on a new thread.
     *
     * This command returns two up to 512-byte User_Data, User_Protected_Data and two counters User_Counter and
     * User_Protected_Counter fields.
     * User_Data and User_ProtectedData are never changed or parsed by the executable code the Tangem COS.
     * The App defines purpose of use, format and it's payload. For example, this field may contain cashed information
     * from blockchain to accelerate preparing new transaction.
     * User_Counter and User_ProtectedCounter are counters, that initial values can be set by App and increased on every signing
     * of new transaction (on SIGN command that calculate new signatures). The App defines purpose of use.
     * For example, this fields may contain blockchain nonce value.
     *
     * @param cardId: CID, Unique Tangem card ID number.
     * @param initialMessage: A custom description that shows at the beginning of the NFC session.
     * If null, default message will be used.
     * @param callback: is triggered on the completion of the [ReadUserDataCommand] and provides
     * card response in the form of [ReadUserDataResponse] if the task was performed successfully
     * or [TangemSdkError] in case of an error.
     */
    fun readUserData(
        cardId: String? = null,
        initialMessage: Message? = null,
        callback: (result: CompletionResult<ReadUserDataResponse>) -> Unit
    ) {
        startSessionWithRunnable(ReadUserDataCommand(), cardId, initialMessage, callback)
    }

    /**
     * This method launches a [WriteUserDataCommand] on a new thread, writing  UserData and UserCounter fields.
     *
     * User_Data is never changed or parsed by the executable code the Tangem COS.
     * The App defines purpose of use, format and its payload. For example, this field may contain cashed information
     * from blockchain to accelerate preparing new transaction.
     * The initial value of User_Counter can be set by an App and increased on every signing
     * of new transaction (on SIGN command that calculate new signatures). The App defines purpose of use.
     * For example, this fields may contain blockchain nonce value.
     *
     * Writing of UserCounter and UserData is protected only by PIN1.
     *
     * @param cardId: CID, Unique Tangem card ID number.
     * @param userData: A data for which an SDK's user can define its purpose of use,
     * format and it's payload. For example, this field may contain cashed information from blockchain
     * to accelerate preparing new transaction.
     * @param userCounter: A counter that initial value can be set by an SDK's user and
     * increased on every signing of new transaction (on [SignCommand] that calculate new signatures).
     * An SDK's user defines purpose of its use. If null, the current counter value will not be overwritten.
     * @param initialMessage: A custom description that shows at the beginning of the NFC session.
     * If null, default message will be used.
     * @param callback: is triggered on the completion of the [WriteUserDataCommand] and provides
     * card response in the form of [WriteUserDataResponse] if the task was performed successfully
     * or [TangemSdkError] in case of an error.
     */
    fun writeUserData(
        cardId: String? = null,
        userData: ByteArray,
        userCounter: Int? = null,
        initialMessage: Message? = null,
        callback: (result: CompletionResult<WriteUserDataResponse>) -> Unit
    ) {
        val command = WriteUserDataCommand(userData = userData, userCounter = userCounter)
        startSessionWithRunnable(command, cardId, initialMessage, callback)
    }

    /**
     * This method launches a [WriteUserDataCommand] on a new thread,
     * writing UserProtectedData and UserProtectedCounter fields.
     *
     * User_ProtectedData is never changed or parsed by the executable code the Tangem COS.
     * The App defines purpose of use, format and its payload. For example, this field may contain cashed information
     * from blockchain to accelerate preparing new transaction.
     * The initial value of User_ProtectedCounter can be set by an App and increased on every signing
     * of a new transaction (on SIGN command that calculate new signatures). The App defines the purpose of use.
     * For example, this fields may contain blockchain nonce value.
     *
     * UserProtectedCounter and UserProtectedData require PIN2 for confirmation.
     *
     * @param cardId: CID, Unique Tangem card ID number.
     * @param userProtectedData: A data for which an SDK's user can define its purpose of use,
     * format and it's payload. For example, this field may contain cashed information from blockchain
     * to accelerate preparing new transaction.
     * @param userProtectedCounter: A counter that initial value can be set by an SDK's user and
     * increased on every signing of new transaction (on [SignCommand] that calculate new signatures).
     * An SDK's user defines purpose of its use. If null, the current counter value will not be overwritten.
     * For example, this fields may contain blockchain nonce value.
     * @param initialMessage: A custom description that shows at the beginning of the NFC session.
     * If null, default message will be used.
     * @param callback: is triggered on the completion of the [WriteUserDataCommand] and provides
     * card response in the form of [WriteUserDataResponse] if the task was performed successfully
     * or [TangemSdkError] in case of an error.
     */
    fun writeUserProtectedData(
        cardId: String? = null,
        userProtectedData: ByteArray,
        userProtectedCounter: Int? = null,
        initialMessage: Message? = null,
        callback: (result: CompletionResult<WriteUserDataResponse>) -> Unit
    ) {
        val command = WriteUserDataCommand(
            userProtectedData = userProtectedData, userProtectedCounter = userProtectedCounter
        )
        startSessionWithRunnable(command, cardId, initialMessage, callback)
    }

    /**
     * This method launches a [CreateWalletTask] on a new thread.
     *
     * This this will create a new wallet on the card having ‘Empty’ state with [CreateWalletCommand]
     * and will check the success of the operation by performing [CheckWalletCommand].
     * A key pair WalletPublicKey / WalletPrivateKey is generated and securely stored in the card.
     * App will need to obtain Wallet_PublicKey from the [CreateWalletResponse] or from the
     * response of [ReadCommand] and then transform it into an address of corresponding
     * blockchain wallet according to a specific blockchain algorithm.
     * WalletPrivateKey is never revealed by the card and will be used by [SignCommand] and [CheckWalletCommand].
     * RemainingSignature is set to MaxSignatures.
     *
     * Note: 'WalletConfig' and 'WalletIndex' available for cards with COS v.4.0 and higher. For earlier
     * versions it will be ignored.
     * @param config: Configuration for wallet that should be created (blockchain name, token...).
     * This parameter available for cards with COS v.4.0 and higher. For earlier versions it will be ignored
     * @param cardId CID, Unique Tangem card ID number.
     * @param initialMessage: A custom description that shows at the beginning of the NFC session.
     * If null, default message will be used.
     * @param callback: is triggered on the completion of the [CreateWalletTask] and provides
     * card response in the form of [CreateWalletResponse] if the task was performed successfully
     * or [TangemSdkError] in case of an error.
     */
    fun createWallet(
        config: WalletConfig? = null,
        cardId: String? = null,
        initialMessage: Message? = null,
        callback: (result: CompletionResult<CreateWalletResponse>) -> Unit
    ) {
        startSessionWithRunnable(CreateWalletTask(config), cardId, initialMessage, callback)
    }

    /**
     * This method launches a [PurgeWalletCommand] on a new thread.
     *
     * This command deletes all wallet data. If IsReusable flag is enabled during personalization,
     * the card changes state to ‘Empty’ and a new wallet can be created by [CreateWalletTask]
     * or [CreateWalletCommand].
     * If IsReusable flag is disabled, the card switches to ‘Purged’ state.
     * ‘Purged’ state is final, it makes the card useless.
     *
     * Note: 'WalletIndex' available for cards with COS v.4.0 or higher
     * @param walletIndex: Pointer to wallet that should be purged.
     * If not specified - wallet at default index will be purged. See `WalletIndex` for more info
     * @param cardId: CID, Unique Tangem card ID number.
     * @param initialMessage: A custom description that shows at the beginning of the NFC session.
     * If null, default message will be used.
     * @param callback: is triggered on the completion of the [PurgeWalletCommand] and provides
     * card response in the form of [PurgeWalletResponse] if the task was performed successfully
     * or [TangemSdkError] in case of an error.
     */
    fun purgeWallet(
        walletIndex: WalletIndex,
        cardId: String? = null,
        initialMessage: Message? = null,
        callback: (result: CompletionResult<PurgeWalletResponse>) -> Unit
    ) {
        startSessionWithRunnable(PurgeWalletCommand(walletIndex), cardId, initialMessage, callback)
    }

    /**
     * This method launches a [VerifyCardCommand] on a new thread.
     *
     * The command to ensures the card has not been counterfeited.
     * By using standard challenge-response scheme, the card proves possession of CardPrivateKey
     * that corresponds to CardPublicKey returned by [ReadCommand]. Then the data is sent
     * to Tangem server to prove that  this card was indeed issued by Tangem.
     * The online part of the verification is unavailable for DevKit cards.
     *
     *
     * @param cardId: CID, Unique Tangem card ID number.
     * @param online: flag that allows disable online verification
     * @param initialMessage: A custom description that shows at the beginning of the NFC session.
     * If null, default message will be used.
     * @param callback: is triggered on the completion of the [VerifyCardCommand] and provides
     * card response in the form of [VerifyCardResponse] if the task was performed successfully
     * or [TangemSdkError] in case of an error.
     */
    fun verify(
        online: Boolean = true,
        cardId: String? = null,
        initialMessage: Message? = null,
        callback: (result: CompletionResult<VerifyCardResponse>) -> Unit
    ) {
        startSessionWithRunnable(VerifyCardCommand(online), cardId, initialMessage, callback)
    }

    /**
     * Command available on SDK cards only
     *
     * This method launches a [PersonalizeCommand] on a new thread.
     *
     * Personalization is an initialization procedure, required before starting using a card.
     * During this procedure a card setting is set up.
     * During this procedure all data exchange is encrypted.
     * @param config: is a configuration file with all the card settings that are written on the card
     * during personalization.
     * @param issuer: Issuer is a third-party team or company wishing to use Tangem cards.
     * @param manufacturer: Tangem Card Manufacturer.
     * @param acquirer: Acquirer is a trusted third-party company that operates proprietary
     * (non-EMV) POS terminal infrastructure and transaction processing back-end.
     * @param initialMessage: A custom description that shows at the beginning of the NFC session.
     * If null, default message will be used.
     * @param callback: is triggered on the completion of the [PersonalizeCommand] and provides
     * card response in the form of [Card] if the command was performed successfully
     * or [TangemSdkError] in case of an error.
     */
    fun personalize(
        config: CardConfig,
        issuer: Issuer,
        manufacturer: Manufacturer,
        acquirer: Acquirer? = null,
        initialMessage: Message? = null,
        callback: (result: CompletionResult<Card>) -> Unit
    ) {
        val command = PersonalizeCommand(config, issuer, manufacturer, acquirer)
        startSessionWithRunnable(command, null, initialMessage, callback)
    }

    /**
     * Command available on SDK cards only
     *
     * This method launches a [DepersonalizeCommand] on a new thread.
     *
     * This command resets card to initial state,
     * erasing all data written during personalization and usage.
     *
     * @param initialMessage: A custom description that shows at the beginning of the NFC session.
     * If null, default message will be used.
     * @param callback: is triggered on the completion of the [DepersonalizeCommand] and provides
     * card response in the form of [DepersonalizeResponse] if the task was performed successfully
     * or [TangemSdkError] in case of an error.
     * */
    fun depersonalize(
        initialMessage: Message? = null,
        callback: (result: CompletionResult<DepersonalizeResponse>) -> Unit
    ) {
        startSessionWithRunnable(DepersonalizeCommand(), null, initialMessage, callback)
    }

    /**
     *
     * This method launches a [SetPinCommand] on a new thread.
     *
     * This command allows to change PIN1. This 32-byte code restricts access to the whole card.
     * App must submit the correct value of PIN1 in each command
     *
     * @param cardId: CID, Unique Tangem card ID number.
     * @param pin: PIN1 value to be set. If null, the command will trigger [SessionViewDelegate] method
     * that prompts user to enter new PIN.
     * @param initialMessage: A custom description that shows at the beginning of the NFC session.
     * If null, default message will be used.
     * @param callback: is triggered on the completion of the [DepersonalizeCommand] and provides
     * card response in the form of [DepersonalizeResponse] if the task was performed successfully
     * or [TangemSdkError] in case of an error.
     * */
    fun changePin1(
        cardId: String? = null,
        pin: ByteArray? = null,
        initialMessage: Message? = null,
        callback: (result: CompletionResult<SetPinResponse>) -> Unit
    ) {
        val command = SetPinCommand.setPin1(pin)
        startSessionWithRunnable(command, cardId, initialMessage, callback)
    }

    /**
     *
     * This method launches a [SetPinCommand] on a new thread.
     *
     * This command allows to change PIN2.
     * All cards will require submitting the correct 32-byte PIN2 code in order to sign a transaction
     * or to perform other commands entailing a change of the card state. App should ask the user
     * to enter PIN2 before sending such commands to the card.
     *
     * @param cardId: CID, Unique Tangem card ID number.
     * @param pin: PIN2 value to be set. If null, the command will trigger [SessionViewDelegate] method
     * that prompts user to enter new PIN.
     * @param initialMessage: A custom description that shows at the beginning of the NFC session.
     * If null, default message will be used.
     * @param callback: is triggered on the completion of the [DepersonalizeCommand] and provides
     * card response in the form of [DepersonalizeResponse] if the task was performed successfully
     * or [TangemSdkError] in case of an error.
     * */
    fun changePin2(
        cardId: String? = null,
        pin: ByteArray? = null,
        initialMessage: Message? = null,
        callback: (result: CompletionResult<SetPinResponse>) -> Unit
    ) {
        val command = SetPinCommand.setPin2(pin)
        startSessionWithRunnable(command, cardId, initialMessage, callback)
    }

    /**
     * Allows running a custom bunch of commands in one [CardSession] by creating a custom task.
     * [TangemSdk] will start a card session, perform preflight [ReadCommand],
     * invoke [CardSessionRunnable.run] and close the session.
     * You can find the current card in the [CardSession.environment].
     *
     * @param runnable: A custom task, adopting [CardSessionRunnable] protocol
     * @param cardId: CID, Unique Tangem card ID number. If not null, the SDK will check that you the card
     * with which you tapped a phone has this [cardId] and SDK will return
     * the [TangemSdkError.WrongCardNumber] otherwise.
     * @param initialMessage: A custom description that shows at the beginning of the NFC session.
     * If null, default message will be used.
     * @param callback: Standard [TangemSdk] callback.
     */
    fun <T : CommandResponse> startSessionWithRunnable(
        runnable: CardSessionRunnable<T>,
        cardId: String? = null,
        initialMessage: Message? = null,
        callback: (result: CompletionResult<T>) -> Unit
    ) {
        if (checkSession()) {
            callback(CompletionResult.Failure(TangemSdkError.Busy()))
            return
        }

        configure()
        cardSession = makeSession(cardId, initialMessage)
        Thread().run { cardSession?.startWithRunnable(runnable, callback) }
    }

    /**
     * Allows running  a custom bunch of commands in one [CardSession] with lightweight closure syntax.
     * Tangem SDK will start a card session and perform preflight [ReadCommand].
     *
     * @param cardId: CID, Unique Tangem card ID number. If not null, the SDK will check that you the card
     * with which you tapped a phone has this [cardId] and SDK will return
     * the [TangemSdkError.WrongCardNumber] otherwise.
     * @param initialMessage: A custom description that shows at the beginning of the NFC session.
     * If null, default message will be used.
     * @param callback: At first, you should check that the [TangemSdkError] is not null,
     * then you can use the [CardSession] to interact with a card.
     */
    fun startSession(
        cardId: String? = null,
        initialMessage: Message? = null,
        callback: (session: CardSession, error: TangemError?) -> Unit
    ) {
        if (checkSession()) {
            callback(cardSession!!, TangemSdkError.Busy())
            return
        }

        configure()
        cardSession = makeSession(cardId, initialMessage)
        Thread().run { cardSession?.start(callback = callback) }
    }

    /**
     * Allows running a custom bunch of commands in one NFC Session by creating a custom task. Tangem SDK will start
     * a card session, perform preflight [ReadCommand], invoke the `run ` method of [CardSessionRunnable] and close
     * the session. You can find the current card in the `environment` property of the [CardSession]
     *
     * @param jsonRequest: a `JSONRPCRequest`, describing specific [CardSessionRunnable]
     * @param completion: a `JSONRPCResponse` with result of the operation
     */

    fun startSessionWithJsonRequest(jsonRequest: String, callback: (String) -> Unit) {
        val request: JSONRPCRequest = try {
            JSONRPCRequest(jsonRequest)
        } catch (ex: JSONRPCException) {
            callback(ex.toJSONRPCResponse(null).toJson())
            return
        }
        try {
            if (checkSession()) throw TangemSdkError.Busy()

            configure()
            val jsonInitialMessage = jsonRpcConverter.jsonConverter.toJson(request.params["initialMessage"])
            val initialMessage = jsonRpcConverter.jsonConverter.fromJson<Message>(jsonInitialMessage)
            cardSession = makeSession(request.params["cid"] as? String, initialMessage)
            val runnable = jsonRpcConverter.convert(request)
            Thread().run {
                cardSession?.startWithRunnable(runnable) { callback(it.toJSONRPCResponse(request.id).toJson()) }
            }

        } catch (ex: Throwable) {
            val jsonStringConvertible = when (ex) {
                is TangemSdkError -> ex.toJSONRPCError().toJSONRPCResponse(request.id)
                is JSONRPCException -> ex.toJSONRPCResponse(request.id)
                else -> JSONRPCError(JSONRPCError.Type.ServerError, ex.localizedMessage).toJSONRPCResponse(request.id)
            }
            callback(jsonStringConvertible.toJson())
        }
    }

    /**
     * Register custom task, that supported JSONRPC
     *
     * @param clazz, that conforms [JSONRPCConvertible]
     */
    fun registerJSONRPCTask(clazz: KClass<*>) {
        jsonRpcConverter.register(clazz)
    }

    private fun checkSession(): Boolean {
        val session = cardSession ?: return false
        return session.state == CardSessionState.Active
    }

    private fun makeSession(cardId: String? = null, initialMessage: Message? = null): CardSession {
        return CardSession(environmentService, reader, viewDelegate, cardId, initialMessage, jsonRpcConverter)
    }

    private fun configure() {
        viewDelegate.setConfig(config)
    }

    companion object
}
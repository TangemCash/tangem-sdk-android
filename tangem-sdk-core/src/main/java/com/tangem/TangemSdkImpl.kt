package com.tangem

import com.tangem.common.CompletionResult
import com.tangem.common.SuccessResponse
import com.tangem.common.card.Card
import com.tangem.common.card.EllipticCurve
import com.tangem.common.core.*
import com.tangem.common.files.DataToWrite
import com.tangem.common.files.FileHashData
import com.tangem.common.files.FileHashHelper
import com.tangem.common.files.FileSettingsChange
import com.tangem.common.hdWallet.DerivationPath
import com.tangem.common.hdWallet.ExtendedPublicKey
import com.tangem.common.json.*
import com.tangem.common.nfc.CardReader
import com.tangem.common.services.Result
import com.tangem.common.services.secure.SecureStorage
import com.tangem.common.services.toTangemSdkError
import com.tangem.crypto.CryptoUtils
import com.tangem.operations.*
import com.tangem.operations.attestation.CardVerifyAndGetInfo
import com.tangem.operations.attestation.OnlineCardVerifier
import com.tangem.operations.derivation.DeriveWalletPublicKeyTask
import com.tangem.operations.derivation.DeriveWalletPublicKeysTask
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.operations.files.*
import com.tangem.operations.issuerAndUserData.*
import com.tangem.operations.personalization.DepersonalizeCommand
import com.tangem.operations.personalization.DepersonalizeResponse
import com.tangem.operations.personalization.PersonalizeCommand
import com.tangem.operations.personalization.entities.Acquirer
import com.tangem.operations.personalization.entities.CardConfig
import com.tangem.operations.personalization.entities.Issuer
import com.tangem.operations.personalization.entities.Manufacturer
import com.tangem.operations.pins.SetUserCodeCommand
import com.tangem.operations.sign.SignCommand
import com.tangem.operations.sign.SignHashCommand
import com.tangem.operations.sign.SignHashResponse
import com.tangem.operations.sign.SignResponse
import com.tangem.operations.wallet.*
import kotlinx.coroutines.*

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
 */
class TangemSdkImpl(
    private val reader: CardReader,
    private val viewDelegate: SessionViewDelegate,
    override val secureStorage: SecureStorage,
    override var config: Config = Config()
) : TangemSdk {

    private var cardSession: CardSession? = null
    private val onlineCardVerifier = OnlineCardVerifier()
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
     * @param initialMessage: A custom description that shows at the beginning of the NFC session.
     * If null, default message will be used.
     * @param callback is triggered on the completion of the [ScanTask] and provides card response
     * in the form of [Card] if the task was performed successfully or [TangemSdkError] in case of an error.
     */
    override fun scanCard(initialMessage: Message?, callback: CompletionCallback<Card>) {
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
     * @param derivationPath: Derivation path of the wallet. Optional
     * @param initialMessage: A custom description that shows at the beginning of the NFC session.
     * default message will be used
     * @param callback: is triggered on the completion of the [SignCommand] and provides response
     * in the form of single signed hash [ByteArray] if the task was performed successfully
     * or [TangemSdkError] in case of an error.
     */
    override fun sign(
        hash: ByteArray,
        walletPublicKey: ByteArray,
        cardId: String,
        derivationPath: DerivationPath?,
        initialMessage: Message?,
        callback: CompletionCallback<SignHashResponse>
    ) {
        val command = SignHashCommand(hash, walletPublicKey, derivationPath)
        startSessionWithRunnable(command, cardId, initialMessage, callback)
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
     * @param hashes: Array of transaction hashes. It can be from one or up to ten hashes of the same length.
     * @param walletPublicKey: Public key of the wallet that should sign hashes.
     * @param cardId: CID, Unique Tangem card ID number
     * @param derivationPath: Derivation path of the wallet. Optional
     * @param initialMessage: A custom description that shows at the beginning of the NFC session.
     * If null, default message will be used.
     * @param callback: is triggered on the completion of the [SignCommand] and provides response
     * in the form of list of signed hashes [List<ByteArray>] if the task was performed successfully
     * or [TangemSdkError] in case of an error.
     */
    override fun sign(
        hashes: Array<ByteArray>,
        walletPublicKey: ByteArray,
        cardId: String,
        derivationPath: DerivationPath?,
        initialMessage: Message?,
        callback: CompletionCallback<SignResponse>
    ) {
        val command = SignCommand(hashes, walletPublicKey, derivationPath)
        startSessionWithRunnable(command, cardId, initialMessage, callback)
    }

    /**
     * Derive public key according to BIP32 (Private parent key → public child key)
     * Warning: Only `secp256k1` and `ed25519` (BIP32-Ed25519 scheme) curves supported
     *
     * @param cardId: CID, Unique Tangem card ID number.
     * @param walletPublicKey: Seed public key.
     * @param derivationPath: Derivation path
     * @param initialMessage: A custom description that shows at the beginning of the NFC session. If null, default
     * message will be used
     * @param callback: is triggered on the completion of the [DeriveWalletPublicKeyTask] and provides response
     * in the form of the [ExtendedPublicKey] if the task was performed successfully or [TangemSdkError] in case
     * of an error.
     */
    override fun deriveWalletPublicKey(
        cardId: String,
        walletPublicKey: ByteArray,
        derivationPath: DerivationPath,
        initialMessage: Message?,
        callback: CompletionCallback<ExtendedPublicKey>
    ) {
        val command = DeriveWalletPublicKeyTask(walletPublicKey, derivationPath)
        startSessionWithRunnable(command, cardId, initialMessage, callback)
    }

    /**
     * Derive multiple wallet public keys according to BIP32 (Private parent key → public child key)
     * Warning: Only `secp256k1` and `ed25519` (BIP32-Ed25519 scheme) curves supported
     *
     * @param cardId: CID, Unique Tangem card ID number.
     * @param walletPublicKey: Seed public key.
     * @param derivationPaths: Derivation paths. Repeated items will be ignored.
     * @param initialMessage: A custom description that shows at the beginning of the NFC session. If null, default
     * message will be used
     * @param callback: is triggered on the completion of the [DeriveWalletPublicKeyTask] and provides response
     * in the form of the [ExtendedPublicKeyList] if the task was performed successfully or [TangemSdkError] in case
     * of an error. All derived keys are unique and will be returned in arbitrary order.
     */
    override fun deriveWalletPublicKeys(
        cardId: String,
        walletPublicKey: ByteArray,
        derivationPaths: List<DerivationPath>,
        initialMessage: Message?,
        callback: CompletionCallback<ExtendedPublicKeysMap>
    ) {
        val command = DeriveWalletPublicKeysTask(walletPublicKey, derivationPaths)
        startSessionWithRunnable(command, cardId, initialMessage, callback)
    }

    /**
     * This method launches a [CreateWalletTask] on a new thread.
     *
     * This command will create a new wallet on the card having ‘Empty’ state.
     * A key pair WalletPublicKey / WalletPrivateKey is generated and securely stored in the card.
     * App will need to obtain Wallet_PublicKey from the response of [CreateWalletTask] or [ReadCommand]
     * and then transform it into an address of corresponding blockchain wallet  according to a specific
     * blockchain algorithm.
     * WalletPrivateKey is never revealed by the card and will be used by [SignCommand] and [AttestWalletKeyCommand].
     * RemainingSignature is set to MaxSignatures.
     *
     * @param curve: Wallet's elliptic curve
     * @param cardId: CID, Unique Tangem card ID number.
     * @param initialMessage: A custom description that shows at the beginning of the NFC session.
     * If null, default message will be used
     * @param callback: is triggered on the completion of the [CreateWalletTask] and provides
     * card response in the form of [CreateWalletResponse] if the task was performed successfully
     * or [TangemSdkError] in case of an error.
     */
    override fun createWallet(
        curve: EllipticCurve,
        cardId: String,
        initialMessage: Message?,
        callback: CompletionCallback<CreateWalletResponse>
    ) {
        startSessionWithRunnable(CreateWalletTask(curve), cardId, initialMessage, callback)
    }

    /**
     * This method launches a [PurgeWalletCommand] on a new thread.
     *
     * This command deletes all wallet data. If IsReusable flag is enabled during personalization.
     *
     * @param walletPublicKey: Public key of wallet that should be purged.
     * @param cardId: CID, Unique Tangem card ID number.
     * @param initialMessage: A custom description that shows at the beginning of the NFC session.
     * If null, default message will be used.
     * @param callback: is triggered on the completion of the [PurgeWalletCommand] and provides
     * card response in the form of [SuccessResponse] if the task was performed successfully
     * or [TangemSdkError] in case of an error.
     */
    override fun purgeWallet(
        walletPublicKey: ByteArray,
        cardId: String,
        initialMessage: Message?,
        callback: CompletionCallback<SuccessResponse>
    ) {
        startSessionWithRunnable(PurgeWalletCommand(walletPublicKey), cardId, initialMessage, callback)
    }

    /**
     *  Get the card info and verify with Tangem backend. Do not use for developer cards
     *
     *  @param cardPublicKey: CardPublicKey returned by [ReadCommand]
     *  @param cardId: CID, Unique Tangem card ID number.
     *  @param callback: [CardVerifyAndGetInfo.Response.Item]
     */
    override fun loadCardInfo(
        cardPublicKey: ByteArray,
        cardId: String,
        callback: CompletionCallback<CardVerifyAndGetInfo.Response.Item>
    ) {
        onlineCardVerifier.scope.launch {
            when (val result = onlineCardVerifier.getCardInfo(cardId, cardPublicKey)) {
                is Result.Success -> callback(CompletionResult.Success(result.data))
                is Result.Failure -> callback(CompletionResult.Failure(result.toTangemSdkError()))
            }
        }
    }

    /**
     * This method launches a [PersonalizeCommand] on a new thread.
     *
     * Command available on SDK cards only
     * Personalization is an initialization procedure, required before starting using a card.
     * During this procedure a card setting is set up.
     * During this procedure all data exchange is encrypted.
     *
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
    override fun personalize(
        config: CardConfig,
        issuer: Issuer,
        manufacturer: Manufacturer,
        acquirer: Acquirer?,
        initialMessage: Message?,
        callback: CompletionCallback<Card>
    ) {
        val command = PersonalizeCommand(config, issuer, manufacturer, acquirer)
        startSessionWithRunnable(command, null, initialMessage, callback)
    }

    /**
     * This method launches a [DepersonalizeCommand] on a new thread.
     *
     * Command available on SDK cards only
     * This command resets card to initial state, erasing all data written during personalization and usage.
     *
     * @param initialMessage: A custom description that shows at the beginning of the NFC session.
     * If null, default message will be used.
     * @param callback: is triggered on the completion of the [DepersonalizeCommand] and provides
     * card response in the form of [DepersonalizeResponse] if the task was performed successfully
     * or [TangemSdkError] in case of an error.
     * */
    override fun depersonalize(
        initialMessage: Message?,
        callback: CompletionCallback<DepersonalizeResponse>
    ) {
        startSessionWithRunnable(DepersonalizeCommand(), null, initialMessage, callback)
    }

    /**
     * This method launches a [SetUserCodeCommand] on a new thread.
     *
     * Set or change card's access code
     *
     * @param accessCode: Access code to set. If null, the user will be prompted to enter code before operation
     * @param cardId: CID, Unique Tangem card ID number.
     * @param initialMessage: A custom description that shows at the beginning of the NFC session.
     * If null, default message will be used.
     * @param callback: is triggered on the completion of the [SetUserCodeCommand] and provides
     * card response in the form of [SuccessResponse] if the task was performed successfully
     * or [TangemSdkError] in case of an error.
     * */
    override fun setAccessCode(
        accessCode: String?,
        cardId: String,
        initialMessage: Message?,
        callback: CompletionCallback<SuccessResponse>
    ) {
        val command = SetUserCodeCommand.changeAccessCode(accessCode)
        startSessionWithRunnable(command, cardId, initialMessage, callback)
    }

    /**
     * This method launches a [SetUserCodeCommand] on a new thread.
     *
     * Set or change card's passcode
     *
     * @param passcode: Passcode to set. If null, the user will be prompted to enter code before operation
     * @param cardId: CID, Unique Tangem card ID number.
     * @param initialMessage: A custom description that shows at the beginning of the NFC session.
     * If null, default message will be used.
     * @param callback: is triggered on the completion of the [SetUserCodeCommand] and provides
     * card response in the form of [SuccessResponse] if the task was performed successfully
     * or [TangemSdkError] in case of an error.
     * */
    override fun setPasscode(
        passcode: String?,
        cardId: String,
        initialMessage: Message?,
        callback: CompletionCallback<SuccessResponse>
    ) {
        val command = SetUserCodeCommand.changePasscode(passcode)
        startSessionWithRunnable(command, cardId, initialMessage, callback)
    }

    /**
     * This method launches a [SetUserCodeCommand] on a new thread.
     *
     * Reset all user codes
     *
     * @param cardId: CID, Unique Tangem card ID number.
     * @param initialMessage: A custom description that shows at the beginning of the NFC session.
     * If null, default message will be used
     * @param callback: is triggered on the completion of the [SetUserCodeCommand] and provides
     * card response in the form of [SuccessResponse] if the task was performed successfully
     * or [TangemSdkError] in case of an error.
     */
    override fun resetUserCodes(
        cardId: String,
        initialMessage: Message?,
        callback: CompletionCallback<SuccessResponse>
    ) {
        startSessionWithRunnable(SetUserCodeCommand.resetUserCodes(), cardId, initialMessage, callback)
    }

    /**
     * This method launches a [ReadFilesTask] on a new thread.
     *
     * This command reads all files stored on card.
     * Note: When performing reading private files command, you must  provide `passcode`
     * Warning: Command available only for cards with COS 3.29 and higher
     *
     * @param readPrivateFiles: If true - all files saved on card will be read otherwise
     * @param indices: indices of files that should be read from card. If not specifies all files will be read.
     * @param cardId: CID, Unique Tangem card ID number.
     * @param initialMessage: A custom description that shows at the beginning of the NFC session.
     * If null, default message will be used
     * @param callback: is triggered on the completion of the [ReadFilesTask] and provides
     * card response in the form of [ReadFilesResponse] if the task was performed successfully
     * or [TangemSdkError] in case of an error.
     */
    override fun readFiles(
        readPrivateFiles: Boolean,
        indices: List<Int>?,
        cardId: String?,
        initialMessage: Message?,
        callback: CompletionCallback<ReadFilesResponse>
    ) {
        val task = ReadFilesTask(readPrivateFiles, indices)
        startSessionWithRunnable(task, cardId, initialMessage, callback)
    }

    /**
     * This method launches a [ChangeFileSettingsTask] on a new thread.
     *
     * Updates selected file settings provided within [File].
     * To perform file settings update you should initially read all files (`readFiles` command), select files
     * that you want to update, change their settings in [FileSettings] and add them to `files` array.
     * Note: In COS 3.29 and higher only file visibility option (public or private) available to update
     * Warning: This method works with COS 3.29 and higher
     *
     * @param changes: Array of file indices with new settings
     * @param cardId: CID, Unique Tangem card ID number.
     * @param initialMessage: A custom description that shows at the beginning of the NFC session.
     * If null, default message will be used.
     * @param callback: is triggered on the completion of the [ChangeFileSettingsTask] and provides
     * card response in the form of [SuccessResponse] if the task was performed successfully
     * or [TangemSdkError] in case of an error.
     */
    override fun changeFileSettings(
        changes: List<FileSettingsChange>,
        cardId: String?,
        initialMessage: Message?,
        callback: CompletionCallback<SuccessResponse>
    ) {
        val task = ChangeFileSettingsTask(changes)
        startSessionWithRunnable(task, cardId, initialMessage, callback)
    }

    /**
     * This method launches a [WriteFilesTask] on a new thread.
     *
     * This command write all files provided in `files` to card.
     * There are 2 main implementation of `DataToWrite` protocol:
     * 1. [FileDataProtectedBySignature] - for files  signed by Issuer (specified on card during personalization)
     * 2. [FileDataProtectedByPasscode] - write files protected by Passcode
     *  Note: Writing files protected by Passcode only available for COS 3.34 and higher
     * Warning: This command available for COS 3.29 and higher
     *
     * @param files: List of files that should be written to card
     * @param cardId: CID, Unique Tangem card ID number.
     * @param initialMessage: A custom description that shows at the beginning of the NFC session.
     * If null, default message will be used.
     * @param callback: is triggered on the completion of the [WriteFilesTask] and provides
     * card response in the form of [WriteFileResponse] if the task was performed successfully
     * or [TangemSdkError] in case of an error.
     */
    override fun writeFiles(
        files: List<DataToWrite>,
        cardId: String?,
        initialMessage: Message?,
        callback: CompletionCallback<WriteFilesResponse>
    ) {
        startSessionWithRunnable(WriteFilesTask(files), cardId, initialMessage, callback)
    }

    /**
     * This method launches a [DeleteFilesTask] on a new thread.
     *
     * This command deletes selected files from card. This operation can't be undone.
     * To perform file deletion you should initially read all files (`readFiles` command) and add them to
     * `indices` array. When files deleted from card, other files change their indices.
     * After deleting files you should additionally perform `readFiles` command to actualize files indexes
     * Warning: This command available for COS 3.29 and higher
     *
     * @param indices: Indexes of files that should be deleted. If null - deletes all files from card
     * then all files will be deleted.
     * @param cardId: CID, Unique Tangem card ID number.
     * @param initialMessage: A custom description that shows at the beginning of the NFC session.
     * If null, default message will be used.
     * @param callback: is triggered on the completion of the [DeleteFilesTask] and provides
     * card response in the form of [DeleteFileResponse] if the task was performed successfully
     * or [TangemSdkError] in case of an error.
     */
    override fun deleteFiles(
        indices: List<Int>?,
        cardId: String?,
        initialMessage: Message?,
        callback: CompletionCallback<SuccessResponse>
    ) {
        startSessionWithRunnable(DeleteFilesTask(indices), cardId, initialMessage, callback)
    }

    /**
     * Creates hashes and signatures for files that signed by issuer
     *
     * @param cardId: CID, Unique Tangem card ID number.
     * @param fileData: File data that will be written on card
     * @param fileCounter:  A counter that protects issuer data against replay attack.
     * @param privateKey: Optional private key that will be used for signing files hashes.
     * If it is provided, then  `FileHashData` will contain signed file signatures.
     * @return [FileHashData] with hashes to sign and signatures if [privateKey] was provided.
     */
    override fun prepareHashes(
        cardId: String,
        fileData: ByteArray,
        fileCounter: Int,
        privateKey: ByteArray?
    ): FileHashData {
        return FileHashHelper.prepareHashes(cardId, fileData, fileCounter, privateKey)
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
    @Deprecated(message = "Use files instead")
    override fun readIssuerData(
        cardId: String?,
        initialMessage: Message?,
        callback: CompletionCallback<ReadIssuerDataResponse>
    ) {
        val command = ReadIssuerDataCommand(config.issuerPublicKey)
        startSessionWithRunnable(command, cardId, initialMessage, callback)
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
    @Deprecated(message = "Use files instead")
    override fun writeIssuerData(
        cardId: String?,
        issuerData: ByteArray,
        issuerDataSignature: ByteArray,
        issuerDataCounter: Int?,
        initialMessage: Message?,
        callback: CompletionCallback<SuccessResponse>
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
    @Deprecated(message = "Use files instead")
    override fun readIssuerExtraData(
        cardId: String?,
        initialMessage: Message?,
        callback: CompletionCallback<ReadIssuerExtraDataResponse>
    ) {
        val command = ReadIssuerExtraDataCommand(config.issuerPublicKey)
        startSessionWithRunnable(command, cardId, initialMessage, callback)
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
     * card response in the form of [SuccessResponse] if the task was performed successfully
     * or [TangemSdkError] in case of an error.
     */
    @Deprecated(message = "Use files instead")
    override fun writeIssuerExtraData(
        cardId: String?,
        issuerData: ByteArray,
        startingSignature: ByteArray,
        finalizingSignature: ByteArray,
        issuerDataCounter: Int?,
        initialMessage: Message?,
        callback: CompletionCallback<SuccessResponse>
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
    @Deprecated(message = "Use files instead")
    override fun readUserData(
        cardId: String?,
        initialMessage: Message?,
        callback: CompletionCallback<ReadUserDataResponse>
    ) {
        val command = ReadUserDataCommand()
        startSessionWithRunnable(command, cardId, initialMessage, callback)
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
     * Writing of UserCounter and UserData is protected only by Access code.
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
     * card response in the form of [SuccessResponse] if the task was performed successfully
     * or [TangemSdkError] in case of an error.
     */
    @Deprecated(message = "Use files instead")
    override fun writeUserData(
        userData: ByteArray,
        userCounter: Int?,
        cardId: String?,
        initialMessage: Message?,
        callback: CompletionCallback<SuccessResponse>
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
     * UserProtectedCounter and UserProtectedData require Passcode for confirmation.
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
     * card response in the form of [SuccessResponse] if the task was performed successfully
     * or [TangemSdkError] in case of an error.
     */
    @Deprecated(message = "Use files instead")
    override fun writeUserProtectedData(
        userProtectedData: ByteArray,
        userProtectedCounter: Int?,
        cardId: String?,
        initialMessage: Message?,
        callback: CompletionCallback<SuccessResponse>
    ) {
        val command = WriteUserDataCommand(
            userProtectedData = userProtectedData,
            userProtectedCounter = userProtectedCounter
        )
        startSessionWithRunnable(command, cardId, initialMessage, callback)
    }

    /**
     * Allows running a custom bunch of commands in one [CardSession] by creating a custom task.
     * [TangemSdkImpl] will start a card session, perform preflight [ReadCommand],
     * invoke [CardSessionRunnable.run] and close the session.
     * You can find the current card in the [CardSession.environment].
     *
     * @param runnable: A custom task, adopting [CardSessionRunnable] protocol
     * @param cardId: CID, Unique Tangem card ID number. If not null, the SDK will check that you the card
     * with which you tapped a phone has this [cardId] and SDK will return
     * the [TangemSdkError.WrongCardNumber] otherwise.
     * @param initialMessage: A custom description that shows at the beginning of the NFC session.
     * If null, default message will be used.
     * @param callback: Standard [TangemSdkImpl] callback.
     */
    override fun <T> startSessionWithRunnable(
        runnable: CardSessionRunnable<T>,
        cardId: String?,
        initialMessage: Message?,
        callback: CompletionCallback<T>
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
    override fun startSession(
        cardId: String?,
        initialMessage: Message?,
        callback: (session: CardSession, error: TangemError?) -> Unit
    ) {
        if (checkSession()) {
            callback(cardSession!!, TangemSdkError.Busy())
            return
        }

        configure()
        cardSession = makeSession(cardId, initialMessage)
        Thread().run { cardSession?.start(onSessionStarted = callback) }
    }

    /**
     * Allows running a custom bunch of commands in one NFC Session by creating a custom task. Tangem SDK will start
     * a card session, perform preflight [ReadCommand], invoke the `run ` method of [CardSessionRunnable] and close
     * the session. You can find the current card in the `environment` property of the [CardSession]
     *
     * @param jsonRequest: a `JSONRPCRequest`, describing specific [CardSessionRunnable]
     * @param completion: a `JSONRPCResponse` with result of the operation
     */

    override fun startSessionWithJsonRequest(
        jsonRequest: String,
        cardId: String?,
        initialMessage: String?,
        callback: (String) -> Unit
    ) {
        val converter = MoshiJsonConverter.INSTANCE
        val linkersList: List<JSONRPCLinker> = try {
            JSONRPCLinker.parse(jsonRequest, converter)
        } catch (ex: JSONRPCException) {
            callback(JSONRPCResponse(null, ex.jsonRpcError, id = null).toJson())
            return
        }

        linkersList.forEach { it.initRunnable(jsonRpcConverter) }
        if (linkersList.any { it.hasError() }) {
            callback(linkersList.createResult(converter))
            return
        }

        try {
            if (checkSession()) throw TangemSdkError.Busy()

            configure()
            val message: Message? = initialMessage?.let { converter.fromJson(it) }

            if (linkersList.size == 1) {
                val jsonrpcLinker = linkersList[0]
                cardSession = makeSession(cardId, message)
                Thread().run {
                    cardSession?.startWithRunnable(jsonrpcLinker.runnable!!) {
                        jsonrpcLinker.linkResult(it)
                        callback(jsonrpcLinker.response.toJson())
                    }
                }
            } else {
                val task = RunnablesTask(linkersList)
                cardSession = makeSession(cardId, message)
                cardSession!!.startWithRunnable(task) { result ->
                    when (result) {
                        is CompletionResult.Success -> callback(converter.toJson(result.data.responses))
                        is CompletionResult.Failure -> {
                            linkersList.forEach { it.linkError(result.error) }
                            callback(linkersList.createResult(converter))
                        }
                    }
                }
            }
        } catch (ex: TangemSdkError) {
            linkersList.forEach { it.linkError(ex) }
            callback(linkersList.createResult(converter))
        }
    }

    /**
     * Register custom task, that supported JSONRPC
     *
     * @param handler, that conforms [JSONRPCHandler]
     */
    override fun registerJSONRPCTask(handler: JSONRPCHandler<*>) {
        jsonRpcConverter.register(handler)
    }

    override fun configure() {
        viewDelegate.setConfig(config)
    }

    override fun makeSession(cardId: String?, initialMessage: Message?): CardSession {
        val environment = SessionEnvironment(config, secureStorage)
        return CardSession(viewDelegate, environment, reader, jsonRpcConverter, cardId, initialMessage)
    }

    override fun checkSession(): Boolean {
        val session = cardSession ?: return false
        return session.state == CardSession.CardSessionState.Active
    }

}
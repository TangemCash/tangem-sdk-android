package com.tangem.tangem_demo.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.tangem.Message
import com.tangem.TangemSdk
import com.tangem.TangemSdkLogger
import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.guard
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toByteArray
import com.tangem.common.files.FileDataProtectedByPasscode
import com.tangem.common.files.FileDataProtectedBySignature
import com.tangem.common.files.FileHashHelper
import com.tangem.common.files.FileSettingsChange
import com.tangem.common.hdWallet.DerivationPath
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.crypto.CryptoUtils
import com.tangem.crypto.sign
import com.tangem.operations.PreflightReadMode
import com.tangem.operations.PreflightReadTask
import com.tangem.operations.attestation.AttestationTask
import com.tangem.operations.issuerAndUserData.WriteIssuerExtraDataCommand
import com.tangem.operations.personalization.entities.CardConfig
import com.tangem.tangem_demo.*
import com.tangem.tangem_demo.ui.settings.SettingsFragment

abstract class BaseFragment : Fragment() {

    protected val jsonConverter: MoshiJsonConverter = MoshiJsonConverter.default()
    protected val sdk: TangemSdk by lazy { (requireActivity() as DemoActivity).sdk }
    protected val logger: TangemSdkLogger by lazy { (requireActivity() as DemoActivity).logger }

    protected lateinit var shPrefs: SharedPreferences
    protected var bshDlg: BottomSheetDialog? = null
    protected var card: Card? = null
    protected var derivationPath: String? = null
    protected var initialMessage: Message? = null
        private set

    protected var selectedIndexOfWallet = -1
    protected val selectedWalletPubKey: ByteArray?
        get() {
            if (selectedIndexOfWallet == -1) return null
            val card = card ?: return null
            if (card.wallets.isEmpty() || selectedIndexOfWallet >= card.wallets.size) return null

            return card.wallets[selectedIndexOfWallet].publicKey
        }

    private var needRescanCard = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        shPrefs = (requireContext().applicationContext as DemoApplication).shPrefs
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(getLayoutId(), container, false)
    }

    override fun onResume() {
        super.onResume()
        initInitialMessage()
    }

    private fun initInitialMessage() {
        if (!shPrefs.getBoolean(SettingsFragment.initialMessageEnabled, false)) {
            initialMessage = null
            return
        }
        val header = shPrefs.getString(SettingsFragment.initialMessageHeader, null)
        val body = shPrefs.getString(SettingsFragment.initialMessageBody, null)
        if (header == null && body == null) return

        initialMessage = Message(header, body)
    }

    protected fun launchJSONRPC(json: String) {
        val message = initialMessage?.let { jsonConverter.toJson(it) }
        sdk.startSessionWithJsonRequest(json, card?.cardId, message) {
            val response = jsonConverter.prettyPrint(jsonConverter.fromJson<Any>(it))
            postUi { showDialog(response) }
        }
    }

    protected fun scanCard() {
        sdk.scanCard(initialMessage) {
            needRescanCard = false
            handleResult(it)
        }
    }

    protected fun personalize(config: CardConfig) {
        sdk.personalize(config, Personalization.issuer(), Personalization.manufacturer(), Personalization.acquirer()) {
            handleResult(it)
        }
    }

    protected fun depersonalize() {
        sdk.depersonalize { handleResult(it) }
    }

    protected fun loadCardInfo() {
        if (card?.cardId == null || card?.cardPublicKey == null) {
            showToast("CardId & publicKey required. Scan your card before proceeding")
            return
        }
        sdk.loadCardInfo(card?.cardPublicKey!!, card?.cardId!!) { handleResult(it) }
    }

    protected fun attestCard(mode: AttestationTask.Mode) {
        val command = AttestationTask(mode, sdk.secureStorage)
        sdk.startSessionWithRunnable(command, card?.cardId, initialMessage) { handleResult(it) }
    }

    protected fun derivePublicKey() {
        val card = card.guard {
            showToast("CardId required. Scan your card before proceeding")
            return
        }
        val walletPublicKey = selectedWalletPubKey.guard {
            showToast("Wallet publicKey required. Scan your card before proceeding")
            return
        }
        val path = createDerivationPath().guard {
            showToast("Failed to parse hd path")
            return
        }

        sdk.deriveWalletPublicKey(card.cardId, walletPublicKey, path) { handleResult(it) }
    }

    protected fun signHash(hash: ByteArray) {
        val cardId = card?.cardId.guard {
            showToast("CardId & walletPublicKey required. Scan your card before proceeding")
            return
        }
        val publicKey = selectedWalletPubKey.guard {
            showToast("Wallet publicKey is null")
            return
        }
        val path = createDerivationPath()
        if (!derivationPath.isNullOrBlank() && path == null) {
            showToast("Failed to parse hd path")
            return
        }
        sdk.sign(hash, publicKey, cardId, path, initialMessage) { handleResult(it) }
    }

    protected fun signHashes(hashes: Array<ByteArray>) {
        val cardId = card?.cardId.guard {
            showToast("CardId & walletPublicKey required. Scan your card before proceeding")
            return
        }
        val publicKey = selectedWalletPubKey.guard {
            showToast("Wallet publicKey is null")
            return
        }
        val path = createDerivationPath()
        if (!derivationPath.isNullOrBlank() && path == null) {
            showToast("Failed to parse hd path")
            return
        }
        sdk.sign(hashes, publicKey, cardId, path, initialMessage) { handleResult(it) }
    }

    protected fun createWallet(curve: EllipticCurve) {
        val cardId = card?.cardId.guard {
            showToast("CardId & walletPublicKey required. Scan your card before proceeding")
            return
        }
        sdk.createWallet(curve, cardId, initialMessage) {
            needRescanCard = it is CompletionResult.Success
            handleResult(it)
        }
    }

    protected fun purgeWallet() {
        val cardId = card?.cardId.guard {
            showToast("CardId & walletPublicKey required. Scan your card before proceeding")
            return
        }
        val publicKey = selectedWalletPubKey.guard {
            showToast("Wallet publicKey is null")
            return
        }
        sdk.purgeWallet(publicKey, cardId, initialMessage) {
            needRescanCard = it is CompletionResult.Success
            handleResult(it)
        }
    }

    protected fun readIssuerData() {
        sdk.readIssuerData(card?.cardId, initialMessage) { handleResult(it) }
    }

    protected fun writeIssuerData() {
        val cardId = card?.cardId.guard {
            showToast("CardId required. Scan your card before proceeding")
            return
        }

        val issuerData = Utils.randomString(Utils.randomInt(15, 30)).toByteArray()
        val counter = 1
        val issuerPrivateKey = Personalization.issuer().dataKeyPair.privateKey
        val signedIssuerData = (cardId.hexToBytes() + issuerData + counter.toByteArray(4)).sign(issuerPrivateKey)

        sdk.writeIssuerData(cardId, issuerData, signedIssuerData, counter, initialMessage) { handleResult(it) }
    }

    @Deprecated("Deprecated in the TangemSdk")
    protected fun readIssuerExtraData() {
        sdk.readIssuerExtraData(card?.cardId, initialMessage) { handleResult(it) }
    }

    @Deprecated("Deprecated in the TangemSdk")
    protected fun writeIssuerExtraData() {
        val cardId = card?.cardId.guard {
            showToast("CardId required. Scan your card before proceeding")
            return
        }

        val counter = 1
        val issuerData = CryptoUtils.generateRandomBytes(WriteIssuerExtraDataCommand.SINGLE_WRITE_SIZE * 5)
        val signatures = FileHashHelper.prepareHashes(
            cardId, issuerData, counter, Personalization.issuer().dataKeyPair.privateKey
        )

        sdk.writeIssuerExtraData(
            cardId, issuerData,
            signatures.startingSignature!!, signatures.finalizingSignature!!,
            counter, initialMessage
        ) { handleResult(it) }
    }

    protected fun readUserData() {
        sdk.readUserData(card?.cardId, initialMessage) { handleResult(it) }
    }

    protected fun writeUserData() {
        val userData = "Some of user data ${Utils.randomString(20)}".toByteArray()
        val counter = 1
        sdk.writeUserData(userData, counter, card?.cardId, initialMessage) { handleResult(it) }
    }

    protected fun writeUserProtectedData() {
        val userProtectedData = "Some of user protected data ${Utils.randomString(20)}".toByteArray()
        val counter = 1
        sdk.writeUserProtectedData(userProtectedData, counter, card?.cardId, initialMessage) { handleResult(it) }
    }

    protected fun setAccessCode() {
        val cardId = card?.cardId.guard {
            showToast("CardId & walletPublicKey required. Scan your card before proceeding")
            return
        }
        sdk.setAccessCode(null, cardId, initialMessage) { handleResult(it) }
    }

    protected fun setPasscode() {
        val cardId = card?.cardId.guard {
            showToast("CardId & walletPublicKey required. Scan your card before proceeding")
            return
        }
        sdk.setPasscode(null, cardId, initialMessage) { handleResult(it) }
    }

    protected fun resetUserCodes() {
        val cardId = card?.cardId.guard {
            showToast("CardId & walletPublicKey required. Scan your card before proceeding")
            return
        }
        sdk.resetUserCodes(cardId, initialMessage) { handleResult(it) }
    }

    protected fun readFiles(readPrivateFiles: Boolean) {
        sdk.readFiles(readPrivateFiles, null, card?.cardId, initialMessage) { handleResult(it) }
    }

    protected fun writeFilesSigned() {
        val cardId = card?.cardId.guard {
            showToast("CardId required. Scan your card before proceeding")
            return
        }
        val file = prepareSignedData(cardId)
        sdk.writeFiles(listOf(file), card?.cardId, initialMessage) { handleResult(it) }
    }

    protected fun writeFilesWithPassCode() {
        val issuerData = CryptoUtils.generateRandomBytes(WriteIssuerExtraDataCommand.SINGLE_WRITE_SIZE * 5)
        val files = listOf(FileDataProtectedByPasscode(issuerData), FileDataProtectedByPasscode(issuerData))
        sdk.writeFiles(files, card?.cardId, initialMessage) { handleResult(it) }
    }

    protected fun deleteFiles(indices: List<Int>? = null) {
        sdk.deleteFiles(indices, card?.cardId, initialMessage) { handleResult(it) }
    }

    protected fun changeFilesSettings(change: FileSettingsChange) {
        sdk.changeFileSettings(listOf(change), card?.cardId, initialMessage) { handleResult(it) }
    }

    private fun handleResult(result: CompletionResult<*>) {
        setCard(result)
        postUi { handleCommandResult(result) }
    }

    private fun setCard(completionResult: CompletionResult<*>) {
        if (completionResult is CompletionResult.Success) {
            when {
                needRescanCard -> {
                    val command = PreflightReadTask(PreflightReadMode.FullCardRead, card?.cardId)
                    sdk.startSessionWithRunnable(command) {
                        needRescanCard = false
                        setCard(it)
                    }
                }
                completionResult.data is Card -> {
                    card = completionResult.data as Card
                    onCardChanged(card)
                }
            }
        }
    }

    protected fun showDialog(message: String) {
        val dlg = bshDlg ?: BottomSheetDialog(requireActivity())
        if (dlg.isShowing) dlg.hide()

        dlg.setContentView(R.layout.bottom_sheet_response_layout)
        val tv = dlg.findViewById<TextView>(R.id.tvResponse) ?: return

        tv.text = message
        dlg.show()
    }

    protected fun showToast(message: String) {
        activity?.let { Toast.makeText(it, message, Toast.LENGTH_LONG).show() }
    }

    protected fun prepareHashesToSign(count: Int): Array<ByteArray> {
        val listOfData = MutableList(count) { Utils.randomString(32) }
        val listOfHashes = listOfData.map { it.toByteArray() }
        return listOfHashes.toTypedArray()
    }

    protected fun prepareSignedData(cardId: String): FileDataProtectedBySignature {
        val counter = 1
        val issuer = Personalization.issuer()
        val issuerData = CryptoUtils.generateRandomBytes(WriteIssuerExtraDataCommand.SINGLE_WRITE_SIZE * 5)
        val signatures = FileHashHelper.prepareHashes(
            cardId, issuerData, counter, issuer.dataKeyPair.privateKey
        )
        return FileDataProtectedBySignature(
            issuerData,
            signatures.startingSignature!!,
            signatures.finalizingSignature!!,
            counter,
        )
    }

    private fun createDerivationPath(): DerivationPath? {
        val hdPath = derivationPath ?: return null
        if (hdPath.isEmpty() || hdPath.isBlank()) return null

        return try {
            DerivationPath(hdPath)
        } catch (ex: Exception) {
            null
        }
    }

    protected abstract fun getLayoutId(): Int
    abstract fun handleCommandResult(result: CompletionResult<*>)
    abstract fun onCardChanged(card: Card?)
}
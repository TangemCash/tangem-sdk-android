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
import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.card.EllipticCurve
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.Config
import com.tangem.common.extensions.guard
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toByteArray
import com.tangem.common.files.FileDataProtectedByPasscode
import com.tangem.common.files.FileDataProtectedBySignature
import com.tangem.common.files.FileHashHelper
import com.tangem.common.files.FileSettingsChange
import com.tangem.crypto.CryptoUtils
import com.tangem.crypto.sign
import com.tangem.operations.issuerAndUserData.WriteIssuerExtraDataCommand
import com.tangem.tangem_demo.DemoApplication
import com.tangem.tangem_demo.R
import com.tangem.tangem_demo.Utils
import com.tangem.tangem_demo.postUi
import com.tangem.tangem_demo.ui.settings.SettingsFragment

abstract class BaseFragment : Fragment() {

    protected lateinit var shPrefs: SharedPreferences
    protected lateinit var sdk: TangemSdk

    protected var card: Card? = null
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

    protected var bshDlg: BottomSheetDialog? = null

    private var needRescanCard = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        shPrefs = (requireContext().applicationContext as DemoApplication).shPrefs
        sdk = initSdk()
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

    protected fun createSdkConfig(): Config = Config().apply {
        linkedTerminal = false
        filter.allowedCardTypes = FirmwareVersion.FirmwareType.values().toList()
    }

    protected fun scanCard(updateOnlyCard: Boolean = false) {
        sdk.scanCard(initialMessage) {
            needRescanCard = false
            if (updateOnlyCard) setCard(it) else handleResult(it)
        }
    }

    protected fun loadCardInfo() {
        if (card?.cardId == null || card?.cardPublicKey == null) {
            showToast("CardId & publicKey required. Scan your card before proceeding")
            return
        }
        sdk.loadCardInfo(card?.cardPublicKey!!, card?.cardId!!) { handleResult(it) }
    }

    protected fun signHash(hash: ByteArray) {
        val publicKey = selectedWalletPubKey.guard {
            showToast("Wallet publicKey is null")
            return
        }
        sdk.sign(hash, publicKey, card?.cardId, initialMessage) { handleResult(it) }
    }

    protected fun signHashes(hashes: Array<ByteArray>) {
        val publicKey = selectedWalletPubKey.guard {
            showToast("Wallet publicKey is null")
            return
        }
        sdk.sign(hashes, publicKey, card?.cardId, initialMessage) { handleResult(it) }
    }

    protected fun createWallet(curve: EllipticCurve, isPermanent: Boolean) {
        sdk.createWallet(curve, isPermanent, card?.cardId, initialMessage) {
            needRescanCard = it is CompletionResult.Success
            handleResult(it)
        }
    }

    protected fun purgeWallet() {
        val publicKey = selectedWalletPubKey.guard {
            showToast("Wallet publicKey is null")
            return
        }
        sdk.purgeWallet(publicKey, card?.cardId, initialMessage) {
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
        val issuerPrivateKey = Utils.issuer().dataKeyPair.privateKey
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
                cardId, issuerData, counter, Utils.issuer().dataKeyPair.privateKey
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

    protected fun setPin1() {
        sdk.setAccessCode(null, card?.cardId, initialMessage) { handleResult(it) }
    }

    protected fun setPin2() {
        sdk.setPasscode(null, card?.cardId, initialMessage) { handleResult(it) }
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
        sdk.changeFilesSettings(listOf(change), card?.cardId, initialMessage) { handleResult(it) }
    }

    private fun handleResult(result: CompletionResult<*>) {
        setCard(result)
        postUi { handleCommandResult(result) }
    }

    private fun setCard(completionResult: CompletionResult<*>) {
        if (completionResult is CompletionResult.Success) {
            when {
                needRescanCard -> scanCard(true)
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
        val listOfData = MutableList(count) { Utils.randomString(20) }
        val listOfHashes = listOfData.map { it.toByteArray() }
        return listOfHashes.toTypedArray()
    }

    protected fun prepareSignedData(cardId: String): FileDataProtectedBySignature {
        val counter = 1
        val issuer = Utils.issuer()
        val issuerData = CryptoUtils.generateRandomBytes(WriteIssuerExtraDataCommand.SINGLE_WRITE_SIZE * 5)
        val signatures = FileHashHelper.prepareHashes(
                cardId, issuerData, counter, issuer.dataKeyPair.privateKey
        )
        return FileDataProtectedBySignature(
                issuerData,
                signatures.startingSignature!!,
                signatures.finalizingSignature!!,
                counter,
                issuer.dataKeyPair.publicKey
        )
    }

    protected abstract fun getLayoutId(): Int
    protected abstract fun initSdk(): TangemSdk
    abstract fun handleCommandResult(result: CompletionResult<*>)
    abstract fun onCardChanged(card: Card?)
}
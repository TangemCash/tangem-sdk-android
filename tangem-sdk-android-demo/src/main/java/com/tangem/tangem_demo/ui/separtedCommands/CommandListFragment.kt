package com.tangem.tangem_demo.ui.separtedCommands

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import com.google.android.material.slider.Slider
import com.tangem.Log
import com.tangem.Message
import com.tangem.TangemSdk
import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.card.EllipticCurve
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.guard
import com.tangem.common.files.FileSettings
import com.tangem.common.files.FileSettingsChange
import com.tangem.operations.attestation.AttestationTask
import com.tangem.operations.pins.SetUserCodeCommand
import com.tangem.operations.sign.SignHashResponse
import com.tangem.tangem_demo.DemoActivity
import com.tangem.tangem_demo.R
import com.tangem.tangem_demo.ui.BaseFragment
import com.tangem.tangem_demo.ui.extension.fitChipsByGroupWidth
import com.tangem.tangem_demo.ui.extension.getFromClipboard
import com.tangem.tangem_sdk_new.extensions.createLogger
import com.tangem.tangem_sdk_new.extensions.init
import kotlinx.android.synthetic.main.attestation.*
import kotlinx.android.synthetic.main.file_data.*
import kotlinx.android.synthetic.main.hd_wallet.*
import kotlinx.android.synthetic.main.issuer_data.*
import kotlinx.android.synthetic.main.issuer_ex_data.*
import kotlinx.android.synthetic.main.json_rpc.*
import kotlinx.android.synthetic.main.scan_card.*
import kotlinx.android.synthetic.main.set_message.*
import kotlinx.android.synthetic.main.set_pin.*
import kotlinx.android.synthetic.main.sign.*
import kotlinx.android.synthetic.main.user_data.*
import kotlinx.android.synthetic.main.wallet.*

/**
 * Created by Anton Zhilenkov on 23/12/2020.
 */
class CommandListFragment : BaseFragment() {

    private val logger = TangemSdk.createLogger()

    override fun initSdk(): TangemSdk = TangemSdk.init(this.requireActivity(), createSdkConfig())

    override fun getLayoutId(): Int = R.layout.fg_command_list

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as? DemoActivity)?.listenPageChanges {
            if (it == 0) Log.addLogger(logger)
            else Log.removeLogger(logger)
        }
        btnScanCard.setOnClickListener { scanCard() }
        btnLoadCardInfo.setOnClickListener { loadCardInfo() }

        chipGroupAttest.fitChipsByGroupWidth()
        btnAttest.setOnClickListener {
            val mode = when (chipGroupAttest.checkedChipId) {
                R.id.chipAttestOffline -> AttestationTask.Mode.Offline
                R.id.chipAttestNormal -> AttestationTask.Mode.Normal
                else -> AttestationTask.Mode.Full
            }
            attestCard(mode)
        }

        val adapter = ArrayAdapter(view.context, android.R.layout.simple_dropdown_item_1line, listOf(
            "m/0/1",
            "m/0'/1'/2",
            "m/44'/0'/0'/1/0"
        ))
        etDerivePublicKey.setAdapter(adapter)
        etDerivePublicKey.addTextChangedListener { hdPath = if (it!!.isEmpty()) null else it!!.toString() }
        btnDerivePublicKey.setOnClickListener { derivePublicKey() }

        btnSignHash.setOnClickListener { signHash(prepareHashesToSign(1)[0]) }
        btnSignHashes.setOnClickListener { signHashes(prepareHashesToSign(11)) }

        btnCreateWalletSecpK1.setOnClickListener { createWallet(EllipticCurve.Secp256k1) }
        btnCreateWalletSecpR1.setOnClickListener { createWallet(EllipticCurve.Secp256r1) }
        btnCreateWalletEdwards.setOnClickListener { createWallet(EllipticCurve.Ed25519) }
        btnPurgeWallet.setOnClickListener { purgeWallet() }

        btnReadIssuerData.setOnClickListener { readIssuerData() }
        btnWriteIssuerData.setOnClickListener { writeIssuerData() }

        btnReadIssuerExData.setOnClickListener { readIssuerExtraData() }
        btnWriteIssuerExData.setOnClickListener { writeIssuerExtraData() }

        btnReadUserData.setOnClickListener { readUserData() }
        btnWriteUserData.setOnClickListener { writeUserData() }
        btnWriteUserProtectedData.setOnClickListener { writeUserProtectedData() }

        btnSetPin1.setOnClickListener { setPin1() }
        btnSetPin2.setOnClickListener { setPin2() }

        btnReadAllFiles.setOnClickListener { readFiles(true) }
        btnReadPublicFiles.setOnClickListener { readFiles(false) }
        btnWriteSignedFile.setOnClickListener { writeFilesSigned() }
        btnWriteFilesWithPasscode.setOnClickListener { writeFilesWithPassCode() }
        btnDeleteAll.setOnClickListener { deleteFiles() }
        btnDeleteFirst.setOnClickListener { deleteFiles(listOf(0)) }
        btnMakeFilePublic.setOnClickListener { changeFilesSettings(FileSettingsChange(0, FileSettings.Public)) }
        btnMakeFilePrivate.setOnClickListener { changeFilesSettings(FileSettingsChange(0, FileSettings.Private)) }

        etJsonRpc.setText(jsonRpcSingleCommandTemplate)
        btnSingleJsonRpc.setOnClickListener { etJsonRpc.setText(jsonRpcSingleCommandTemplate) }
        btnListJsonRpc.setOnClickListener { etJsonRpc.setText(jsonRpcListCommandsTemplate) }
        btnPasteJsonRpc.setOnClickListener { requireContext().getFromClipboard()?.let { etJsonRpc.setText(it) } }
        btnLaunchJsonRpc.setOnClickListener { launchJSONRPC(etJsonRpc.text.toString().trim()) }

        btnCheckSetMessage.setOnClickListener {
            sdk.startSessionWithRunnable(MultiMessageTask(), card?.cardId, initialMessage) { handleCommandResult(it) }
        }
        
        sliderWallet.stepSize = 1f
    }

    override fun handleCommandResult(result: CompletionResult<*>) {
        when (result) {
            is CompletionResult.Success -> {
                val json = jsonConverter.prettyPrint(result.data)
                showDialog(json)
            }
            is CompletionResult.Failure -> {
                if (result.error is TangemSdkError.UserCancelled) {
                    showToast("${result.error.customMessage}: User was cancelled the operation")
                } else {
                    showToast(result.error.customMessage)
                }
            }
        }
    }

    override fun onCardChanged(card: Card?) {
        updateWalletsSlider()
    }

    private fun updateWalletsSlider() {
        sliderWallet.removeOnSliderTouchListener(touchListener)
        val card = card.guard {
            walletIndexesContainer.visibility = View.GONE
            selectedIndexOfWallet = -1
            return
        }
        val walletsCount = card.wallets.size
        if (walletsCount == 0) {
            walletIndexesContainer.visibility = View.GONE
            selectedIndexOfWallet = -1
            return
        }

        sliderWallet.post {
            if (walletsCount == 1) {
                selectedIndexOfWallet = 0
                walletIndexesContainer.visibility = View.GONE
                sliderWallet.valueTo = 0f
                return@post
            }

            walletIndexesContainer.visibility = View.VISIBLE
            sliderWallet.valueFrom = 0f
            sliderWallet.valueTo = walletsCount - 1f

            if (selectedIndexOfWallet == -1 || selectedIndexOfWallet >= walletsCount) {
                selectedIndexOfWallet = 0
            }
            sliderWallet.value = selectedIndexOfWallet.toFloat()
            tvWalletIndex.text = "$selectedIndexOfWallet"
            sliderWallet.addOnSliderTouchListener(touchListener)
        }
    }

    private val touchListener = object : Slider.OnSliderTouchListener {
        override fun onStartTrackingTouch(slider: Slider) {
        }

        override fun onStopTrackingTouch(slider: Slider) {
            selectedIndexOfWallet = slider.value.toInt()
            tvWalletIndex.text = "$selectedIndexOfWallet"
        }
    }

    private var jsonRpcSingleCommandTemplate: String = """
    {
        "jsonrpc": "2.0",
        "id": 2,
        "method": "scan",
        "params": {}
    }
    """.trim()

    private var jsonRpcListCommandsTemplate: String = """
    [
        {
          "method": "scan",
          "params": {},
          "id": 1,
          "jsonrpc": "2.0"
        },
        {
          "method": "create_wallet",
          "params": {
            "curve": "Secp256k1"
          },
          "jsonrpc": "2.0"
        },
        {
          "method": "scan",
          "id": 2,
          "jsonrpc": "2.0"
        }
    ]
    """.trim()
}

class MultiMessageTask : CardSessionRunnable<SignHashResponse> {

    val message1 = Message("Header - 1", "Body - 1")
    val message2 = Message("Header - 2", "Body - 2")

    override fun run(session: CardSession, callback: CompletionCallback<SignHashResponse>) {
        session.setMessage(message1)
        Thread.sleep(2000)
        session.setMessage(message2)
        Thread.sleep(2000)
        SetUserCodeCommand.changeAccessCode("1").run(session) {
            when (it) {
                is CompletionResult.Success -> {
                    session.setMessage(Message("Success", "SignHashCommand"))
                    Thread.sleep(2000)
                    callback(CompletionResult.Failure(TangemSdkError.ExceptionError(
                        Throwable("Test error message")
                    )))
                }
                is CompletionResult.Failure -> {
                    session.setMessage(Message("Success", "SignHashCommand"))
                    Thread.sleep(2000)
                    callback(CompletionResult.Failure(it.error))
                }
            }
        }
    }
}
package com.tangem.demo.ui.separtedCommands

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.google.android.material.slider.Slider
import com.tangem.common.CompletionResult
import com.tangem.common.UserCodeType
import com.tangem.common.card.Card
import com.tangem.common.card.EllipticCurve
import com.tangem.common.core.Config
import com.tangem.common.core.TangemSdkError
import com.tangem.common.core.UserCodeRequestPolicy
import com.tangem.common.extensions.toHexString
import com.tangem.demo.Backup
import com.tangem.demo.postUi
import com.tangem.demo.ui.BaseFragment
import com.tangem.demo.ui.backup.BackupActivity
import com.tangem.demo.ui.extension.beginDelayedTransition
import com.tangem.demo.ui.extension.copyToClipboard
import com.tangem.demo.ui.extension.fitChipsByGroupWidth
import com.tangem.demo.ui.extension.setTextFromClipboard
import com.tangem.demo.ui.separtedCommands.task.MultiMessageTask
import com.tangem.demo.ui.separtedCommands.task.ResetToFactorySettingsTask
import com.tangem.operations.attestation.AttestationTask
import com.tangem.operations.files.FileVisibility
import com.tangem.tangem_demo.R
import kotlinx.android.synthetic.main.attestation.*
import kotlinx.android.synthetic.main.backup.*
import kotlinx.android.synthetic.main.card.*
import kotlinx.android.synthetic.main.file_data.*
import kotlinx.android.synthetic.main.hd_wallet.*
import kotlinx.android.synthetic.main.issuer_data.*
import kotlinx.android.synthetic.main.issuer_ex_data.*
import kotlinx.android.synthetic.main.json_rpc.*
import kotlinx.android.synthetic.main.set_pin.*
import kotlinx.android.synthetic.main.sign.*
import kotlinx.android.synthetic.main.user_data.*
import kotlinx.android.synthetic.main.utils.*
import kotlinx.android.synthetic.main.wallet.*

/**
 * Created by Anton Zhilenkov on 23/12/2020.
 */
class CommandListFragment : BaseFragment() {

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

    private val sliderTouchListener = object : Slider.OnSliderTouchListener {
        @SuppressLint("RestrictedApi")
        override fun onStartTrackingTouch(slider: Slider) {
        }

        @SuppressLint("RestrictedApi")
        override fun onStopTrackingTouch(slider: Slider) {
            selectedIndexOfWallet = slider.value.toInt()
            updateWalletInfo()
        }
    }

    override fun getLayoutId(): Int = R.layout.fg_command_list

    @Suppress("LongMethod")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chipGroupUserCodeRequestPolicy.fitChipsByGroupWidth()
        chipGroupUserCodeRequestPolicy.setOnCheckedChangeListener { _, checkedId ->
            val showTypeSelector = checkedId == R.id.chipPolicyAlways ||
                checkedId == R.id.chipPolicyAlwaysWithBiometrics
            chipGroupUserCodeType.isVisible = showTypeSelector
            userCodeRequestPolicyDivider.isVisible = showTypeSelector
        }
        btnScanCard.setOnClickListener {
            val type = when (chipGroupUserCodeType.checkedChipId) {
                R.id.chipTypeAccessCode -> UserCodeType.AccessCode
                R.id.chipTypePasscode -> UserCodeType.Passcode
                else -> UserCodeType.AccessCode
            }
            val policy = when (chipGroupUserCodeRequestPolicy.checkedChipId) {
                R.id.chipPolicyDefault -> UserCodeRequestPolicy.Default
                R.id.chipPolicyAlways -> UserCodeRequestPolicy.Always(type)
                R.id.chipPolicyAlwaysWithBiometrics -> UserCodeRequestPolicy.AlwaysWithBiometrics(type)
                else -> Config().userCodeRequestPolicy
            }
            scanCard(policy)
        }
        btnLoadCardInfo.setOnClickListener { loadCardInfo() }

        btnPersonalizePrimary.setOnClickListener { personalize(Backup.primaryCardConfig()) }
        btnPersonalizeBackup1.setOnClickListener { personalize(Backup.backup1Config()) }
        btnPersonalizeBackup2.setOnClickListener { personalize(Backup.backup2Config()) }
        btnDepersonalize.setOnClickListener { depersonalize() }

        btnStartBackup.setOnClickListener {
            val intent = Intent(requireContext(), BackupActivity::class.java)
            startActivity(intent)
        }

        chipGroupAttest.fitChipsByGroupWidth()
        btnAttest.setOnClickListener {
            val mode = when (chipGroupAttest.checkedChipId) {
                R.id.chipAttestOffline -> AttestationTask.Mode.Offline
                R.id.chipAttestNormal -> AttestationTask.Mode.Normal
                else -> AttestationTask.Mode.Full
            }
            attestCard(mode)
        }

        val adapter = ArrayAdapter(
            view.context,
            android.R.layout.simple_dropdown_item_1line,
            listOf(
                "m/0/1",
                "m/0'/1'/2",
                "m/44'/0'/0'/1/0",
            ),
        )
        etDerivePublicKey.setAdapter(adapter)
        etDerivePublicKey.addTextChangedListener { derivationPath = if (it!!.isEmpty()) null else it.toString() }
        btnDerivePublicKey.setOnClickListener { derivePublicKey() }

        btnPasteHashes.setOnClickListener { etHashesToSign.setTextFromClipboard() }
        btnSignHash.setOnClickListener { sign(SignStrategyType.SINGLE) }
        btnSignHashes.setOnClickListener { sign(SignStrategyType.MULTIPLE) }

        btnCreateWalletSecpK1.setOnClickListener {
            createOrImportWallet(
                EllipticCurve.Secp256k1,
                etMnemonic.text?.toString(),
            )
        }
        btnCreateWalletSecpR1.setOnClickListener {
            createOrImportWallet(
                EllipticCurve.Secp256r1,
                etMnemonic.text?.toString(),
            )
        }
        btnCreateWalletEdwards.setOnClickListener {
            createOrImportWallet(
                EllipticCurve.Ed25519,
                etMnemonic.text?.toString(),
            )
        }
        btnPasteMnemonic.setOnClickListener { etMnemonic.setTextFromClipboard() }

        btnPurgeWallet.setOnClickListener { purgeWallet() }
        btnPurgeAllWallet.setOnClickListener { purgeAllWallet() }

        btnReadIssuerData.setOnClickListener { readIssuerData() }
        btnWriteIssuerData.setOnClickListener { writeIssuerData() }

        btnReadIssuerExData.setOnClickListener { readIssuerExtraData() }
        btnWriteIssuerExData.setOnClickListener { writeIssuerExtraData() }

        btnReadUserData.setOnClickListener { readUserData() }
        btnWriteUserData.setOnClickListener { writeUserData() }
        btnWriteUserProtectedData.setOnClickListener { writeUserProtectedData() }

        btnSetAccessCode.setOnClickListener { setAccessCode() }
        btnSetPasscode.setOnClickListener { setPasscode() }
        btnResetUserCodes.setOnClickListener { resetUserCodes() }
        btnClearUserCodes.setOnClickListener {
            clearUserCodes()
            userCodeRepositoryContainer.isVisible = hasSavedUserCodes()
        }
        btnDeleteUserCode.setOnClickListener {
            deleteUserCodeForScannedCard()
            if (hasSavedUserCodes()) {
                btnDeleteUserCode.isVisible = hasSavedUserCodeForScannedCard()
            } else {
                userCodeRepositoryContainer.isVisible = false
            }
        }

        btnReadAllFiles.setOnClickListener { readFiles(true) }
        btnReadPublicFiles.setOnClickListener { readFiles(false) }
        btnWriteUserFile.setOnClickListener { writeUserFile() }
        btnWriteOwnerFile.setOnClickListener { writeOwnerFile() }
        btnDeleteAll.setOnClickListener { deleteFiles() }
        btnDeleteFirst.setOnClickListener { deleteFiles(listOf(0)) }
        btnMakeFilePublic.setOnClickListener { changeFilesSettings(mapOf(0 to FileVisibility.Public)) }
        btnMakeFilePrivate.setOnClickListener { changeFilesSettings(mapOf(0 to FileVisibility.Private)) }

        etJsonRpc.setText(jsonRpcSingleCommandTemplate)
        btnSingleJsonRpc.setOnClickListener { etJsonRpc.setText(jsonRpcSingleCommandTemplate) }
        btnListJsonRpc.setOnClickListener { etJsonRpc.setText(jsonRpcListCommandsTemplate) }
        btnPasteJsonRpc.setOnClickListener { etJsonRpc.setTextFromClipboard() }
        btnLaunchJsonRpc.setOnClickListener { launchJSONRPC(etJsonRpc.text.toString().trim()) }

        btnResetToFactory.setOnClickListener {
            sdk.startSessionWithRunnable(ResetToFactorySettingsTask()) {
                postUi { handleCommandResult(it) }
            }
        }
        btnCheckSetMessage.setOnClickListener {
            sdk.startSessionWithRunnable(
                runnable = MultiMessageTask(),
                cardId = card?.cardId,
                initialMessage = initialMessage,
            ) { postUi { handleCommandResult(it) } }
        }

        sliderWallet.stepSize = 1f
        tvWalletPubKey.setOnClickListener {
            requireContext().copyToClipboard(tvWalletPubKey.text)
            showToast("PubKey copied to clipboard")
        }
    }

    private fun sign(strategyType: SignStrategyType) {
        val userHexHash = etHashesToSign.text.toString()
        val strategy = when (strategyType) {
            SignStrategyType.SINGLE -> {
                SingleSignStrategy(userHexHash, ::signHash, ::prepareHashesToSign)
            }
            SignStrategyType.MULTIPLE -> {
                MultiplySignStrategy(userHexHash, ::signHashes, ::prepareHashesToSign)
            }
        }
        strategy.onError = { showToast(it) }
        strategy.execute()
    }

    override fun handleCommandResult(result: CompletionResult<*>) {
        when (result) {
            is CompletionResult.Success -> {
                val json = jsonConverter.prettyPrint(result.data)
                showDialog(json)

                if (hasSavedUserCodes()) {
                    userCodeRepositoryContainer.isVisible = true
                    btnDeleteUserCode.isVisible = hasSavedUserCodeForScannedCard()
                } else {
                    userCodeRepositoryContainer.isVisible = false
                }
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
        postUi { updateWalletsSlider() }
    }

    private fun updateWalletsSlider() {
        fun updateWalletInfoContainerVisibility(visibility: Int) {
            if (visibility == View.VISIBLE) flCardContainer.beginDelayedTransition()
            walletInfoContainer.visibility = visibility
        }
        sliderWallet.removeOnSliderTouchListener(sliderTouchListener)

        if (walletsCount == 0) {
            updateWalletInfoContainerVisibility(View.GONE)
            selectedIndexOfWallet = -1
            return
        }

        sliderWallet.post {
            if (walletsCount == 1) {
                selectedIndexOfWallet = 0
                updateWalletInfoContainerVisibility(View.VISIBLE)
                sliderWallet.value = 0f
                sliderWallet.valueTo = 1f
                sliderWallet.isEnabled = false
                updateWalletInfo()
                return@post
            }

            updateWalletInfoContainerVisibility(View.VISIBLE)
            sliderWallet.isEnabled = true
            sliderWallet.valueFrom = 0f
            sliderWallet.valueTo = walletsCount - 1f

            if (selectedIndexOfWallet == -1 || selectedIndexOfWallet >= walletsCount) {
                selectedIndexOfWallet = 0
            }
            sliderWallet.value = selectedIndexOfWallet.toFloat()
            sliderWallet.addOnSliderTouchListener(sliderTouchListener)
            updateWalletInfo()
        }
    }

    private fun updateWalletInfo() {
        tvWalletsCount.text = "$walletsCount"
        tvWalletIndex.text = "$selectedIndexOfWallet"
        tvWalletCurve.text = "${selectedWallet?.curve}"
        tvWalletPubKey.text = "${selectedWallet?.publicKey?.toHexString()}"
    }
}

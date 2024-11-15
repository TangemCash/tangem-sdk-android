package com.tangem.sdk

import android.app.Activity
import android.os.Build
import androidx.core.app.ComponentActivity
import com.tangem.Log
import com.tangem.Message
import com.tangem.SessionViewDelegate
import com.tangem.ViewDelegateMessage
import com.tangem.WrongValueType
import com.tangem.common.CardIdFormatter
import com.tangem.common.UserCodeType
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.Config
import com.tangem.common.core.ProductType
import com.tangem.common.core.TangemError
import com.tangem.common.extensions.VoidCallback
import com.tangem.operations.resetcode.ResetCodesViewDelegate
import com.tangem.sdk.extensions.sdkThemeContext
import com.tangem.sdk.nfc.NfcAntennaLocationProvider
import com.tangem.sdk.nfc.NfcManager
import com.tangem.sdk.ui.AttestationFailedDialog
import com.tangem.sdk.ui.NfcEnableDialog
import com.tangem.sdk.ui.NfcSessionDialog

/**
 * Default implementation of [SessionViewDelegate].
 * If no customisation is required, this is the preferred way to use Tangem SDK.
 */
class DefaultSessionViewDelegate(
    private val nfcManager: NfcManager,
    private val activity: ComponentActivity,
) : SessionViewDelegate {

    var sdkConfig: Config = Config()

    override val resetCodesViewDelegate: ResetCodesViewDelegate = AndroidResetCodesViewDelegate(activity)

    private var readingDialog: NfcSessionDialog? = null
    private var nfcEnableDialog: NfcEnableDialog? = null
    private var stoppedBySession: Boolean = false

    override fun onSessionStarted(
        cardId: String?,
        message: ViewDelegateMessage?,
        enableHowTo: Boolean,
        iconScanRes: Int?,
        productType: ProductType,
    ) {
        Log.view { "onSessionStarted" }
        createAndShowState(
            state = SessionViewDelegateState.Ready(formatCardId(cardId), productType),
            enableHowTo = enableHowTo,
            message = message,
            iconScanRes = iconScanRes,
        )
        checkNfcEnabled()
    }

    private fun checkNfcEnabled() {
        Log.view { "checkNfcEnabled" }
        // workaround to delay checking isNfcEnabled to let nfcManager become enabled state after enableReaderMode
        postUI(msTime = DIALOG_NFC_DELAY) {
            Log.view { "checkNfcEnabled isNfcEnabled=${nfcManager.isNfcEnabled}" }
            if (!nfcManager.isNfcEnabled) {
                nfcEnableDialog?.cancel()
                nfcEnableDialog = NfcEnableDialog()
                if (!activity.isFinishing && !activity.isDestroyed) {
                    Log.view { "checkNfcEnabled show dialog" }
                    activity.let { nfcEnableDialog?.show(it) }
                }
            }
        }
    }

    override fun onSessionStopped(message: Message?, onDialogHidden: () -> Unit) {
        Log.view { "session stopped" }
        stoppedBySession = true
        readingDialog?.show(SessionViewDelegateState.Success(message)) {
            onDialogShown()
            onDialogHidden()
        }
    }

    override fun onSecurityDelay(ms: Int, totalDurationSeconds: Int, productType: ProductType) {
        Log.view { "showing security delay: $ms, $totalDurationSeconds" }
        readingDialog?.show(
            SessionViewDelegateState.SecurityDelay(ms, totalDurationSeconds, productType),
            ::onDialogShown,
        )
    }

    override fun onDelay(total: Int, current: Int, step: Int, productType: ProductType) {
        Log.view { "showing delay" }
        readingDialog?.show(SessionViewDelegateState.Delay(total, current, step, productType), ::onDialogShown)
    }

    override fun onTagLost(productType: ProductType) {
        Log.view { "tag lost" }
        readingDialog?.show(SessionViewDelegateState.TagLost(productType), ::onDialogShown)
    }

    override fun onTagConnected() {
        Log.view { "tag connected" }
        readingDialog?.show(SessionViewDelegateState.TagConnected, ::onDialogShown)
    }

    override fun onWrongCard(wrongValueType: WrongValueType) {
        Log.view { "wrong card detected" }
        readingDialog?.show(SessionViewDelegateState.WrongCard(wrongValueType), ::onDialogShown)
    }

    override fun onError(error: TangemError) {
        readingDialog?.show(SessionViewDelegateState.Error(error), ::onDialogShown)
    }

    override fun requestUserCode(
        type: UserCodeType,
        isFirstAttempt: Boolean,
        showForgotButton: Boolean,
        cardId: String?,
        callback: CompletionCallback<String>,
    ) {
        Log.view { "showing pin request with type: $type" }
        postUI(msTime = 200) {
            Log.view { "activity lifecycle: ${activity.lifecycle.currentState}" }
            Log.view { "readingDialog: $readingDialog" }
            val dialog = readingDialog ?: createReadingDialog(activity)

            dialog.show(
                SessionViewDelegateState.PinRequested(
                    type = type,
                    isFirstAttempt = isFirstAttempt,
                    showForgotButton = showForgotButton,
                    cardId = cardId,
                    callback = callback,
                ),
                ::onDialogShown,
            )
        }
    }

    override fun requestUserCodeChange(type: UserCodeType, cardId: String?, callback: CompletionCallback<String>) {
        Log.view { "showing pin change request with type: $type" }
        val dialog = readingDialog ?: createReadingDialog(activity)

        dialog.show(SessionViewDelegateState.PinChangeRequested(type, cardId, callback), ::onDialogShown)
    }

    override fun dismiss() {
        readingDialog?.dismissInternal()
    }

    override fun setConfig(config: Config) {
        sdkConfig = config
    }

    override fun setMessage(message: ViewDelegateMessage?) {
        Log.view { "set message with header: ${message?.header}, and body: ${message?.body}" }
        postUI { readingDialog?.setMessage(message) }
    }

    override fun attestationDidFail(isDevCard: Boolean, positive: VoidCallback, negative: VoidCallback) {
        AttestationFailedDialog.didFail(activity, isDevCard, positive) {
            negative()
            dismiss()
        }
    }

    override fun attestationCompletedOffline(positive: VoidCallback, negative: VoidCallback, retry: VoidCallback) {
        AttestationFailedDialog.completedOffline(
            context = activity,
            positive = positive,
            negative = {
                negative()
                dismiss()
            },
            retry = retry,
        )
    }

    override fun attestationCompletedWithWarnings(positive: VoidCallback) {
        AttestationFailedDialog.completedWithWarnings(activity, positive)
    }

    private fun onDialogShown() {
        readingDialog = null
    }

    private fun createAndShowState(
        state: SessionViewDelegateState,
        enableHowTo: Boolean,
        message: ViewDelegateMessage? = null,
        iconScanRes: Int? = null,
    ) {
        Log.view { "createAndShowState" }
        postUI {
            if (readingDialog == null) {
                Log.view { "createAndConfigDialog" }
                createAndConfigureDialog(state, enableHowTo, message, iconScanRes)
            } else {
                readingDialog?.show(state, ::onDialogShown)
            }
        }
    }

    private fun createAndConfigureDialog(
        state: SessionViewDelegateState,
        enableHowTo: Boolean,
        message: ViewDelegateMessage? = null,
        iconScanRes: Int? = null,
    ) {
        with(createReadingDialog(activity, iconScanRes)) {
            Log.view { "createReadingDialog readingDialog $this" }
            showHowTo(enableHowTo)
            setInitialMessage(message)
            setScanImage(sdkConfig.scanTagImage)
            show(state, ::onDialogShown)
        }
    }

    private fun createReadingDialog(activity: Activity, iconScanRes: Int? = null): NfcSessionDialog {
        Log.view { "createReadingDialog" }
        return NfcSessionDialog(
            context = activity.sdkThemeContext(),
            nfcManager = nfcManager,
            nfcLocationProvider = NfcAntennaLocationProvider(Build.DEVICE),
            iconScanRes = iconScanRes,
        )
            .apply {
                setOwnerActivity(activity)
                dismissWithAnimation = true
                stoppedBySession = false
                create()
                setOnCancelListener {
                    if (!stoppedBySession) nfcManager.reader.stopSession(true)
                    readingDialog = null
                }
            }
            .also {
                readingDialog = it
            }
    }

    private fun formatCardId(cardId: String?): String? {
        cardId ?: return null

        val formatter = CardIdFormatter(sdkConfig.cardIdDisplayFormat)
        return formatter.getFormattedCardId(cardId)
    }

    companion object {
        private const val DIALOG_NFC_DELAY = 800L
    }
}

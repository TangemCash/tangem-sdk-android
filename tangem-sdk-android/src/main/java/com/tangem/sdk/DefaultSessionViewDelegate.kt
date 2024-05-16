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
    ) {
        Log.view { "session started" }
        createAndShowState(SessionViewDelegateState.Ready(formatCardId(cardId)), enableHowTo, message, iconScanRes)
        checkNfcEnabled()
    }

    private fun checkNfcEnabled() {
        postUI(msTime = 500) {
            if (!nfcManager.isNfcEnabled) {
                nfcEnableDialog?.cancel()
                nfcEnableDialog = NfcEnableDialog()
                if (!activity.isFinishing && !activity.isDestroyed) {
                    activity.let { nfcEnableDialog?.show(it) }
                }
            }
        }
    }

    override fun onSessionStopped(message: Message?) {
        Log.view { "session stopped" }
        stoppedBySession = true
        readingDialog?.show(SessionViewDelegateState.Success(message))
    }

    override fun onSecurityDelay(ms: Int, totalDurationSeconds: Int) {
        Log.view { "showing security delay: $ms, $totalDurationSeconds" }
        readingDialog?.show(SessionViewDelegateState.SecurityDelay(ms, totalDurationSeconds))
    }

    override fun onDelay(total: Int, current: Int, step: Int) {
        Log.view { "showing delay" }
        readingDialog?.show(SessionViewDelegateState.Delay(total, current, step))
    }

    override fun onTagLost() {
        Log.view { "tag lost" }
        readingDialog?.show(SessionViewDelegateState.TagLost)
    }

    override fun onTagConnected() {
        Log.view { "tag connected" }
        readingDialog?.show(SessionViewDelegateState.TagConnected)
    }

    override fun onWrongCard(wrongValueType: WrongValueType) {
        Log.view { "wrong card detected" }
        readingDialog?.show(SessionViewDelegateState.WrongCard(wrongValueType))
    }

    override fun onError(error: TangemError) {
        readingDialog?.show(SessionViewDelegateState.Error(error))
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
            if (readingDialog == null) createReadingDialog(activity)
            readingDialog?.show(
                SessionViewDelegateState.PinRequested(
                    type = type,
                    isFirstAttempt = isFirstAttempt,
                    showForgotButton = showForgotButton,
                    cardId = cardId,
                    callback = callback,
                ),
            )
        }
    }

    override fun requestUserCodeChange(type: UserCodeType, cardId: String?, callback: CompletionCallback<String>) {
        Log.view { "showing pin change request with type: $type" }
        if (readingDialog == null) createReadingDialog(activity)
        readingDialog?.show(SessionViewDelegateState.PinChangeRequested(type, cardId, callback))
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

    private fun createAndShowState(
        state: SessionViewDelegateState,
        enableHowTo: Boolean,
        message: ViewDelegateMessage? = null,
        iconScanRes: Int? = null,
    ) {
        postUI {
            if (readingDialog == null) {
                createReadingDialog(activity, iconScanRes)
            } else {
                readingDialog?.dismissInternal()
            }
            readingDialog?.showHowTo(enableHowTo)
            readingDialog?.setInitialMessage(message)
            readingDialog?.setScanImage(sdkConfig.scanTagImage)
            readingDialog?.show(state)
        }
    }

    private fun createReadingDialog(activity: Activity, iconScanRes: Int? = null) {
        val nfcLocationProvider = NfcAntennaLocationProvider(Build.DEVICE)
        readingDialog = NfcSessionDialog(
            context = activity.sdkThemeContext(),
            nfcManager = nfcManager,
            nfcLocationProvider = nfcLocationProvider,
            iconScanRes = iconScanRes,
        ).apply {
            setOwnerActivity(activity)
            dismissWithAnimation = true
            stoppedBySession = false
            create()
            setOnCancelListener {
                if (!stoppedBySession) nfcManager.reader.stopSession(true)
                readingDialog = null
            }
        }
    }

    private fun formatCardId(cardId: String?): String? {
        val cardId = cardId ?: return null

        val formatter = CardIdFormatter(sdkConfig.cardIdDisplayFormat)
        return formatter.getFormattedCardId(cardId)
    }
}

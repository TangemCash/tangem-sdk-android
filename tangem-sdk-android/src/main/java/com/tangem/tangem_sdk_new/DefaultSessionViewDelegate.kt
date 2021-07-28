package com.tangem.tangem_sdk_new

import android.app.Activity
import android.os.Build
import android.view.ContextThemeWrapper
import com.tangem.*
import com.tangem.common.UserCodeType
import com.tangem.common.core.Config
import com.tangem.common.core.TangemError
import com.tangem.common.extensions.VoidCallback
import com.tangem.common.nfc.CardReader
import com.tangem.tangem_sdk_new.nfc.NfcAntennaLocationProvider
import com.tangem.tangem_sdk_new.nfc.NfcManager
import com.tangem.tangem_sdk_new.ui.NfcSessionDialog
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Default implementation of [SessionViewDelegate].
 * If no customisation is required, this is the preferred way to use Tangem SDK.
 */
class DefaultSessionViewDelegate(
    private val nfcManager: NfcManager,
    private val reader: CardReader
) : SessionViewDelegate {

    var sdkConfig: Config? = null

    lateinit var activity: Activity
    private var readingDialog: NfcSessionDialog? = null
    private var stoppedBySession: Boolean = false

    override fun onSessionStarted(cardId: String?, message: Message?, enableHowTo: Boolean) {
        Log.view { "Session started" }
        postUI {
            if (readingDialog == null) createReadingDialog(activity)
            readingDialog?.enableHowTo(enableHowTo)
            readingDialog?.setMessage(message)
            readingDialog?.show(SessionViewDelegateState.Ready(formatCardId(cardId), message))
        }
    }

    private fun createReadingDialog(activity: Activity) {
        val nfcLocationProvider = NfcAntennaLocationProvider(Build.DEVICE)
        val themeWrapper = ContextThemeWrapper(activity, R.style.CardSdkTheme)
        readingDialog = NfcSessionDialog(themeWrapper, nfcManager, nfcLocationProvider).apply {
            setOwnerActivity(activity)
            dismissWithAnimation = true
            stoppedBySession = false
            create()
            setOnCancelListener {
                if (!stoppedBySession) reader.stopSession(true)
                createReadingDialog(activity)
            }
        }
    }

    override fun onSessionStopped(message: Message?) {
        Log.view { "Session stopped" }
        stoppedBySession = true
        postUI { readingDialog?.show(SessionViewDelegateState.Success(message)) }
    }

    override fun onSecurityDelay(ms: Int, totalDurationSeconds: Int) {
        Log.view { "Showing security delay: $ms, $totalDurationSeconds" }
        postUI {
            readingDialog?.show(SessionViewDelegateState.SecurityDelay(ms, totalDurationSeconds))
        }
    }

    override fun onDelay(total: Int, current: Int, step: Int) {
        Log.view { "Showing delay" }
        postUI {
            readingDialog?.show(SessionViewDelegateState.Delay(total, current, step))
        }
    }

    override fun onTagLost() {
        Log.view { "Tag lost" }
        postUI { readingDialog?.show(SessionViewDelegateState.TagLost) }
    }

    override fun onTagConnected() {
        Log.view { "Tag connected" }
        postUI { readingDialog?.show(SessionViewDelegateState.TagConnected) }
    }

    override fun onWrongCard(wrongValueType: WrongValueType) {
        Log.view { "Wrong card detected" }
        postUI { readingDialog?.show(SessionViewDelegateState.WrongCard(wrongValueType)) }
    }

    override fun onError(error: TangemError) {
        postUI { readingDialog?.show(SessionViewDelegateState.Error(error)) }
    }

    override fun requestUserCode(type: UserCodeType, isFirstAttempt: Boolean, callback: (pin: String) -> Unit) {
        Log.view { "Showing pin request with type: $type" }
        postUI { readingDialog?.show(SessionViewDelegateState.PinRequested(type, isFirstAttempt, callback)) }
    }

    override fun requestUserCodeChange(type: UserCodeType, callback: (pin: String) -> Unit) {
        Log.view { "Showing pin change request with type: $type" }
        postUI {
            if (readingDialog == null) {
                createReadingDialog(activity)
            }
            readingDialog?.enableHowTo(false)
            readingDialog?.show(SessionViewDelegateState.PinChangeRequested(type, callback))
        }
    }

    override fun setConfig(config: Config) {
        sdkConfig = config
    }

    override fun setMessage(message: Message?) {
        Log.view { "Set message with header: ${message?.header}, and body: ${message?.body}" }
        readingDialog?.setMessage(message)
    }

    override fun dismiss() {
        postUI { readingDialog?.dismiss() }
    }

    override fun attestationDidFail(positive: VoidCallback, negative: VoidCallback) {}

    override fun attestationCompletedOffline(positive: VoidCallback, negative: VoidCallback, retry: VoidCallback) {
//        title: "Online attestation failed",
//        message: "We cannot finish card's online attestation at this time. You can continue at your own risk and try again later, retry now or cancel the operation
    }

    override fun attestationCompletedWithWarnings(neutral: VoidCallback) {}

    private fun formatCardId(cardId: String?): String? {
        val cardId = cardId ?: return null
        val displayedNumbersCount = sdkConfig?.cardIdDisplayedNumbersCount ?: return cardId

        return cardId.dropLast(1).takeLast(displayedNumbersCount)
    }

    companion object {
        fun createLogger(): TangemSdkLogger {
            return object : TangemSdkLogger {
                private val tag = "TangemSdkLogger"
                private val dateFormatter: DateFormat = SimpleDateFormat("HH:mm:ss:SSS", Locale.getDefault())

                override fun log(message: () -> String, level: Log.Level) {
                    if (!Log.Config.Verbose.levels.contains(level)) return

                    val prefixDelimiter = if (level.prefix.isEmpty()) "" else ": "
                    val logMessage = "${dateFormatter.format(Date())}: ${level.prefix}$prefixDelimiter${message()}"
                    when (level) {
                        Log.Level.Debug -> android.util.Log.d(tag, logMessage)
                        Log.Level.Warning -> android.util.Log.w(tag, logMessage)
                        Log.Level.Error -> android.util.Log.e(tag, logMessage)
                        else -> android.util.Log.v(tag, logMessage)
                    }
                }
            }
        }
    }
}
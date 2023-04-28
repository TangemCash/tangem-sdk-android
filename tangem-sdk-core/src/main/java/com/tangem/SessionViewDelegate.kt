package com.tangem

import com.squareup.moshi.JsonClass
import com.tangem.common.StringsLocator
import com.tangem.common.UserCodeType
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.Config
import com.tangem.common.core.TangemError
import com.tangem.common.extensions.VoidCallback
import com.tangem.operations.resetcode.ResetCodesViewDelegate

/**
 * Allows interaction with users and shows visual elements.
 *
 * Its default implementation, DefaultCardManagerDelegate, is in our tangem-sdk module.
 */
interface SessionViewDelegate {

    val resetCodesViewDelegate: ResetCodesViewDelegate

    /**
     * It is called when user is expected to scan a Tangem Card with an Android device.
     */
    fun onSessionStarted(cardId: String?, message: ViewDelegateMessage? = null, enableHowTo: Boolean)

    /**
     * It is called when security delay is triggered by the card.
     * A user is expected to hold the card until the security delay is over.
     */
    fun onSecurityDelay(ms: Int, totalDurationSeconds: Int)

    /**
     * It is called when long tasks are performed.
     * A user is expected to hold the card until the task is complete.
     */
    fun onDelay(total: Int, current: Int, step: Int)

    /**
     * It is called when user takes the card away from the Android device during the scanning
     * (for example when security delay is in progress) and the TagLostException is received.
     */
    fun onTagLost()

    fun onTagConnected()

    fun onWrongCard(wrongValueType: WrongValueType)

    /**
     * It is called when NFC session was completed and a user can take the card away from the Android device.
     */
    fun onSessionStopped(message: Message? = null)

    /**
     * It is called when some error occur during NFC session.
     */
    fun onError(error: TangemError)

    /**
     * It is called when a user is expected to enter pin code.
     */
    fun requestUserCode(
        type: UserCodeType,
        isFirstAttempt: Boolean,
        showForgotButton: Boolean,
        cardId: String?,
        callback: CompletionCallback<String>,
    )

    /**
     * It is called when a user wants to change pin code.
     */
    fun requestUserCodeChange(type: UserCodeType, cardId: String?, callback: CompletionCallback<String>)

    fun setConfig(config: Config)

    fun setMessage(message: ViewDelegateMessage?)

    fun dismiss()

    fun attestationDidFail(isDevCard: Boolean, positive: VoidCallback, negative: VoidCallback)

    fun attestationCompletedOffline(positive: VoidCallback, negative: VoidCallback, retry: VoidCallback)

    fun attestationCompletedWithWarnings(positive: VoidCallback)
}

/**
 * Wrapper for a message that can be shown to the user after a NFC session has started.
 */
interface ViewDelegateMessage {
    val header: String?
    val body: String?
}

@JsonClass(generateAdapter = true)
data class Message(
    override val header: String? = null,
    override val body: String? = null,
) : ViewDelegateMessage

data class LocatorMessage(
    private val headerSource: Source? = null,
    private val bodySource: Source? = null,
) : ViewDelegateMessage {

    override val header: String?
        get() = convertedHeader
    override val body: String?
        get() = convertedBody

    private var convertedHeader: String? = null
    private var convertedBody: String? = null

    fun fetchMessages(locator: StringsLocator) {
        convertedHeader = fetchString(headerSource, locator)
        convertedBody = fetchString(bodySource, locator)
    }

    private fun fetchString(message: Source?, locator: StringsLocator): String? {
        return message?.let { locator.getString(it.id, *it.formatArgs) }
    }

    class Source(
        val id: StringsLocator.ID,
        vararg val formatArgs: Any = emptyArray(),
    )
}

enum class WrongValueType { CardId, CardType }

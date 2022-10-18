package com.tangem

import com.squareup.moshi.JsonClass
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
    fun onSessionStarted(cardId: String?, message: Message? = null, enableHowTo: Boolean)

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
        type: UserCodeType, isFirstAttempt: Boolean,
        showForgotButton: Boolean,
        cardId: String?,
        callback: CompletionCallback<String>
    )

    /**
     * It is called when a user wants to change pin code.
     */
    fun requestUserCodeChange(type: UserCodeType, cardId: String?, callback: CompletionCallback<String>)

    fun setConfig(config: Config)

    fun setMessage(message: Message?)

    fun dismiss()

    fun attestationDidFail(isDevCard: Boolean, positive: VoidCallback, negative: VoidCallback)

    fun attestationCompletedOffline(
        positive: VoidCallback,
        negative: VoidCallback,
        retry: VoidCallback
    )

    fun attestationCompletedWithWarnings(positive: VoidCallback)
}

/**
 * Wrapper for a message that can be shown to user after a start of NFC session.
 */
@JsonClass(generateAdapter = true)
data class Message(val header: String? = null, val body: String? = null)

enum class WrongValueType { CardId, CardType }
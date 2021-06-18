package com.tangem

import com.squareup.moshi.JsonClass
import com.tangem.commands.PinType

/**
 * Allows interaction with users and shows visual elements.
 *
 * Its default implementation, DefaultCardManagerDelegate, is in our tangem-sdk module.
 */
interface SessionViewDelegate {

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
    fun onPinRequested(pinType: PinType, isFirstAttempt: Boolean, callback: (pin: String) -> Unit)

    /**
     * It is called when a user wants to change pin code.
     */
    fun onPinChangeRequested(pinType: PinType, callback: (pin: String) -> Unit)

    fun setConfig(config: Config)

    fun setMessage(message: Message?)

    fun dismiss()
}

/**
 * Wrapper for a message that can be shown to user after a start of NFC session.
 */
@JsonClass(generateAdapter = true)
data class Message(val header: String? = null, val body: String? = null)

enum class WrongValueType {
    CardId, CardType;

    companion object {
        fun fromTangemError(error: TangemError): WrongValueType? {
            return when (error) {
                is TangemSdkError.WrongCardType -> CardType
                is TangemSdkError.WrongCardNumber -> CardId
                else -> null
            }
        }
    }
}
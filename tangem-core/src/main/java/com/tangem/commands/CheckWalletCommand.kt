package com.tangem.commands

import com.tangem.CardSession
import com.tangem.SessionEnvironment
import com.tangem.TangemSdkError
import com.tangem.commands.common.card.Card
import com.tangem.commands.common.card.CardStatus
import com.tangem.commands.common.card.EllipticCurve
import com.tangem.common.CompletionResult
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag
import com.tangem.crypto.CryptoUtils

/**
 * Deserialized response from the Tangem card after [CheckWalletCommand].
 *
 * @property cardId Unique Tangem card ID number
 * @property salt Random salt generated by the card.
 * @property walletSignature Challenge and salt signed with the wallet private key.
 */
class CheckWalletResponse(
        val cardId: String,
        val salt: ByteArray,
        val walletSignature: ByteArray
) : CommandResponse {

    fun verify(curve: EllipticCurve, publicKey: ByteArray, challenge: ByteArray): Boolean {
        return CryptoUtils.verify(
                publicKey,
                challenge + salt,
                walletSignature,
                curve)
    }
}

/**
 * This command proves that the wallet private key from the card corresponds to the wallet public key.
 * Standard challenge/response scheme is used.
 *
 * @property walletPointer Pointer to wallet for interaction (works only on COS v.4.0 and higher. For previous version pointer will be ignored).
 * @property challenge Random challenge generated by application
 */
class CheckWalletCommand(
    private val curve: EllipticCurve,
    private val publicKey: ByteArray,
    override var walletPointer: WalletPointer?
) : Command<CheckWalletResponse>(), WalletPointable {

    private val challenge = CryptoUtils.generateRandomBytes(16)

    override fun run(session: CardSession, callback: (result: CompletionResult<CheckWalletResponse>) -> Unit) {
        super.run(session) { result ->
            when (result) {
                is CompletionResult.Failure -> {
                    callback(CompletionResult.Failure(result.error))
                }
                is CompletionResult.Success -> {
                    val verified = result.data.verify(
                            curve,
                            publicKey,
                            challenge
                    )
                    if (verified) {
                        callback(CompletionResult.Success(result.data))
                    } else {
                        callback(CompletionResult.Failure(TangemSdkError.VerificationFailed()))
                    }
                }
            }
        }
    }

    override fun performPreCheck(card: Card): TangemSdkError? {
        if (card.status == CardStatus.NotPersonalized) {
            return TangemSdkError.NotPersonalized()
        }
        if (card.isActivated) {
            return TangemSdkError.NotActivated()
        }
        return null
    }

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.Pin, environment.pin1?.value)
        tlvBuilder.append(TlvTag.CardId, environment.card?.cardId)
        tlvBuilder.append(TlvTag.Challenge, challenge)
        walletPointer?.addTlvData(tlvBuilder)
        return CommandApdu(Instruction.CheckWallet, tlvBuilder.serialize())
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): CheckWalletResponse {
        val tlvData = apdu.getTlvData() ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)
        return CheckWalletResponse(
                cardId = decoder.decode(TlvTag.CardId),
                salt = decoder.decode(TlvTag.Salt),
                walletSignature = decoder.decode(TlvTag.Signature)
        )
    }
}
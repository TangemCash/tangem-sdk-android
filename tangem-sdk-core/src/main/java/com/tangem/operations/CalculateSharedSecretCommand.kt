package com.tangem.operations

import com.squareup.moshi.JsonClass
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.card.Card
import com.tangem.common.core.SessionEnvironment
import com.tangem.common.core.TangemSdkError
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag
import com.tangem.operations.read.WalletPointer
import com.tangem.operations.sign.SignCommand

@JsonClass(generateAdapter = true)
class CalculateSharedSecretResponse(
    val cardId: String,
    val walletSharedSecret: ByteArray,
) : CommandResponse


class CalculateSharedSecretCommand(
    private val sessionKeyA: ByteArray,
    private val walletPointer: WalletPointer,
) : Command<CalculateSharedSecretResponse>() {

    override fun preflightReadMode(): PreflightReadMode =
        PreflightReadMode.ReadWallet(walletPointer)

    override fun requiresPasscode(): Boolean = true


    private var environment: SessionEnvironment? = null

    override fun performPreCheck(card: Card): TangemSdkError? {
        val wallet = card.wallets.firstOrNull() ?: return TangemSdkError.WalletNotFound()

        return null
    }


    /**
     * Application can optionally submit a public key Terminal_PublicKey in [SignCommand].
     * Submitted key is stored by the Tangem card if it differs from a previous submitted Terminal_PublicKey.
     * The Tangem card will not enforce security delay if [SignCommand] will be called with
     * TerminalTransactionSignature parameter containing a correct signature of raw data to be signed made with
     * TerminalPrivateKey (this key should be generated and security stored by the application).
     */
    override fun serialize(environment: SessionEnvironment): CommandApdu {

        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.Pin, environment.accessCode.value)
        tlvBuilder.append(TlvTag.Pin2, environment.passcode.value)
        tlvBuilder.append(TlvTag.CardId, environment.card?.cardId)
        tlvBuilder.append(TlvTag.SessionKeyA, sessionKeyA)
        when (walletPointer) {
            is WalletPointer.WalletIndex ->
                tlvBuilder.append(TlvTag.WalletIndex, walletPointer.index)
            is WalletPointer.WalletPublicKey ->
                tlvBuilder.append(TlvTag.WalletPublicKey, walletPointer.publicKey)
        }
        tlvBuilder.append(TlvTag.WalletHdPath, walletPointer.walletHdPath)
        tlvBuilder.append(TlvTag.WalletTweak, walletPointer.walletTweak)

        return CommandApdu(Instruction.CalculateSharedSecret, tlvBuilder.serialize())
    }

    override fun deserialize(
        environment: SessionEnvironment,
        apdu: ResponseApdu,
    ): CalculateSharedSecretResponse {
        val tlvData = deserializeApdu(environment, apdu)
        val decoder = TlvDecoder(tlvData)

        return CalculateSharedSecretResponse(
            decoder.decode(TlvTag.CardId),
            decoder.decode(TlvTag.WalletSharedSecret)
        )
    }
}
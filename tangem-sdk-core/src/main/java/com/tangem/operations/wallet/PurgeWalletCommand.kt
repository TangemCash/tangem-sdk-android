package com.tangem.operations.wallet

import com.tangem.common.CompletionResult
import com.tangem.common.SuccessResponse
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.card.Card
import com.tangem.common.core.CardSession
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.SessionEnvironment
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.guard
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag
import com.tangem.operations.Command
import com.tangem.operations.PreflightReadMode

/**
 * This command deletes all wallet data and its private and public keys
 * @property walletPublicKey: Public key of the wallet to delete
 */
class PurgeWalletCommand(
    private val walletPublicKey: ByteArray
) : Command<SuccessResponse>() {

    override fun preflightReadMode(): PreflightReadMode = PreflightReadMode.ReadWallet(walletPublicKey)

    override fun requiresPasscode(): Boolean = true

    override fun performPreCheck(card: Card): TangemSdkError? {
        val wallet = card.wallet(walletPublicKey)

        return when {
            wallet == null -> TangemSdkError.WalletNotFound()
            wallet.settings.isPermanent -> TangemSdkError.PurgeWalletProhibited()
            else -> null
        }
    }

    override fun run(session: CardSession, callback: CompletionCallback<SuccessResponse>) {
        super.run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    val card = session.environment.card.guard {
                        callback(CompletionResult.Failure(TangemSdkError.CardError()))
                        return@run
                    }
                    session.environment.card = card.removeWallet(walletPublicKey)
                    callback(CompletionResult.Success(result.data))
                }
                is CompletionResult.Failure -> callback(result)
            }
        }
    }

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.Pin, environment.accessCode.value)
        tlvBuilder.append(TlvTag.Pin2, environment.passcode.value)
        tlvBuilder.append(TlvTag.CardId, environment.card?.cardId)
        tlvBuilder.append(TlvTag.WalletPublicKey, walletPublicKey)

        return CommandApdu(Instruction.PurgeWallet, tlvBuilder.serialize())
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): SuccessResponse {
        val tlvData = apdu.getTlvData(environment.encryptionKey) ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)
        return SuccessResponse(decoder.decode(TlvTag.CardId))
    }
}
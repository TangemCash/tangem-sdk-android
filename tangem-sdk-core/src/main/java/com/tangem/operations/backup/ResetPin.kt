package com.tangem.operations.backup

import com.tangem.common.CompletionResult
import com.tangem.common.SuccessResponse
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.backup.ResetPinSession
import com.tangem.common.card.Card
import com.tangem.common.core.CardSession
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.SessionEnvironment
import com.tangem.common.core.TangemSdkError
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag
import com.tangem.operations.Command
import com.tangem.operations.PreflightReadMode

/**
 */
class ResetPinCommand(
    private val resetPinSession: ResetPinSession
) : Command<SuccessResponse>() {

    override fun requiresPasscode(): Boolean = false
    override fun preflightReadMode(): PreflightReadMode {
        return PreflightReadMode.ReadCardOnly
    }

    override fun performPreCheck(card: Card): TangemSdkError? {
        if (card.backupStatus!=Card.BackupStatus.Active) {
            return TangemSdkError.InvalidState()
        }
        return null
    }

    override fun run(session: CardSession, callback: CompletionCallback<SuccessResponse>) {
        val card = session.environment.card ?: throw TangemSdkError.MissingPreflightRead()

        super.run(session) { result ->
            when (result) {
                is CompletionResult.Success -> callback(CompletionResult.Success(result.data))
                is CompletionResult.Failure -> callback(result)
            }
        }
    }

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.CardId, environment.card?.cardId)
        tlvBuilder.append(TlvTag.NewPin, resetPinSession.newPIN)
        tlvBuilder.append(TlvTag.NewPin2, resetPinSession.newPIN2)
        tlvBuilder.append(TlvTag.Cvc, environment.cvc)

        return CommandApdu(Instruction.SetPin, tlvBuilder.serialize())
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): SuccessResponse {
        val tlvData = apdu.getTlvData(environment.encryptionKey) ?: throw TangemSdkError.DeserializeApduFailed()
        val decoder = TlvDecoder(tlvData)
        return SuccessResponse(decoder.decode(TlvTag.CardId))
    }
}


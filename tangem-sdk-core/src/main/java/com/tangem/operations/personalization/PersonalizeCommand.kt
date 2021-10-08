package com.tangem.operations.personalization

import com.tangem.Log
import com.tangem.common.CompletionResult
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.card.Card
import com.tangem.common.card.EncryptionMode
import com.tangem.common.core.CardSession
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.SessionEnvironment
import com.tangem.common.core.TangemSdkError
import com.tangem.common.deserialization.CardDeserializer
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvTag
import com.tangem.crypto.sign
import com.tangem.operations.Command
import com.tangem.operations.PreflightReadMode
import com.tangem.operations.PreflightReadTask
import com.tangem.operations.personalization.entities.*

/**
 * Command available on SDK cards only
 *
 * Personalization is an initialization procedure, required before starting using a card.
 * During this procedure a card setting is set up.
 * During this procedure all data exchange is encrypted.
 * @property config is a configuration file with all the card settings that are written on the card
 * during personalization.
 * @property issuer Issuer is a third-party team or company wishing to use Tangem cards.
 * @property manufacturer Tangem Card Manufacturer.
 * @property acquirer Acquirer is a trusted third-party company that operates proprietary
 * (non-EMV) POS terminal infrastructure and transaction processing back-end.
 */
class PersonalizeCommand(
    private val config: CardConfig,
    private val issuer: Issuer,
    private val manufacturer: Manufacturer,
    private val acquirer: Acquirer? = null
) : Command<Card>() {

    override fun preflightReadMode(): PreflightReadMode = PreflightReadMode.None

    override fun run(session: CardSession, callback: CompletionCallback<Card>) {
        Log.command(this)
        //We have to run preflight read ourselves to catch the notPersonalized error
        val read = PreflightReadTask(PreflightReadMode.ReadCardOnly, null)
        read.run(session) { result ->
            when (result) {
                is CompletionResult.Success -> callback(CompletionResult.Failure(TangemSdkError.AlreadyPersonalized()))
                is CompletionResult.Failure -> {
                    if (result.error is TangemSdkError.NotPersonalized) {
                        runPersonalize(session, callback)
                    } else {
                        callback(result)
                    }
                }
            }
        }
    }

    private fun runPersonalize(session: CardSession, callback: CompletionCallback<Card>) {
        val encryptionMode = session.environment.encryptionMode
        val encryptionKey = session.environment.encryptionKey
        session.environment.encryptionMode = EncryptionMode.None
        session.environment.encryptionKey = devPersonalizationKey
        super.run(session) { result ->
            session.environment.encryptionMode = encryptionMode
            session.environment.encryptionKey = encryptionKey
            callback(result)
        }
    }

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        return CommandApdu(Instruction.Personalize, serializePersonalizationData(config))
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): Card {
        val decoder = CardDeserializer.getDecoder(environment, apdu)
        val cardDataDecoder = CardDeserializer.getCardDataDecoder(environment, decoder.tlvList)
        return CardDeserializer.deserialize(decoder, cardDataDecoder, true)
    }

    private fun serializePersonalizationData(config: CardConfig): ByteArray {
        val cardId = config.createCardId() ?: throw TangemSdkError.SerializeCommandError()

        val cardData = config.cardData.createPersonalizationCardData()
        val createWallet = config.createWallet != 0

        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.CardId, cardId)
        tlvBuilder.append(TlvTag.CurveId, config.curveID)
        tlvBuilder.append(TlvTag.MaxSignatures, config.maxSignatures ?: Int.MAX_VALUE)
        tlvBuilder.append(TlvTag.SigningMethod, config.signingMethod)
        tlvBuilder.append(TlvTag.SettingsMask, config.createSettingsMask())
        tlvBuilder.append(TlvTag.PauseBeforePin2, config.pauseBeforePin2 / 10)
        tlvBuilder.append(TlvTag.Cvc, config.cvc.toByteArray())
        tlvBuilder.append(TlvTag.NdefData, serializeNdef(config))
        tlvBuilder.append(TlvTag.CreateWalletAtPersonalize, createWallet)
        tlvBuilder.append(TlvTag.WalletsCount, config.walletsCount)
        tlvBuilder.append(TlvTag.NewPin, config.pinSha256())
        tlvBuilder.append(TlvTag.NewPin2, config.pin2Sha256())
        tlvBuilder.append(TlvTag.NewPin3, config.pin3Sha256())
        tlvBuilder.append(TlvTag.CrExKey, config.hexCrExKey)
        tlvBuilder.append(TlvTag.IssuerPublicKey, issuer.dataKeyPair.publicKey)
        tlvBuilder.append(TlvTag.IssuerTransactionPublicKey, issuer.transactionKeyPair.publicKey)
        tlvBuilder.append(TlvTag.AcquirerPublicKey, acquirer?.keyPair?.publicKey)
        tlvBuilder.append(TlvTag.CardData, serializeCardData(cardData, cardId))

        return tlvBuilder.serialize()
    }

    private fun serializeNdef(config: CardConfig): ByteArray {
        return if (config.ndefRecords.isEmpty()) {
            ByteArray(0)
        } else {
            NdefEncoder(config.ndefRecords, config.useDynamicNDEF == true).encode()
        }
    }

    private fun serializeCardData(cardData: CardData, cardId: String): ByteArray {
        val signature = cardId.toByteArray().sign(manufacturer.keyPair.privateKey)

        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.BatchId, cardData.batchId)
        tlvBuilder.append(TlvTag.ProductMask, cardData.productMask)
        tlvBuilder.append(TlvTag.ManufactureDateTime, cardData.manufactureDateTime)
        tlvBuilder.append(TlvTag.IssuerName, issuer.id)
        tlvBuilder.append(TlvTag.BlockchainName, cardData.blockchainName)
        tlvBuilder.append(TlvTag.CardIdManufacturerSignature, signature)

        if (cardData.tokenSymbol != null) {
            tlvBuilder.append(TlvTag.TokenSymbol, cardData.tokenSymbol)
            tlvBuilder.append(TlvTag.TokenContractAddress, cardData.tokenContractAddress)
            tlvBuilder.append(TlvTag.TokenDecimal, cardData.tokenDecimal)
        }

        return tlvBuilder.serialize()
    }

    companion object {
        val devPersonalizationKey = "1234".calculateSha256().copyOf(32)
    }
}
package com.tangem.commands.personalization

import com.tangem.CardSession
import com.tangem.EncryptionMode
import com.tangem.SessionEnvironment
import com.tangem.TangemSdkError
import com.tangem.commands.Command
import com.tangem.commands.common.card.Card
import com.tangem.commands.common.card.CardData
import com.tangem.commands.common.card.CardDeserializer
import com.tangem.commands.common.card.CardStatus
import com.tangem.commands.personalization.entities.*
import com.tangem.common.CompletionResult
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvTag
import com.tangem.crypto.sign

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
        private val issuer: Issuer, private val manufacturer: Manufacturer,
        private val acquirer: Acquirer? = null
) : Command<Card>() {

    override fun run(session: CardSession, callback: (result: CompletionResult<Card>) -> Unit) {
        val encryptionMode = session.environment.encryptionMode
        val encryptionKey = session.environment.encryptionKey
        session.environment.encryptionMode = EncryptionMode.NONE
        session.environment.encryptionKey = devPersonalizationKey
        super.run(session) { result ->
            session.environment.encryptionMode = encryptionMode
            session.environment.encryptionKey = encryptionKey
            callback(result)
        }
    }

    override fun performPreCheck(card: Card): TangemSdkError? {
        if (card.status != CardStatus.NotPersonalized) {
            return TangemSdkError.AlreadyPersonalized()
        }
        return null
    }

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        return CommandApdu(Instruction.Personalize, serializePersonalizationData(config))
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): Card {
        return CardDeserializer.deserialize(apdu)
    }

    private fun serializePersonalizationData(config: CardConfig): ByteArray {
        val cardId = config.createCardId() ?: throw TangemSdkError.SerializeCommandError()

        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.CardId, cardId)
        tlvBuilder.append(TlvTag.CurveId, config.curveID)
        tlvBuilder.append(TlvTag.MaxSignatures, config.maxSignatures)
        tlvBuilder.append(TlvTag.SigningMethod, config.signingMethods)
        tlvBuilder.append(TlvTag.SettingsMask, config.createSettingsMask())
        tlvBuilder.append(TlvTag.PauseBeforePin2, config.pauseBeforePin2 / 10)
        tlvBuilder.append(TlvTag.Cvc, config.cvc.toByteArray())
        if (!config.ndefRecords.isNullOrEmpty())
            tlvBuilder.append(TlvTag.NdefData, serializeNdef(config))

        tlvBuilder.append(TlvTag.CreateWalletAtPersonalize, config.createWallet)
        tlvBuilder.append(TlvTag.WalletsCount, config.walletsCount)

        tlvBuilder.append(TlvTag.NewPin, config.pin)
        tlvBuilder.append(TlvTag.NewPin2, config.pin2)
        tlvBuilder.append(TlvTag.NewPin3, config.pin3)
        tlvBuilder.append(TlvTag.CrExKey, config.hexCrExKey)
        tlvBuilder.append(TlvTag.IssuerDataPublicKey, issuer.dataKeyPair.publicKey)
        tlvBuilder.append(TlvTag.IssuerTransactionPublicKey, issuer.transactionKeyPair.publicKey)

        tlvBuilder.append(TlvTag.AcquirerPublicKey, acquirer?.keyPair?.publicKey)

        tlvBuilder.append(TlvTag.CardData, serializeCardData(cardId, config.cardData))
        return tlvBuilder.serialize()
    }

    private fun serializeNdef(config: CardConfig): ByteArray {
        return NdefEncoder(config.ndefRecords, config.useDynamicNDEF).encode()
    }

    private fun serializeCardData(cardId: String, cardData: CardData): ByteArray {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.Batch, cardData.batchId)
        tlvBuilder.append(TlvTag.ProductMask, cardData.productMask)
        tlvBuilder.append(TlvTag.ManufactureDateTime, cardData.manufactureDateTime)
        tlvBuilder.append(TlvTag.IssuerId, issuer.id)
        tlvBuilder.append(TlvTag.BlockchainId, cardData.blockchainName)

        if (cardData.tokenSymbol != null) {
            tlvBuilder.append(TlvTag.TokenSymbol, cardData.tokenSymbol)
            tlvBuilder.append(TlvTag.TokenContractAddress, cardData.tokenContractAddress)
            tlvBuilder.append(TlvTag.TokenDecimal, cardData.tokenDecimal)
        }
        tlvBuilder.append(
            TlvTag.CardIdManufacturerSignature,
            cardId.hexToBytes().sign(manufacturer.keyPair.privateKey)
        )
        return tlvBuilder.serialize()
    }

    companion object {
        val devPersonalizationKey = "1234".calculateSha256().copyOf(32)
    }
}
package com.tangem.operations.wallet

import com.squareup.moshi.JsonClass
import com.tangem.common.CompletionResult
import com.tangem.common.MaskBuilder
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.card.*
import com.tangem.common.core.*
import com.tangem.common.extensions.guard
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag
import com.tangem.operations.Command
import com.tangem.operations.CommandResponse

/**
 * Response from the Tangem card after `CreateWalletCommand`.
 */
@JsonClass(generateAdapter = true)
class CreateWalletResponse(
    /**
     * Unique Tangem card ID number
     */
    val cardId: String,
    /**
     * Created wallet
     */
    val wallet: CardWallet,
) : CommandResponse

/**
 * This command will create a new wallet on the card having ‘Empty’ state.
 * A key pair WalletPublicKey / WalletPrivateKey is generated and securely stored in the card.
 * App will need to obtain Wallet_PublicKey from the response of `CreateWalletCommand`or `ReadCommand`
 * and then transform it into an address of corresponding blockchain wallet
 * according to a specific blockchain algorithm.
 * WalletPrivateKey is never revealed by the card and will be used by `SignCommand` and `AttestWalletKeyCommand`.
 * RemainingSignature is set to MaxSignatures.
 *
 *  @property curve: Elliptic curve of the wallet
 */
class CreateWalletCommand(
    private val curve: EllipticCurve
) : Command<CreateWalletResponse>() {

    private val signingMethod = SigningMethod.build(SigningMethod.Code.SignHash)
    private var walletIndex: Int? = null

    override fun requiresPasscode(): Boolean = true

    override fun performPreCheck(card: Card): TangemSdkError? {
        if (card.firmwareVersion >= FirmwareVersion.MultiWalletAvailable && !card.settings.isSelectBlockchainAllowed) {
            return TangemSdkError.WalletCannotBeCreated()
        }
        if (!card.supportedCurves.contains(curve)) {
            return TangemSdkError.UnsupportedCurve()
        }
        if (card.firmwareVersion < FirmwareVersion.MultiWalletAvailable) {
            card.settings.defaultSigningMethods?.let {
                if (!it.toList().containsAll(signingMethod.toList())) {
                    return TangemSdkError.UnsupportedWalletConfig()
                }
            }
        }

        return null
    }

    override fun run(session: CardSession, callback: CompletionCallback<CreateWalletResponse>) {
        super.run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    session.environment.card = session.environment.card?.addWallet(result.data.wallet)
                    callback(CompletionResult.Success(result.data))
                }
                is CompletionResult.Failure -> callback(result)
            }
        }
    }

    override fun mapError(card: Card?, error: TangemError): TangemError {
        if (error is TangemSdkError.InvalidParams) {
            val card = card ?: return error

            if (card.firmwareVersion >= FirmwareVersion.IsPasscodeStatusAvailable && card.isPasscodeSet == false) {
                return TangemSdkError.AlreadyCreated()
            }
        }

        return error
    }

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val card = environment.card ?: throw TangemSdkError.MissingPreflightRead()

        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.Pin, environment.accessCode.value)
        tlvBuilder.append(TlvTag.Pin2, environment.passcode.value)
        tlvBuilder.append(TlvTag.CardId, card.cardId)
        tlvBuilder.append(TlvTag.Cvc, environment.cvc)

        if (card.firmwareVersion >= FirmwareVersion.MultiWalletAvailable) {
            val cardWalletSettingsMask = MaskBuilder().apply {
                add(CardWallet.SettingsMask.Code.IsReusable)
            }.build<CardWallet.SettingsMask>()

            tlvBuilder.append(TlvTag.SettingsMask, cardWalletSettingsMask)
            tlvBuilder.append(TlvTag.CurveId, curve)
            tlvBuilder.append(TlvTag.SigningMethod, signingMethod)

            // We need to execute this wallet index calculation stuff only after precheck.
            // Run fires only before precheck. And precheck will not fire if error handling disabled
            val maxIndex = card.settings.maxWalletsCount
            val occupiedIndexes = card.wallets.map { it.index }
            val allIndexes = 0 until maxIndex

            walletIndex = allIndexes.filter { !occupiedIndexes.contains(it) }.minOrNull().guard {
                if (maxIndex == 1) {
                    throw TangemSdkError.AlreadyCreated()
                } else {
                    throw TangemSdkError.MaxNumberOfWalletsCreated()
                }
            }
            tlvBuilder.append(TlvTag.WalletIndex, walletIndex)
        }

        return CommandApdu(Instruction.CreateWallet, tlvBuilder.serialize())
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): CreateWalletResponse {
        val tlvData = apdu.getTlvData(environment.encryptionKey) ?: throw TangemSdkError.DeserializeApduFailed()
        val card = environment.card ?: throw TangemSdkError.UnknownError()

        val decoder = TlvDecoder(tlvData)
        val index = decoder.decodeOptional(TlvTag.WalletIndex) ?: walletIndex!!
        val wallet = CardWallet(
                decoder.decode(TlvTag.WalletPublicKey),
                decoder.decodeOptional(TlvTag.WalletHDChain),
                curve,
                CardWallet.Settings(card.settings.isPermanentWallet),
                0,
                card.remainingSignatures,
                index
        )
        return CreateWalletResponse(decoder.decode(TlvTag.CardId), wallet)
    }
}


package com.tangem.operations.read

import com.squareup.moshi.JsonClass
import com.tangem.Log
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.card.Card
import com.tangem.common.card.CardWallet
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.CardSession
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.SessionEnvironment
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.common.deserialization.WalletDeserializer
import com.tangem.common.hdWallet.DerivationPath
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag
import com.tangem.operations.Command
import com.tangem.operations.CommandResponse
import com.tangem.operations.PreflightReadMode

/**
 * Created by Anton Zhilenkov on 26/03/2021.
 */
@JsonClass(generateAdapter = true)
class ReadWalletResponse(
    val cardId: String,
    val wallet: CardWallet
) : CommandResponse

/**
 * Read signle wallet on card. This command executes before interacting with specific wallet to retrieve
 * information about it and perform prechecks
 */
class ReadWalletCommand(
    private val walletIndex: Int,
    private val derivationPath: DerivationPath? = null
) : Command<ReadWalletResponse>() {

    override fun preflightReadMode(): PreflightReadMode = PreflightReadMode.ReadCardOnly

    override fun performPreCheck(card: Card): TangemError? {
        if (card.firmwareVersion < FirmwareVersion.MultiWalletAvailable) {
            return TangemSdkError.NotSupportedFirmwareVersion()
        }
        if (derivationPath != null && !card.settings.isHDWalletAllowed) {
            return TangemSdkError.HDWalletDisabled()
        }

        return null
    }

    override fun run(session: CardSession, callback: CompletionCallback<ReadWalletResponse>) {
        Log.command(this) { " attempt to read wallet with index: $walletIndex" }
        transceive(session, callback)
    }

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.Pin, environment.accessCode.value)
        tlvBuilder.append(TlvTag.CardId, environment.card?.cardId)
        tlvBuilder.append(TlvTag.InteractionMode, ReadMode.Wallet)
        tlvBuilder.append(TlvTag.WalletIndex, walletIndex)
        tlvBuilder.append(TlvTag.WalletHDPath, derivationPath)

        return CommandApdu(Instruction.Read, tlvBuilder.serialize())
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): ReadWalletResponse {
        val card = environment.card ?: throw TangemSdkError.UnknownError()
        val tlvData = apdu.getTlvData() ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)
        val wallet = try {
            WalletDeserializer(card.settings.isPermanentWallet).deserializeWallet(decoder)
        } catch (ex: Exception) {
            throw TangemSdkError.WalletNotFound()
        }

        Log.debug { "Read wallet: $wallet" }
        return ReadWalletResponse(decoder.decode(TlvTag.CardId), wallet)
    }
}
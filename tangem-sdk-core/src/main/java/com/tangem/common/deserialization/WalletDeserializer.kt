package com.tangem.common.deserialization

import com.tangem.common.card.CardWallet
import com.tangem.common.core.TangemSdkError
import com.tangem.common.tlv.Tlv
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag

/**
 * Deserialize v4 wallets only
 * @property isDefaultPermanentWallet - newest v4 cards don't have their own wallet settings,
 * so we should take them from the card's settings
 */
internal class WalletDeserializer(
    private val isDefaultPermanentWallet: Boolean,
) {

    internal fun deserializeWallets(decoder: TlvDecoder): DeserializedWallets {
        val cardWalletsData: List<ByteArray> = decoder.decodeArray(TlvTag.CardWallet)
        if (cardWalletsData.isEmpty()) throw TangemSdkError.DeserializeApduFailed()

        val walletsDecoders = cardWalletsData.mapNotNull { walletData ->
            Tlv.deserialize(walletData)?.let { tlvs -> TlvDecoder(tlvs) }
        }
        val wallets = walletsDecoders.mapNotNull {
            try {
                deserializeWallet(it)
            } catch (exception: TangemSdkError.WalletNotFound) {
                null
            }
        }

        return Pair(wallets, cardWalletsData.size)
    }

    internal fun deserializeWallet(decoder: TlvDecoder): CardWallet {
        val status: CardWallet.Status? = decoder.decode(TlvTag.Status)
        return if (status == CardWallet.Status.Loaded || status == CardWallet.Status.Backuped) {
            deserialize(decoder, status)
        } else {
            throw TangemSdkError.WalletNotFound()
        }
    }

    private fun deserialize(decoder: TlvDecoder, status: CardWallet.Status): CardWallet {
        val walletSettingsMask: CardWallet.SettingsMask? =
            decoder.decodeOptional(TlvTag.SettingsMask)

        val settings = if (walletSettingsMask != null) {
            CardWallet.Settings(walletSettingsMask)
        } else {
            // Newest v4 cards don't have their own wallet settings, so we should take them from the card's settings
            CardWallet.Settings(isDefaultPermanentWallet)
        }

        return CardWallet(
            publicKey = decoder.decode(TlvTag.WalletPublicKey),
            chainCode = decoder.decodeOptional(TlvTag.WalletHDChain),
            curve = decoder.decode(TlvTag.CurveId),
            settings = settings,
            totalSignedHashes = decoder.decode(TlvTag.WalletSignedHashes),
            remainingSignatures = null,
            index = decoder.decode(TlvTag.WalletIndex),
            hasBackup = status == CardWallet.Status.Backuped,
        )
    }
}

typealias DeserializedWallets = Pair<List<CardWallet>, Int>

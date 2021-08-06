package com.tangem.common.deserialization

import com.tangem.common.card.CardWallet
import com.tangem.common.core.TangemSdkError
import com.tangem.common.tlv.Tlv
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag

/**
 * Created by Anton Zhilenkov on 11/07/2021.
 */
class WalletDeserializer {
    companion object {
        internal fun deserializeWallets(decoder: TlvDecoder): DeserializedWallets {
            val cardWalletsData: List<ByteArray> = decoder.decodeArray(TlvTag.CardWallet)
            if (cardWalletsData.isEmpty()) throw TangemSdkError.DeserializeApduFailed()

            val walletsDecoders = cardWalletsData.mapNotNull { walletData ->
                Tlv.deserialize(walletData)?.let { tlvs -> TlvDecoder(tlvs) }
            }
            val wallets = walletsDecoders.mapNotNull { deserializeWallet(it) }

            return Pair(wallets, cardWalletsData.size)
        }

        internal fun deserializeWallet(decoder: TlvDecoder): CardWallet? {
            val status: CardWallet.Status = decoder.decode(TlvTag.Status)
            return if (status != CardWallet.Status.Loaded) null else deserialize(decoder)
        }

        private fun deserialize(decoder: TlvDecoder): CardWallet {
            val settings = CardWallet.Settings(decoder.decode(TlvTag.SettingsMask) as CardWallet.SettingsMask)

            return CardWallet(
                    decoder.decode(TlvTag.WalletPublicKey),
                    decoder.decodeOptional(TlvTag.WalletHDChain),
                    decoder.decode(TlvTag.CurveId),
                    settings,
                    decoder.decode(TlvTag.WalletSignedHashes),
                    null,
                    decoder.decode(TlvTag.WalletIndex)
            )
        }
    }
}

typealias DeserializedWallets = Pair<List<CardWallet>, Int>
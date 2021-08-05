package com.tangem.common.deserialization

import com.tangem.common.card.Token
import com.tangem.common.card.WalletData
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag

/**
 * Created by Anton Zhilenkov on 20/07/2021.
 */
class WalletDataDeserializer {
    companion object {
        internal fun deserialize(cardDataDecoder: TlvDecoder): WalletData? {
            val blockchain: String = cardDataDecoder.decodeOptional(TlvTag.BlockchainName) ?: return null

            return WalletData(blockchain, deserializeToken(cardDataDecoder))
        }

        private fun deserializeToken(cardDataDecoder: TlvDecoder): Token? {
            val tokenName: String? = cardDataDecoder.decodeOptional(TlvTag.TokenName)
            val tokenSymbol: String = cardDataDecoder.decodeOptional(TlvTag.TokenSymbol)
                    ?: return null
            val tokenContractAddress: String = cardDataDecoder.decodeOptional(TlvTag.TokenContractAddress)
                    ?: return null
            val tokenDecimals: Int = cardDataDecoder.decodeOptional(TlvTag.TokenDecimal)
                    ?: return null

            return Token(tokenName ?: tokenSymbol,
                    tokenSymbol,
                    tokenContractAddress,
                    tokenDecimals)
        }
    }
}
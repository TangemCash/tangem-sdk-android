package com.tangem.common.deserialization

import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.card.Card
import com.tangem.common.card.CardWallet
import com.tangem.common.card.EllipticCurve
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.SessionEnvironment
import com.tangem.common.core.TangemSdkError
import com.tangem.common.tlv.Tlv
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag

class CardDeserializer {

    companion object {
        internal fun deserialize(
            decoder: TlvDecoder,
            cardDataDecoder: TlvDecoder?,
            allowNotPersonalized: Boolean = false
        ): Card {
            val status: Card.Status = decoder.decode(TlvTag.Status)
            assertStatus(allowNotPersonalized, status)
            assertActivation(decoder.decode(TlvTag.IsActivated) as Boolean)
            val cardDataDecoder = cardDataDecoder ?: throw TangemSdkError.DeserializeApduFailed()

            val firmware = FirmwareVersion(decoder.decode(TlvTag.Firmware))
            val cardSettingsMask: Card.SettingsMask = decoder.decode(TlvTag.SettingsMask)

            val isPasscodeSet: Boolean? = if (firmware >= FirmwareVersion.IsPasscodeStatusAvailable) {
                !(decoder.decode(TlvTag.Pin2IsDefault) as Boolean)
            } else {
                null
            }

            val defaultCurve: EllipticCurve? = decoder.decodeOptional(TlvTag.CurveId)
            val wallets = mutableListOf<CardWallet>()
            var remainingSignatures: Int? = null

            if (firmware < FirmwareVersion.MultiWalletAvailable && status == Card.Status.Loaded) {
                val curve = defaultCurve ?: throw TangemSdkError.DecodingFailed("Missing curve id")

                remainingSignatures = decoder.decodeOptional(TlvTag.WalletRemainingSignatures)
                val walletSettings = CardWallet.Settings(cardSettingsMask.toWalletSettingsMask())

                val wallet = CardWallet(
                        decoder.decode(TlvTag.WalletPublicKey),
                        null,
                        curve,
                        walletSettings,
                        decoder.decodeOptional(TlvTag.WalletSignedHashes),
                        remainingSignatures,
                        0
                )
                wallets.add(wallet)
            }

            val manufacturer = Card.Manufacturer(
                    decoder.decode(TlvTag.ManufacturerName),
                    cardDataDecoder.decode(TlvTag.ManufactureDateTime),
                    cardDataDecoder.decode(TlvTag.CardIDManufacturerSignature),
            )
            val issuer = Card.Issuer(
                    cardDataDecoder.decode(TlvTag.IssuerName),
                    decoder.decode(TlvTag.IssuerDataPublicKey),
            )

            val terminalStatus = if (decoder.decode(TlvTag.TerminalIsLinked)) {
                Card.LinkedTerminalStatus.Current
            } else {
                Card.LinkedTerminalStatus.None
            }

            val settings = cardSettings(decoder, cardSettingsMask, defaultCurve)

            val supportedCurves: List<EllipticCurve> = if (firmware < FirmwareVersion.MultiWalletAvailable) {
                if (defaultCurve == null) listOf() else listOf(defaultCurve)
            } else {
                EllipticCurve.values().toList()
            }
            return Card(
                    decoder.decode(TlvTag.CardId),
                    cardDataDecoder.decode(TlvTag.BatchId),
                    decoder.decode(TlvTag.CardPublicKey),
                    firmware,
                    manufacturer,
                    issuer,
                    settings,
                    terminalStatus,
                    isPasscodeSet,
                    supportedCurves,
                    wallets.toList(),
                    health = decoder.decodeOptional(TlvTag.Health),
                    remainingSignatures = remainingSignatures
            )
        }

        internal fun getDecoder(environment: SessionEnvironment, apdu: ResponseApdu): TlvDecoder {
            val tlv = apdu.getTlvData(environment.encryptionKey) ?: throw TangemSdkError.DeserializeApduFailed()

            return TlvDecoder(tlv)
        }

        internal fun getCardDataDecoder(environment: SessionEnvironment, tlvData: List<Tlv>): TlvDecoder? {
            val cardDataValue = tlvData.find { it.tag == TlvTag.CardData }?.let {
                Tlv.deserialize(it.value)
            }

            return if (cardDataValue.isNullOrEmpty()) null else TlvDecoder(cardDataValue)
        }

        private fun assertStatus(allowNotPersonalized: Boolean, status: Card.Status) {
            when {
                status == Card.Status.NotPersonalized && !allowNotPersonalized -> throw TangemSdkError.NotPersonalized()
                status == Card.Status.Purged -> throw TangemSdkError.WalletIsPurged()
            }
        }

        private fun assertActivation(isNeedActivation: Boolean) {
            if (isNeedActivation) throw TangemSdkError.NotActivated()
        }

        private fun cardSettings(
            decoder: TlvDecoder,
            mask: Card.SettingsMask,
            defaultCurve: EllipticCurve?
        ): Card.Settings = Card.Settings(
                decoder.decodeOptional<Int>(TlvTag.PauseBeforePin2)?.let { it * 10 } ?: 0,
                decoder.decodeOptional(TlvTag.WalletsCount) ?: 1,
                mask,
                decoder.decode(TlvTag.SigningMethod),
                defaultCurve,
        )
    }
}
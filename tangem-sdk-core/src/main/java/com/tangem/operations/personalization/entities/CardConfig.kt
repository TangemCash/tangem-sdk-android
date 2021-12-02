package com.tangem.operations.personalization.entities

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.common.Mask
import com.tangem.common.MaskBuilder
import com.tangem.common.card.Card
import com.tangem.common.card.EllipticCurve
import com.tangem.common.card.SigningMethod
import com.tangem.common.extensions.calculateSha256
import java.util.*

/**
 * It is a configuration file with all the card settings that are written on the card
 * during [PersonalizeCommand].
 */
@JsonClass(generateAdapter = true)
data class CardConfig(
    internal val releaseVersion: Boolean,
    internal val issuerName: String,
    internal val series: String?,
    internal val startNumber: Long,
    internal val count: Int,
    internal val numberFormat: String,
    @Json(name = "PIN")
    internal val pin: String,
    @Json(name = "PIN2")
    internal val pin2: String,
    @Json(name = "PIN3")
    internal val pin3: String?,
    internal val hexCrExKey: ByteArray?,
    @Json(name = "CVC")
    internal val cvc: String,
    @Json(name = "pauseBeforePIN2")
    internal val pauseBeforePin2: Int,
    internal val smartSecurityDelay: Boolean,
    internal val curveID: EllipticCurve,
    @Json(name = "SigningMethod")
    internal val signingMethod: SigningMethod,
    @Json(name = "MaxSignatures")
    internal val maxSignatures: Int?,
    @Json(name = "allowSwapPIN")
    internal val allowSetPIN1: Boolean,
    @Json(name = "allowSwapPIN2")
    internal val allowSetPIN2: Boolean,
    internal val useActivation: Boolean,
    @Json(name = "useCVC")
    internal val useCvc: Boolean,
    @Json(name = "useNDEF")
    internal val useNDEF: Boolean,
    internal val useDynamicNDEF: Boolean?,
    internal val useOneCommandAtTime: Boolean?,
    internal val useBlock: Boolean,
    internal val allowSelectBlockchain: Boolean,
    @Json(name = "forbidPurgeWallet")
    internal val prohibitPurgeWallet: Boolean,
    @Json(name = "protocolAllowUnencrypted")
    internal val allowUnencrypted: Boolean,
    @Json(name = "protocolAllowStaticEncryption")
    internal val allowFastEncryption: Boolean,
    internal val protectIssuerDataAgainstReplay: Boolean?,
    @Json(name = "forbidDefaultPIN")
    internal val prohibitDefaultPIN1: Boolean,
    internal val disablePrecomputedNDEF: Boolean?,
    internal val skipSecurityDelayIfValidatedByIssuer: Boolean,
    @Json(name = "skipCheckPIN2andCVCIfValidatedByIssuer")
    internal val skipCheckPIN2CVCIfValidatedByIssuer: Boolean,
    internal val skipSecurityDelayIfValidatedByLinkedTerminal: Boolean,
    internal val restrictOverwriteIssuerDataEx: Boolean?,
    internal val disableIssuerData: Boolean?,
    internal val disableUserData: Boolean?,
    internal val disableFiles: Boolean?,
    internal val allowHDWallets: Boolean?,
    internal val allowBackup: Boolean?,
    internal val createWallet: Int,
    internal val cardData: CardConfigData,
    @Json(name = "NDEF")
    val ndefRecords: List<NdefRecord>,
    /**
     * Number of wallets supported by card, by default - 1
     */
    internal val walletsCount: Int?,
    internal val isReusable: Boolean?,
) {

    fun pinSha256(): ByteArray = pin.calculateSha256()
    fun pin2Sha256(): ByteArray = pin2.calculateSha256()
    fun pin3Sha256(): ByteArray? = pin3?.calculateSha256()

    companion object

    class CardConfigData(
        val date: Date?,
        val batch: String,
        val blockchain: String,
        @Json(name = "product_note")
        val productNote: Boolean?,
        @Json(name = "product_tag")
        val productTag: Boolean?,
        @Json(name = "product_id_card")
        val productIdCard: Boolean?,
        @Json(name = "product_id_issuer")
        val productIdIssuer: Boolean?,
        @Json(name = "product_authentication")
        val productAuthentication: Boolean?,
        @Json(name = "product_twin_card")
        val productTwin: Boolean?,
        @Json(name = "token_symbol")
        val tokenSymbol: String?,
        @Json(name = "token_contract_address")
        val tokenContractAddress: String?,
        @Json(name = "token_decimal")
        val tokenDecimal: Int?,
    ) {

        internal fun createPersonalizationCardData(): CardData {
            return CardData(
                batch,
                date ?: Date(),
                blockchain,
                createProductMask(),
                tokenSymbol,
                tokenContractAddress,
                tokenDecimal
            )
        }

        private fun createProductMask(): ProductMask {
            val builder = MaskBuilder()
            if (productNote == true) {
                builder.add(ProductMask.Code.Note)
            }
            if (productTag == true) {
                builder.add(ProductMask.Code.Tag)
            }
            if (productIdCard == true) {
                builder.add(ProductMask.Code.IdCard)
            }
            if (productIdIssuer == true) {
                builder.add(ProductMask.Code.IdIssuer)
            }
            if (productAuthentication == true) {
                builder.add(ProductMask.Code.Authentication)
            }
            if (productTwin == true) {
                builder.add(ProductMask.Code.TwinCard)
            }

            return builder.build()
        }
    }
}

internal fun CardConfig.createSettingsMask(): Card.SettingsMask {
    val builder = MaskBuilder()

    val isReusable = isReusable ?: true
    builder.addIf(isReusable, Card.SettingsMask.Code.IsReusable)
    builder.addIf(allowSetPIN1, Card.SettingsMask.Code.AllowSetPIN1)
    builder.addIf(allowSetPIN2, Card.SettingsMask.Code.AllowSetPIN2)
    builder.addIf(useCvc, Card.SettingsMask.Code.UseCvc)
    builder.addIf(useOneCommandAtTime, Card.SettingsMask.Code.UseOneCommandAtTime)
    builder.addIf(useNDEF, Card.SettingsMask.Code.UseNDEF)
    builder.addIf(useDynamicNDEF, Card.SettingsMask.Code.UseDynamicNDEF)
    builder.addIf(disablePrecomputedNDEF, Card.SettingsMask.Code.DisablePrecomputedNDEF)
    builder.addIf(allowUnencrypted, Card.SettingsMask.Code.AllowUnencrypted)
    builder.addIf(allowFastEncryption, Card.SettingsMask.Code.AllowFastEncryption)
    builder.addIf(prohibitDefaultPIN1, Card.SettingsMask.Code.ProhibitDefaultPIN1)
    builder.addIf(useActivation, Card.SettingsMask.Code.UseActivation)
    builder.addIf(useBlock, Card.SettingsMask.Code.UseBlock)
    builder.addIf(smartSecurityDelay, Card.SettingsMask.Code.SmartSecurityDelay)
    builder.addIf(protectIssuerDataAgainstReplay, Card.SettingsMask.Code.ProtectIssuerDataAgainstReplay)
    builder.addIf(prohibitPurgeWallet, Card.SettingsMask.Code.PermanentWallet)
    builder.addIf(allowSelectBlockchain, Card.SettingsMask.Code.AllowSelectBlockchain)
    builder.addIf(skipCheckPIN2CVCIfValidatedByIssuer, Card.SettingsMask.Code.SkipCheckPIN2CVCIfValidatedByIssuer)
    builder.addIf(skipSecurityDelayIfValidatedByIssuer, Card.SettingsMask.Code.SkipSecurityDelayIfValidatedByIssuer)
    builder.addIf(skipSecurityDelayIfValidatedByLinkedTerminal, Card.SettingsMask.Code.SkipSecurityDelayIfValidatedByLinkedTerminal)
    builder.addIf(restrictOverwriteIssuerDataEx, Card.SettingsMask.Code.RestrictOverwriteIssuerExtraData)
    builder.addIf(disableIssuerData, Card.SettingsMask.Code.DisableIssuerData)
    builder.addIf(disableUserData, Card.SettingsMask.Code.DisableUserData)
    builder.addIf(disableFiles, Card.SettingsMask.Code.DisableFiles)
    builder.addIf(allowHDWallets, Card.SettingsMask.Code.AllowHDWallets)
    builder.addIf(allowBackup, Card.SettingsMask.Code.AllowBackup)
    builder.addIf(isReusable, Card.SettingsMask.Code.IsReusable)

    return builder.build()
}

private fun MaskBuilder.addIf(condition: Boolean?, maskCode: Mask.Code) {
    if (condition == true) add(maskCode)
}

internal fun CardConfig.createCardId(): String? {
    if (series == null) return null
    if (startNumber <= 0 || (series.length != 2 && series.length != 4)) return null

    val Alf = "ABCDEF0123456789"
    fun checkSeries(series: String): Boolean {
        val containsList = series.filter { Alf.contains(it) }
        return containsList.length == series.length
    }
    if (!checkSeries(series)) return null

    val tail = if (series.length == 2) String.format("%013d", startNumber) else String.format("%011d", startNumber)
    var cardId = (series + tail).replace(" ", "")
    if (cardId.length != 15 || Alf.indexOf(cardId[0]) == -1 || Alf.indexOf(cardId[1]) == -1)
        return null

    cardId += "0"
    val length = cardId.length
    var sum = 0
    for (i in 0 until length) {
        // get digits in reverse order
        var digit: Int
        val cDigit = cardId[length - i - 1]
        digit = if (cDigit in '0'..'9') cDigit - '0' else cDigit - 'A'

        // every 2nd number multiply with 2
        if (i % 2 == 1) digit *= 2
        sum += if (digit > 9) digit - 9 else digit
    }
    val lunh = (10 - sum % 10) % 10
    return cardId.substring(0, 15) + String.format("%d", lunh)
}
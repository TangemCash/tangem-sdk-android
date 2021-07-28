package com.tangem.operations.personalization.entities

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.common.MaskBuilder
import com.tangem.common.card.CardSettingsMask
import com.tangem.common.card.EllipticCurve
import com.tangem.common.card.SigningMethod
import com.tangem.common.extensions.calculateSha256
import com.tangem.crypto.sign
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
    internal val startNumber: Int,
    internal val count: Int,
    internal val numberFormat: String,
    @Json(name = "PIN")
    internal val pin: ByteArray,
    @Json(name = "PIN2")
    internal val pin2: ByteArray,
    @Json(name = "PIN3")
    internal val pin3: ByteArray?,
    internal val hexCrExKey: String?,
    @Json(name = "CVC")
    internal val cvc: String,
    @Json(name = "pauseBeforePIN2")
    internal val pauseBeforePin2: Int,
    internal val smartSecurityDelay: Boolean,
    internal val curveID: EllipticCurve,
    @Json(name = "SigningMethod")
    internal val signingMethod: SigningMethod,
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
    internal val restrictOverwriteIssuerExtraData: Boolean?,
    internal val disableIssuerData: Boolean?,
    internal val disableUserData: Boolean?,
    internal val disableFiles: Boolean?,
    internal val createWallet: Int,
    internal val cardData: CardConfigData,
    @Json(name = "NDEF")
    val ndefRecords: List<NdefRecord>,
    /**
     * Number of wallets supported by card, by default - 1
     */
    internal val walletsCount: Byte?,
) {

    fun pinSha256(): ByteArray = pin.calculateSha256()
    fun pin2Sha256(): ByteArray = pin2.calculateSha256()
    fun pin3Sha256(): ByteArray? = pin3?.calculateSha256()

    companion object

    class CardConfigData(
        val date: Date?,
        val batch: String,
        val blockchain: String,
        val productNote: Boolean?,
        val productTag: Boolean?,
        val productIdCard: Boolean?,
        val productIdIssuer: Boolean?,
        val productAuthentication: Boolean?,
        val productTwin: Boolean?,
        val tokenSymbol: String?,
        val tokenContractAddress: String?,
        val tokenDecimal: Int?,
    ) {

        internal fun createPersonalizationCardData(issuer: Issuer, manufacturer: Manufacturer, cardId: String): CardData {
            val manufacturerSignature = cardId.toByteArray().sign(manufacturer.keyPair.privateKey)

            return CardData(batch,
                    date ?: Date(),
                    issuer.name,
                    blockchain,
                    manufacturerSignature,
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

internal fun CardConfig.createSettingsMask(): CardSettingsMask {
    val builder = MaskBuilder()

    if (allowSetPIN1) {
        builder.add(CardSettingsMask.Code.AllowSetPIN1)
    }
    if (allowSetPIN2) {
        builder.add(CardSettingsMask.Code.AllowSetPIN2)
    }
    if (useCvc) {
        builder.add(CardSettingsMask.Code.UseCvc)
    }

    //Now we can personalize only reusable wallets
    builder.add(CardSettingsMask.Code.IsReusable)

    if (useOneCommandAtTime == true) {
        builder.add(CardSettingsMask.Code.UseOneCommandAtTime)
    }
    if (useNDEF) {
        builder.add(CardSettingsMask.Code.UseNDEF)
    }
    if (useDynamicNDEF == true) {
        builder.add(CardSettingsMask.Code.UseDynamicNDEF)
    }
    if (disablePrecomputedNDEF == true) {
        builder.add(CardSettingsMask.Code.DisablePrecomputedNDEF)
    }
    if (allowUnencrypted) {
        builder.add(CardSettingsMask.Code.AllowUnencrypted)
    }
    if (allowFastEncryption) {
        builder.add(CardSettingsMask.Code.AllowFastEncryption)
    }
    if (prohibitDefaultPIN1) {
        builder.add(CardSettingsMask.Code.ProhibitDefaultPIN1)
    }
    if (useActivation) {
        builder.add(CardSettingsMask.Code.UseActivation)
    }
    if (useBlock) {
        builder.add(CardSettingsMask.Code.UseBlock)
    }
    if (smartSecurityDelay) {
        builder.add(CardSettingsMask.Code.SmartSecurityDelay)
    }
    if (protectIssuerDataAgainstReplay == true) {
        builder.add(CardSettingsMask.Code.ProtectIssuerDataAgainstReplay)
    }
    if (prohibitPurgeWallet) {
        builder.add(CardSettingsMask.Code.PermanentWallet)
    }
    if (allowSelectBlockchain) {
        builder.add(CardSettingsMask.Code.AllowSelectBlockchain)
    }
    if (skipCheckPIN2CVCIfValidatedByIssuer) {
        builder.add(CardSettingsMask.Code.SkipCheckPIN2CVCIfValidatedByIssuer)
    }
    if (skipSecurityDelayIfValidatedByIssuer) {
        builder.add(CardSettingsMask.Code.SkipSecurityDelayIfValidatedByIssuer)
    }
    if (skipSecurityDelayIfValidatedByLinkedTerminal) {
        builder.add(CardSettingsMask.Code.SkipSecurityDelayIfValidatedByLinkedTerminal)
    }
    if (restrictOverwriteIssuerExtraData == true) {
        builder.add(CardSettingsMask.Code.RestrictOverwriteIssuerExtraData)
    }
    if (disableIssuerData == true) {
        builder.add(CardSettingsMask.Code.DisableIssuerData)
    }
    if (disableUserData == true) {
        builder.add(CardSettingsMask.Code.DisableUserData)
    }
    if (disableFiles == true) {
        builder.add(CardSettingsMask.Code.DisableFiles)
    }
    return builder.build()
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

fun createCardId(series: String, startNumber: Long): String? {
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

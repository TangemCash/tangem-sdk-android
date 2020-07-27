package com.tangem.devkit.ucase.variants.responses.converter

import com.tangem.commands.*
import com.tangem.commands.personalization.DepersonalizeResponse
import com.tangem.devkit._arch.structure.abstraction.Item
import com.tangem.devkit._arch.structure.impl.TextItem
import com.tangem.devkit.ucase.variants.responses.*
import ru.dev.gbixahue.eu4d.lib.kotlin.stringOf

/**
 * Created by Anton Zhilenkov on 07/04/2020.
 */
class SignResponseConverter : BaseResponseConverter<SignResponse>() {

    override fun convert(from: SignResponse): List<Item> {
        return listOf(
                TextItem(SignId.cid, from.cardId),
                TextItem(SignId.walletSignedHashes, valueToString(from.walletSignedHashes)),
                TextItem(SignId.walletRemainingSignatures, valueToString(from.walletRemainingSignatures)),
                TextItem(SignId.signature, fieldConverter.byteArrayToHex(from.signature))
        )
    }
}

class DepersonalizeResponseConverter : BaseResponseConverter<DepersonalizeResponse>() {
    override fun convert(from: DepersonalizeResponse): List<Item> {
        return listOf(TextItem(DepersonalizeId.isSuccess, stringOf(from.success)))
    }
}

class CreateWalletResponseConverter : BaseResponseConverter<CreateWalletResponse>() {
    override fun convert(from: CreateWalletResponse): List<Item> {
        return listOf(
                TextItem(CreateWalletId.cid, from.cardId),
                TextItem(CreateWalletId.cardStatus, valueToString(from.status)),
                TextItem(CreateWalletId.walletPublicKey, fieldConverter.byteArrayToHex(from.walletPublicKey))
        )
    }
}

class PurgeWalletResponseConverter : BaseResponseConverter<PurgeWalletResponse>() {
    override fun convert(from: PurgeWalletResponse): List<Item> {
        return listOf(
                TextItem(CreateWalletId.cid, from.cardId),
                TextItem(CreateWalletId.cardStatus, valueToString(from.status))
        )
    }
}

class ReadIssuerDataResponseConverter : BaseResponseConverter<ReadIssuerDataResponse>() {
    override fun convert(from: ReadIssuerDataResponse): List<Item> {
        return listOf(
                TextItem(ReadIssuerDataId.cid, from.cardId),
                TextItem(ReadIssuerDataId.issuerData, fieldConverter.byteArrayToString(from.issuerData)),
                TextItem(ReadIssuerDataId.issuerDataSignature, fieldConverter.byteArrayToHex(from.issuerDataSignature)),
                TextItem(ReadIssuerDataId.issuerDataCounter, valueToString(from.issuerDataCounter))
        )
    }
}

class ReadIssuerExtraDataResponseConverter : BaseResponseConverter<ReadIssuerExtraDataResponse>() {
    override fun convert(from: ReadIssuerExtraDataResponse): List<Item> {
        return listOf(
                TextItem(ReadIssuerExtraDataId.cid, from.cardId),
                TextItem(ReadIssuerExtraDataId.size, valueToString(from.size)),
                TextItem(ReadIssuerExtraDataId.issuerData, fieldConverter.byteArrayToString(from.issuerData)),
                TextItem(ReadIssuerExtraDataId.issuerDataSignature, fieldConverter.byteArrayToHex(from.issuerDataSignature)),
                TextItem(ReadIssuerExtraDataId.issuerDataCounter, valueToString(from.issuerDataCounter))
        )
    }
}

class WriteIssuerDataResponseConverter : BaseResponseConverter<WriteIssuerDataResponse>() {
    override fun convert(from: WriteIssuerDataResponse): List<Item> {
        return listOf(
                TextItem(CardId.cardId, from.cardId)
        )
    }
}

class ReadUserDataResponseConverter : BaseResponseConverter<ReadUserDataResponse>() {
    override fun convert(from: ReadUserDataResponse): List<Item> {
        return listOf(
                TextItem(ReadUserDataId.cid, from.cardId),
                TextItem(ReadUserDataId.userData, fieldConverter.byteArrayToString(from.userData)),
                TextItem(ReadUserDataId.userProtectedData, fieldConverter.byteArrayToString(from.userProtectedData)),
                TextItem(ReadUserDataId.userCounter, valueToString(from.userCounter)),
                TextItem(ReadUserDataId.userProtectedCounter, valueToString(from.userProtectedCounter))
        )
    }
}

class WriteUserDataResponseConverter : BaseResponseConverter<WriteUserDataResponse>() {
    override fun convert(from: WriteUserDataResponse): List<Item> {
        return listOf(
                TextItem(CardId.cardId, from.cardId)
        )
    }
}
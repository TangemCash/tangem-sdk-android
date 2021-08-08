package com.tangem.common.moshiJsonConverter

import com.tangem.Message
import com.tangem.common.MaskBuilder
import com.tangem.common.card.Card
import com.tangem.common.card.CardWallet
import com.tangem.common.card.SigningMethod
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.crypto.CryptoUtils
import com.tangem.operations.attestation.Attestation
import com.tangem.operations.personalization.entities.ProductMask
import org.junit.Test

class MoshiJsonConverterTest {
    private val moshi = MoshiJsonConverter.INSTANCE

    @Test
    fun arrayOfByteArray() {
        val data: Array<ByteArray> = (0..3).map { CryptoUtils.generateRandomBytes(10) }.toTypedArray()
        val jsonBytesArray = moshi.toJson(data, "")
        val byteArray: Array<ByteArray>? = moshi.fromJson(jsonBytesArray)
        assert(byteArray != null)

        val nullSafeByteArray = byteArray!!
        assert(data.size == nullSafeByteArray.size)
        val result = data.mapIndexed { index, bytes -> nullSafeByteArray[index].contentEquals(bytes) }
        assert(!result.contains(false))
    }

    @Test
    fun productMask() {
        val builder = MaskBuilder().apply { ProductMask.Code.values().forEach { this.add(it) } }
        val initialProductMask: ProductMask = builder.build()

        val jsonList = moshi.toJson(initialProductMask)
        val resultProductMask: ProductMask? = moshi.fromJson(jsonList)
        assert(initialProductMask.rawValue == resultProductMask?.rawValue)
    }

    @Test
    fun settingsMask() {
        val builder = MaskBuilder().apply { Card.SettingsMask.Code.values().forEach { this.add(it) } }
        val initialMask: Card.SettingsMask = builder.build()

        val jsonList = moshi.toJson(initialMask)
        val resultMask: Card.SettingsMask? = moshi.fromJson(jsonList)
        assert(initialMask.rawValue == resultMask?.rawValue)
    }

    @Test
    fun signingMethodMask() {
        val initialMask = SigningMethod.build(*SigningMethod.Code.values())

        val jsonList = moshi.toJson(initialMask)
        val resultMask: SigningMethod? = moshi.fromJson(jsonList)
        assert(initialMask.rawValue == resultMask?.rawValue)
    }

    @Test
    fun walletSettingsMask() {
        val builder = MaskBuilder().apply { CardWallet.SettingsMask.Code.values().forEach { this.add(it) } }
        val initialMask: CardWallet.SettingsMask = builder.build()

        val jsonList = moshi.toJson(initialMask)
        val resultMask: CardWallet.SettingsMask? = moshi.fromJson(jsonList)
        assert(initialMask.rawValue == resultMask?.rawValue)
    }

    @Test
    fun attestation(){
        val attestation = Attestation(
                Attestation.Status.Verified,
                Attestation.Status.Failed,
                Attestation.Status.Warning,
                Attestation.Status.VerifiedOffline,
        )

        val json = moshi.toJson(attestation)
        val result:Attestation? = moshi.fromJson(json)

        assert(attestation == result)
    }

    @Test
    fun toMap() {
        val list = listOf("1", "2", "3")
        var resultMap: Map<String, Any> = moshi.toMap(list)
        assert(resultMap.isEmpty())

        val initialMessage = Message("header", null)
        resultMap = moshi.toMap(initialMessage)
        assert(resultMap["header"] == "header")
        assert(!resultMap.containsKey("body"))

        val resultMessage: Message? = moshi.fromJson(moshi.toJson(resultMap))
        assert(initialMessage == resultMessage)

        resultMap = moshi.toMap("sdknfdjn")
        assert(resultMap.isEmpty())

        val jsonMap = "{\"id\":1,\"jsonrpc\":\"2.0\",\"method\":\"SCAN_TASK\",\"params\":{}}";
        resultMap = moshi.toMap(jsonMap)
        assert(resultMap["id"] == 1.0)
        assert(resultMap["jsonrpc"] == "2.0")
        assert((resultMap["params"] as Map<*, *>).isEmpty())


        val jsonMapWithNull = "{\"id\":null,\"jsonrpc\":\"2.0\"}";
        resultMap = moshi.toMap(jsonMapWithNull)
        assert(resultMap.isNotEmpty())
        assert(!resultMap.containsKey("id"))
        assert(resultMap["id"] == null)
        assert(resultMap["jsonrpc"] == "2.0")

        val jsonArray = "[1, 2, 4]";
        resultMap = moshi.toMap(jsonArray)
        assert(resultMap.isEmpty())
    }
}
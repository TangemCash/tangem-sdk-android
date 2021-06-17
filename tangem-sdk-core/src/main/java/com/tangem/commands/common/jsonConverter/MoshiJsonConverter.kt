package com.tangem.commands.common.jsonConverter

import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.commands.common.card.FirmwareVersion
import com.tangem.commands.common.card.masks.*
import com.tangem.commands.wallet.WalletIndex
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import java.lang.reflect.ParameterizedType
import java.text.SimpleDateFormat
import java.util.*
import kotlin.reflect.KClass

class MoshiJsonConverter(adapters: List<Any> = listOf()) {

    val moshi: Moshi = Moshi.Builder().apply {
        adapters.forEach { this.add(it) }
        add(KotlinJsonAdapterFactory())
    }.build()

    inline fun <reified T> fromJson(json: String): T? {
        val adapter = moshi.adapter(T::class.java)
        return adapter.fromJson(json)
    }

    fun <T> fromJson(json: String, type: ParameterizedType): T? {
        return moshi.adapter<T>(type).fromJson(json)
    }

    fun toJson(any: Any?, indent: String? = null): String {
        return when (any) {
            null -> "{}"
            else -> moshi.adapter(Any::class.java).indent(indent ?: "").toJson(any)
        }
    }

    fun toMap(any: Any?): Map<String, Any> {
        val rawResult: Map<String, Any?>? = when (any) {
            null -> null
            is List<*> -> null
            is String -> {
                if (any.startsWith("{") && any.endsWith("}")) {
                    fromJson<Map<String, Any?>?>(any, typedMap())
                } else {
                    null
                }
            }
            else -> fromJson(toJson(any), typedMap())
        }
        val result = mutableMapOf<String, Any>()
        rawResult?.filterNot { it.value == null }?.forEach { result[it.key] = it.value!! }
        return result.toMap()
    }

    fun prettyPrint(any: Any?, indent: String = "   "): String = toJson(any, indent)

    fun typedList(clazz: Class<*>): ParameterizedType {
        return Types.newParameterizedType(List::class.java, clazz)
    }

    fun typedMap(key: KClass<*> = String::class, value: KClass<*> = Any::class): ParameterizedType {
        return Types.newParameterizedType(Map::class.java, key.javaObjectType, value.javaObjectType)
    }

    companion object {
        var INSTANCE: MoshiJsonConverter = default()
            private set

        fun setInstance(converter: MoshiJsonConverter) {
            INSTANCE = converter
        }

        fun default() = MoshiJsonConverter(getTangemSdkAdapters())

        fun getTangemSdkAdapters(): List<Any> {
            return listOf(
                    TangemSdkAdapter.ByteTypeAdapter(),
                    TangemSdkAdapter.SigningMethodTypeAdapter(),
                    TangemSdkAdapter.SettingsMaskTypeAdapter(),
                    TangemSdkAdapter.ProductMaskTypeAdapter(),
                    TangemSdkAdapter.WalletSettingsMaskAdapter(),
                    TangemSdkAdapter.DateTypeAdapter(),
                    TangemSdkAdapter.FirmwareVersionAdapter(),
                    TangemSdkAdapter.WalletIntIndexAdapter(),
                    TangemSdkAdapter.WalletPubKeyIndexAdapter(),
                    TangemSdkAdapter.WalletIndexAdapter(),
            )
        }
    }
}

class TangemSdkAdapter {
    class ByteTypeAdapter {
        @ToJson
        fun toJson(src: ByteArray): String = src.toHexString()

        @FromJson
        fun fromJson(json: String): ByteArray = json.hexToBytes()
    }

    class SettingsMaskTypeAdapter {
        @ToJson
        fun toJson(src: SettingsMask): List<String> = src.toList().map { it.name }

        @FromJson
        fun fromJson(jsonList: List<String>): SettingsMask = SettingsMaskBuilder().apply {
            jsonList.forEach {
                try {
                    add(Settings.valueOf(it))
                } catch (ex: IllegalArgumentException) {
                    ex.printStackTrace()
                }
            }
        }.build()
    }

    class ProductMaskTypeAdapter {
        @ToJson
        fun toJson(src: ProductMask): List<String> = src.toList().map { it.name }

        @FromJson
        fun fromJson(jsonList: List<String>): ProductMask = ProductMaskBuilder().apply {
            jsonList.forEach {
                try {
                    add(Product.valueOf(it))
                } catch (ex: IllegalArgumentException) {
                    ex.printStackTrace()
                }
            }
        }.build()
    }

    class SigningMethodTypeAdapter {
        @ToJson
        fun toJson(src: SigningMethodMask): List<String> = src.toList().map { it.name }

        @FromJson
        fun fromJson(jsonList: List<String>): SigningMethodMask = SigningMethodMaskBuilder().apply {
            jsonList.forEach {
                try {
                    add(SigningMethod.valueOf(it))
                } catch (ex: IllegalArgumentException) {
                    ex.printStackTrace()
                }
            }
        }.build()
    }

    class WalletSettingsMaskAdapter {
        @ToJson
        fun toJson(src: WalletSettingsMask): List<String> = src.toList().map { it.name }

        @FromJson
        fun fromJson(jsonList: List<String>): WalletSettingsMask = WalletSettingsMaskBuilder().apply {
            jsonList.forEach {
                try {
                    add(WalletSetting.valueOf(it))
                } catch (ex: IllegalArgumentException) {
                    ex.printStackTrace()
                }
            }
        }.build()
    }

    class DateTypeAdapter {
        private val dateFormatter = SimpleDateFormat("yyyy-MM-dd")

        @ToJson
        fun toJson(src: Date): String {
            return dateFormatter.format(src).toString()
        }

        @FromJson
        fun fromJson(json: String): Date = dateFormatter.parse(json)
    }

    class FirmwareVersionAdapter {
        @ToJson
        fun toJson(src: FirmwareVersion): String = src.version

        @FromJson
        fun fromJson(json: String): FirmwareVersion = FirmwareVersion(json)
    }

    class WalletIntIndexAdapter {
        @ToJson
        fun toJson(src: WalletIndex.Index): String = src.index.toString()

        @FromJson
        fun fromJson(json: String): WalletIndex.Index = WalletIndex.Index(json.toInt())
    }

    class WalletPubKeyIndexAdapter {
        @ToJson
        fun toJson(src: WalletIndex.PublicKey): String = src.data.toHexString()

        @FromJson
        fun fromJson(json: String): WalletIndex.PublicKey {
            return WalletIndex.PublicKey(json.hexToBytes())
        }
    }

    class WalletIndexAdapter {
        @ToJson
        fun toJson(src: WalletIndex): String {
            return when (src) {
                is WalletIndex.Index -> WalletIntIndexAdapter().toJson(src)
                is WalletIndex.PublicKey -> WalletPubKeyIndexAdapter().toJson(src)
            }
        }

        @FromJson
        fun fromJson(json: String): WalletIndex {
            val possibleIntIndex = json.toIntOrNull()
            return if (possibleIntIndex != null) {
                WalletIntIndexAdapter().fromJson(json)
            } else {
                WalletPubKeyIndexAdapter().fromJson(json)
            }
        }
    }
}
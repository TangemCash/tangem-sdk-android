package com.tangem.common.json

import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.common.MaskBuilder
import com.tangem.common.card.CardSettingsMask
import com.tangem.common.card.CardWalletSettingsMask
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.card.SigningMethod
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import com.tangem.operations.personalization.entities.ProductMask
import java.lang.reflect.ParameterizedType
import java.text.SimpleDateFormat
import java.util.*
import kotlin.reflect.KClass

class MoshiJsonConverter(adapters: List<Any> = listOf()) {

    val moshi: Moshi = Moshi.Builder().apply {
        adapters.forEach { this.add(it) }
        add(KotlinJsonAdapterFactory())
    }.build()

    fun <T> fromJson(json: String, klass: KClass<*>): T? {
        val adapter = moshi.adapter(klass::class.java)
        return adapter.fromJson(json) as? T
    }

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
        fun toJson(src: CardSettingsMask): List<String> = src.toList().map { it.name }

        @FromJson
        fun fromJson(jsonList: List<String>): CardSettingsMask = MaskBuilder().apply {
            jsonList.forEach {
                try {
                    add(CardSettingsMask.Code.valueOf(it))
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
        fun fromJson(jsonList: List<String>): ProductMask = MaskBuilder().apply {
            jsonList.forEach {
                try {
                    add(ProductMask.Code.valueOf(it))
                } catch (ex: IllegalArgumentException) {
                    ex.printStackTrace()
                }
            }
        }.build()
    }

    class SigningMethodTypeAdapter {
        @ToJson
        fun toJson(src: SigningMethod): List<String> = src.toList().map { it.name }

        @FromJson
        fun fromJson(jsonList: List<String>): SigningMethod {
            val methods = jsonList.mapNotNull {
                try {
                    SigningMethod.Code.valueOf(it)
                } catch (ex: IllegalArgumentException) {
                    null
                }
            }
            return SigningMethod.build(*methods.toTypedArray())
        }
    }

    class WalletSettingsMaskAdapter {
        @ToJson
        fun toJson(src: CardWalletSettingsMask): List<String> = src.toList().map { it.name }

        @FromJson
        fun fromJson(jsonList: List<String>): CardWalletSettingsMask = MaskBuilder().apply {
            jsonList.forEach {
                try {
                    add(CardWalletSettingsMask.Code.valueOf(it))
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
        fun toJson(src: FirmwareVersion): String = src.stringValue

        @FromJson
        fun fromJson(json: String): FirmwareVersion = FirmwareVersion(json)
    }
}
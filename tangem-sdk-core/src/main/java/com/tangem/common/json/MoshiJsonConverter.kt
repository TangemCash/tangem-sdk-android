package com.tangem.common.json

import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.common.MaskBuilder
import com.tangem.common.card.Card
import com.tangem.common.card.CardWallet
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.card.SigningMethod
import com.tangem.common.extensions.guard
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import com.tangem.common.files.DataToWrite
import com.tangem.common.files.FileDataProtectedByPasscode
import com.tangem.common.files.FileDataProtectedBySignature
import com.tangem.operations.PreflightReadMode
import com.tangem.operations.personalization.entities.ProductMask
import java.lang.reflect.ParameterizedType
import java.text.SimpleDateFormat
import java.util.*
import kotlin.reflect.KClass

class MoshiJsonConverter(adapters: List<Any> = listOf(), typedAdapters: Map<Class<*>, JsonAdapter<*>> = mapOf()) {

    val moshi: Moshi = Moshi.Builder().apply {
        adapters.forEach { this.add(it) }
        typedAdapters.forEach { add(it.key, it.value) }
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

        fun default() = MoshiJsonConverter(getTangemSdkAdapters(), getTangemSdkTypedAdapters())

        fun getTangemSdkAdapters(): List<Any> {
            return listOf(
                    TangemSdkAdapter.ByteArrayAdapter(),
                    TangemSdkAdapter.CardSettingsMaskAdapter(),
                    TangemSdkAdapter.ProductMaskAdapter(),
                    TangemSdkAdapter.WalletSettingsMaskAdapter(),
                    TangemSdkAdapter.DateAdapter(),
                    TangemSdkAdapter.FirmwareVersionAdapter(),
                    TangemSdkAdapter.PreflightReadModeAdapter(),
                    TangemSdkAdapter.DataToWriteAdapter(),
            )
        }

        fun getTangemSdkTypedAdapters(): Map<Class<*>, JsonAdapter<*>> {
            val map = mutableMapOf<Class<*>, JsonAdapter<*>>()
            map[FirmwareVersion::class.java] = TangemSdkAdapter.FirmwareVersionAdapter()
            map[SigningMethod::class.java] = TangemSdkAdapter.SigningMethodAdapter()
            return map
        }
    }
}

class TangemSdkAdapter {
    class ByteArrayAdapter {
        @ToJson
        fun toJson(src: ByteArray): String = src.toHexString()

        @FromJson
        fun fromJson(json: String): ByteArray = json.hexToBytes()
    }

    class CardSettingsMaskAdapter {
        @ToJson
        fun toJson(src: Card.SettingsMask): List<String> = src.toList().map { it.name }

        @FromJson
        fun fromJson(jsonList: List<String>): Card.SettingsMask = MaskBuilder().apply {
            jsonList.forEach {
                try {
                    add(Card.SettingsMask.Code.valueOf(it))
                } catch (ex: IllegalArgumentException) {
                    ex.printStackTrace()
                }
            }
        }.build()
    }

    class ProductMaskAdapter {
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

    class WalletSettingsMaskAdapter {
        @ToJson
        fun toJson(src: CardWallet.SettingsMask): List<String> = src.toList().map { it.name }

        @FromJson
        fun fromJson(jsonList: List<String>): CardWallet.SettingsMask = MaskBuilder().apply {
            jsonList.forEach {
                try {
                    add(CardWallet.SettingsMask.Code.valueOf(it))
                } catch (ex: IllegalArgumentException) {
                    ex.printStackTrace()
                }
            }
        }.build()
    }

    class PreflightReadModeAdapter {
        @ToJson
        fun toJson(src: PreflightReadMode): String {
            return when (src) {
                PreflightReadMode.FullCardRead -> "FullCardRead"
                PreflightReadMode.None -> "None"
                PreflightReadMode.ReadCardOnly -> "ReadCardOnly"
                is PreflightReadMode.ReadWallet -> src.publicKey.toHexString()
            }
        }

        @FromJson
        fun fromJson(json: String): PreflightReadMode {
            return when (json) {
                "FullCardRead" -> PreflightReadMode.FullCardRead
                "None" -> PreflightReadMode.None
                "ReadCardOnly" -> PreflightReadMode.ReadCardOnly
                else -> {
                    if (json.length == 64) PreflightReadMode.ReadWallet(json.hexToBytes())
                    else throw java.lang.IllegalArgumentException()
                }
            }
        }
    }

    class DateAdapter {
        private val dateFormatter = SimpleDateFormat("yyyy-MM-dd")

        @ToJson
        fun toJson(src: Date): String {
            return dateFormatter.format(src).toString()
        }

        @FromJson
        fun fromJson(json: String): Date = dateFormatter.parse(json)
    }

    class FirmwareVersionAdapter : JsonAdapter<FirmwareVersion>() {
        @FromJson
        override fun fromJson(reader: JsonReader): FirmwareVersion {
            var version: FirmwareVersion? = null
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "stringValue" -> version = FirmwareVersion(reader.nextString())
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            return version ?: throw java.lang.IllegalArgumentException()
        }

        @ToJson
        override fun toJson(writer: JsonWriter, value: FirmwareVersion?) {
            writer.beginObject();
            value?.major?.let { writer.name("major").value(it) }
            value?.minor?.let { writer.name("minor").value(it) }
            value?.patch?.let { writer.name("patch").value(it) }
            value?.stringValue?.let { writer.name("stringValue").value(it) }
            value?.type?.let { writer.name("type").value(it.rawValue) }
            writer.endObject()
        }
    }

    class SigningMethodAdapter : JsonAdapter<SigningMethod>() {
        override fun fromJson(reader: JsonReader): SigningMethod? {
            try {
                reader.beginArray()
                val list = mutableListOf<String>()
                while (reader.hasNext()) {
                    list.add(reader.nextString())
                }
                reader.endArray()
                val methods = list.mapNotNull {
                    try {
                        SigningMethod.Code.valueOf(it)
                    } catch (ex: IllegalArgumentException) {
                        null
                    }
                }
                return SigningMethod.build(*methods.toTypedArray())
            } catch (ex: Exception) {
                //Is not an array... Try parse as rawValue
            }

            return SigningMethod(reader.nextInt())
        }

        override fun toJson(writer: JsonWriter, value: SigningMethod?) {
            val value = value.guard {
                writer.beginObject()
                writer.endObject()
                return
            }

            writer.beginArray()
            value.toList().forEach { writer.value(it.name) }
            writer.endArray()
        }
    }

    class DataToWriteAdapter {
        @ToJson
        fun toJson(src: DataToWrite): String {
            return when (src) {
                is FileDataProtectedBySignature -> DataProtectedBySignatureAdapter().toJson(src)
                is FileDataProtectedByPasscode -> DataProtectedByPasscodeAdapter().toJson(src)
                else -> throw UnsupportedOperationException()
            }
        }

        @FromJson
        fun fromJson(map: MutableMap<String, Any>): DataToWrite {
            return if (map.containsKey("startingSignature")) {
                DataProtectedBySignatureAdapter().fromJson(map)
            } else {
                DataProtectedByPasscodeAdapter().fromJson(map)
            }
        }
    }

    class DataProtectedBySignatureAdapter {
        @ToJson
        fun toJson(src: FileDataProtectedBySignature): String = MoshiJsonConverter.default().toJson(src)

        @FromJson
        fun fromJson(map: MutableMap<String, Any>): FileDataProtectedBySignature {
            val converter = MoshiJsonConverter.default()
            return converter.fromJson(converter.toJson(map))!!
        }
    }

    class DataProtectedByPasscodeAdapter {
        @ToJson
        fun toJson(src: FileDataProtectedByPasscode): String = MoshiJsonConverter.default().toJson(src)

        @FromJson
        fun fromJson(map: MutableMap<String, Any>): FileDataProtectedByPasscode {
            val converter = MoshiJsonConverter.default()
            return converter.fromJson(converter.toJson(map))!!
        }
    }
}
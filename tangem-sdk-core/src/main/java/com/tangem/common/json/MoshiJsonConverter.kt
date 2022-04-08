package com.tangem.common.json

import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.common.MaskBuilder
import com.tangem.common.card.*
import com.tangem.common.extensions.guard
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import com.tangem.common.hdWallet.DerivationNode
import com.tangem.common.hdWallet.DerivationPath
import com.tangem.common.hdWallet.bip.BIP44
import com.tangem.operations.PreflightReadMode
import com.tangem.operations.attestation.Attestation
import com.tangem.operations.attestation.AttestationTask
import com.tangem.operations.files.FileDataMode
import com.tangem.operations.files.FileToWrite
import com.tangem.operations.files.FileVisibility
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
        val INSTANCE: MoshiJsonConverter = default()

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
                TangemSdkAdapter.EllipticCurveAdapter(),
                TangemSdkAdapter.LinkedTerminalStatusAdapter(),
                TangemSdkAdapter.CardStatusAdapter(),
                TangemSdkAdapter.CardWalletStatusAdapter(),
                TangemSdkAdapter.CardSettingsMaskCodeAdapter(),
                TangemSdkAdapter.CardWalletSettingsMaskCodeAdapter(),
                TangemSdkAdapter.SigningMethodCodeAdapter(),
                TangemSdkAdapter.ProductMaskCodeAdapter(),
                TangemSdkAdapter.EncryptionModeAdapter(),
                TangemSdkAdapter.FirmwareTypeAdapter(),
                TangemSdkAdapter.FileToWriteAdapter(),
                TangemSdkAdapter.FileDataModeAdapter(),
                TangemSdkAdapter.FilesVisibilityAdapter(),
                TangemSdkAdapter.AttestationStatusAdapter(),
                TangemSdkAdapter.AttestationModeAdapter(),
                TangemSdkAdapter.BIP44ChainAdapter(),
                TangemSdkAdapter.BackupStatusAdapter(),
                TangemSdkAdapter.DerivationPathAdapter(),
                TangemSdkAdapter.DerivationNodeAdapter(),
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
                PreflightReadMode.FullCardRead -> "fullCardRead"
                PreflightReadMode.ReadCardOnly -> "readCardOnly"
                PreflightReadMode.None -> "none"
            }
        }

        @FromJson
        fun fromJson(json: String): PreflightReadMode {
            return when (json) {
                "fullCardRead" -> PreflightReadMode.FullCardRead
                "readCardOnly" -> PreflightReadMode.ReadCardOnly
                "none" -> PreflightReadMode.None
                else -> throw java.lang.IllegalArgumentException()
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

    class FileToWriteAdapter {
        @ToJson
        fun toJson(src: FileToWrite): String {
            return when (src) {
                is FileToWrite.ByFileOwner -> ByFileOwnerAdapter().toJson(src)
                is FileToWrite.ByUser -> ByUserAdapter().toJson(src)
            }
        }

        @FromJson
        fun fromJson(map: MutableMap<String, Any>): FileToWrite {
            return if (map.containsKey("startingSignature")) {
                ByFileOwnerAdapter().fromJson(map)
            } else {
                ByUserAdapter().fromJson(map)
            }
        }

        class ByUserAdapter {
            @ToJson
            fun toJson(src: FileToWrite.ByUser): String = MoshiJsonConverter.default().toJson(src)

            @FromJson
            fun fromJson(map: MutableMap<String, Any>): FileToWrite.ByUser {
                val converter = MoshiJsonConverter.default()
                return converter.fromJson(converter.toJson(map))!!
            }
        }

        class ByFileOwnerAdapter {
            @ToJson
            fun toJson(src: FileToWrite.ByFileOwner): String = MoshiJsonConverter.default().toJson(src)

            @FromJson
            fun fromJson(map: MutableMap<String, Any>): FileToWrite.ByFileOwner {
                val converter = MoshiJsonConverter.default()
                return converter.fromJson(converter.toJson(map))!!
            }
        }
    }

    class FilesVisibilityAdapter {
        @ToJson
        fun toJson(src: FileVisibility): String = EnumConverter.toJson(src)

        @FromJson
        fun fromJson(json: String): FileVisibility = EnumConverter.toEnum(json)
    }

    class DerivationPathAdapter {
        @ToJson
        fun toJson(src: DerivationPath): String {
            val map = mapOf(
                "rawPath" to src.rawPath,
                "nodes" to src.nodes.map { it.index }
            )
            return MoshiJsonConverter.default().toJson(map)
        }

        @FromJson
        fun fromJson(json: String): DerivationPath {
            val map = MoshiJsonConverter.default().toMap(json)
            val rawPath = map["rawPath"] as String
            val nodeIndexes = map["nodes"] as List<Number>
            val nodes = nodeIndexes.map { DerivationNode.fromIndex(it.toLong()) }
            return DerivationPath(rawPath, nodes)
        }
    }

    class DerivationNodeAdapter {
        @ToJson
        fun toJson(src: DerivationNode): String {
            return when (src) {
                is DerivationNode.Hardened -> HardenedNodeAdapter().toJson(src)
                is DerivationNode.NonHardened -> NonHardenedNodeAdapter().toJson(src)
            }
        }

        @FromJson
        fun fromJson(map: MutableMap<String, Any>): DerivationNode {
            val index = map["index"] as Number
            return DerivationNode.fromIndex(index.toLong())
        }

        class HardenedNodeAdapter {
            @ToJson
            fun toJson(src: DerivationNode.Hardened): String {
                return MoshiJsonConverter.default().toJson(src)
            }

            @FromJson
            fun fromJson(map: MutableMap<String, Any>): DerivationNode.Hardened {
                val converter = MoshiJsonConverter.default()
                return converter.fromJson(converter.toJson(map))!!
            }
        }

        class NonHardenedNodeAdapter {
            @ToJson
            fun toJson(src: DerivationNode.NonHardened): String {
                return MoshiJsonConverter.default().toJson(src)
            }

            @FromJson
            fun fromJson(map: MutableMap<String, Any>): DerivationNode.NonHardened {
                val converter = MoshiJsonConverter.default()
                return converter.fromJson(converter.toJson(map))!!
            }
        }
    }

    class EllipticCurveAdapter {
        @ToJson
        fun toJson(src: EllipticCurve): String = EnumConverter.toJson(src)

        @FromJson
        fun fromJson(json: String): EllipticCurve = EnumConverter.toEnum(json)
    }

    class LinkedTerminalStatusAdapter {
        @ToJson
        fun toJson(src: Card.LinkedTerminalStatus): String = EnumConverter.toJson(src)

        @FromJson
        fun fromJson(json: String): Card.LinkedTerminalStatus = EnumConverter.toEnum(json)
    }

    class CardStatusAdapter {
        @ToJson
        fun toJson(src: Card.Status): String = EnumConverter.toJson(src)

        @FromJson
        fun fromJson(json: String): Card.Status = EnumConverter.toEnum(json)
    }

    class BackupStatusAdapter {
        @ToJson
        fun toJson(src: Card.BackupStatus): String = EnumConverter.toJson(src.toRawStatus())

        @FromJson
        fun fromJson(json: String): Card.BackupStatus =
                Card.BackupStatus.from(EnumConverter.toEnum<Card.BackupRawStatus>(json)) ?: Card.BackupStatus.NoBackup
    }

    class CardWalletStatusAdapter {
        @ToJson
        fun toJson(src: CardWallet.Status): String = EnumConverter.toJson(src)

        @FromJson
        fun fromJson(json: String): CardWallet.Status = EnumConverter.toEnum(json)
    }

    class CardSettingsMaskCodeAdapter {
        @ToJson
        fun toJson(src: Card.SettingsMask.Code): String = EnumConverter.toJson(src)

        @FromJson
        fun fromJson(json: String): Card.SettingsMask.Code = EnumConverter.toEnum(json)
    }

    class CardWalletSettingsMaskCodeAdapter {
        @ToJson
        fun toJson(src: CardWallet.SettingsMask.Code): String = EnumConverter.toJson(src)

        @FromJson
        fun fromJson(json: String): CardWallet.SettingsMask.Code = EnumConverter.toEnum(json)
    }

    class SigningMethodCodeAdapter {
        @ToJson
        fun toJson(src: SigningMethod.Code): String = EnumConverter.toJson(src)

        @FromJson
        fun fromJson(json: String): SigningMethod.Code = EnumConverter.toEnum(json)
    }

    class ProductMaskCodeAdapter {
        @ToJson
        fun toJson(src: ProductMask.Code): String = EnumConverter.toJson(src)

        @FromJson
        fun fromJson(json: String): ProductMask.Code = EnumConverter.toEnum(json)
    }

    class EncryptionModeAdapter {
        @ToJson
        fun toJson(src: EncryptionMode): String = EnumConverter.toJson(src)

        @FromJson
        fun fromJson(json: String): EncryptionMode = EnumConverter.toEnum(json)
    }

    class FirmwareTypeAdapter {
        @ToJson
        fun toJson(src: FirmwareVersion.FirmwareType): String = EnumConverter.toJson(src)

        @FromJson
        fun fromJson(json: String): FirmwareVersion.FirmwareType = EnumConverter.toEnum(json)
    }

    class FileDataModeAdapter {
        @ToJson
        fun toJson(src: FileDataMode): String = EnumConverter.toJson(src)

        @FromJson
        fun fromJson(json: String): FileDataMode = EnumConverter.toEnum(json)
    }

    class AttestationStatusAdapter {
        @ToJson
        fun toJson(src: Attestation.Status): String = EnumConverter.toJson(src)

        @FromJson
        fun fromJson(json: String): Attestation.Status = EnumConverter.toEnum(json)
    }

    class AttestationModeAdapter {
        @ToJson
        fun toJson(src: AttestationTask.Mode): String = EnumConverter.toJson(src)

        @FromJson
        fun fromJson(json: String): AttestationTask.Mode = EnumConverter.toEnum(json)
    }

    class BIP44ChainAdapter {
        @ToJson
        fun toJson(src: BIP44.Chain): String = EnumConverter.toJson(src)

        @FromJson
        fun fromJson(json: String): BIP44.Chain = EnumConverter.toEnum(json)
    }

    private class EnumConverter {
        companion object {
            inline fun <reified T : Enum<T>> toEnum(json: String): T = enumValueOf(json.capitalize())

            fun toJson(enum: Enum<*>): String = enum.name.decapitalize()
        }
    }
}
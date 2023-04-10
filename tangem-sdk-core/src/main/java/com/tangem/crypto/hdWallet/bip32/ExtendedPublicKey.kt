package com.tangem.crypto.hdWallet.bip32

import com.squareup.moshi.JsonClass
import com.tangem.Log
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.calculateRipemd160
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toByteArray
import com.tangem.common.extensions.toCompressedPublicKey
import com.tangem.common.extensions.toInt
import com.tangem.common.extensions.toLong
import com.tangem.crypto.NetworkType
import com.tangem.crypto.Secp256k1
import com.tangem.crypto.Secp256k1Key
import com.tangem.crypto.decodeBase58WithChecksum
import com.tangem.crypto.encodeToBase58WithChecksum
import com.tangem.crypto.hdWallet.DerivationNode
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.HDWalletError
import com.tangem.crypto.hdWallet.bip32.serialization.ExtendedKeySerializable
import com.tangem.crypto.hdWallet.bip32.serialization.ExtendedKeySerializationError
import com.tangem.crypto.hdWallet.bip32.serialization.ExtendedKeySerializer
import com.tangem.crypto.hmacSha512
import com.tangem.operations.CommandResponse

/**
 * Created by Anton Zhilenkov on 03/08/2021.
 */
@JsonClass(generateAdapter = true)
class ExtendedPublicKey @kotlin.jvm.Throws constructor(
    val publicKey: ByteArray,
    val chainCode: ByteArray,
    val depth: Int = 0,
    val parentFingerprint: ByteArray = "0x00000000".hexToBytes(),
    val childNumber: Long = 0L,
) : CommandResponse, ExtendedKeySerializable {

    init {
        if (depth == 0 && (parentFingerprint.first().toInt() != 0 || childNumber != 0L)) {
            throw ExtendedKeySerializationError.WrongKey
        }
    }

    /**
     * This function performs CKDpub((Kpar, cpar), i) → (Ki, ci) to compute a child extended public key from
     * the parent extended public key.
     * It is only defined for non-hardened child keys. `secp256k1` only
     */
    @Suppress("MagicNumber")
    @Throws(HDWalletError::class)
    fun derivePublicKey(node: DerivationNode): ExtendedPublicKey {
        try {
            Secp256k1Key(publicKey)
        } catch (e: Exception) {
            throw TangemSdkError.UnsupportedCurve()
        }

        val index = node.index

        // We can derive only non-hardened keys
        if (index >= BIP32.Constants.hardenedOffset) throw HDWalletError.HardenedNotSupported

        val data = publicKey.toCompressedPublicKey() + index.toByteArray(4)
        val i = chainCode.hmacSha512(data).clone()
        val iL = i.sliceArray(0 until 32)
        val chainCode = i.sliceArray(32 until 64)

        val ki = Secp256k1.gMultiplyAndAddPoint(iL, publicKey)
        val derivedPublicKey = ki.getEncoded(true)

        return ExtendedPublicKey(
            publicKey = derivedPublicKey,
            chainCode = chainCode,
            depth = depth + 1,
            parentFingerprint = publicKey.calculateSha256().calculateRipemd160().take(4).toByteArray(),
            childNumber = index,
        )
    }

    /**
     * This function performs CKDpub((Kpar, cpar), i) → (Ki, ci) to compute a child extended public key from the
     * parent extended public key.
     * It is only defined for non-hardened child keys. `secp256k1` only
     */
    @Throws(HDWalletError::class)
    fun derivePublicKey(derivationPath: DerivationPath): ExtendedPublicKey {
        var key: ExtendedPublicKey = this
        derivationPath.nodes.forEach {
            key = key.derivePublicKey(it)
        }
        return key
    }

    @Throws(Exception::class)
    override fun serialize(networkType: NetworkType): String {
        val secpKey = try {
            Secp256k1Key(publicKey)
        } catch (e: Exception) {
            throw TangemSdkError.UnsupportedCurve()
        }

        val compressedKey = secpKey.compress()
        val version = ExtendedKeySerializer.Version.Public
        val prefix = version.getPrefix(networkType)

        val data = prefix.toByteArray(size = 4) +
            byteArrayOf(depth.toByte()) +
            parentFingerprint +
            childNumber.toByteArray(size = 4) +
            chainCode +
            compressedKey

        if (data.size != ExtendedKeySerializer.Constants.DATA_LENGTH) {
            throw ExtendedKeySerializationError.WrongLength
        }

        return data.encodeToBase58WithChecksum()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ExtendedPublicKey

        if (!publicKey.contentEquals(other.publicKey)) return false
        if (!chainCode.contentEquals(other.chainCode)) return false
        if (depth != other.depth) return false
        if (!parentFingerprint.contentEquals(other.parentFingerprint)) return false
        if (childNumber != other.childNumber) return false

        return true
    }

    override fun hashCode(): Int {
        var result = publicKey.contentHashCode()
        result = 31 * result + chainCode.contentHashCode()
        result = 31 * result + depth
        result = 31 * result + parentFingerprint.contentHashCode()
        result = 31 * result + childNumber.hashCode()
        return result
    }

    companion object {
        @Suppress("MagicNumber")
        fun from(extendedKeyString: String, networkType: NetworkType): ExtendedPublicKey {
            val data = try {
                extendedKeyString.decodeBase58WithChecksum()
            } catch (e: Exception) {
                Log.error { e.stackTraceToString() }
                throw ExtendedKeySerializationError.DecodingFailed
            }

            if (data.size != ExtendedKeySerializer.Constants.DATA_LENGTH) {
                throw ExtendedKeySerializationError.WrongLength
            }

            val decodedVersion = data.copyOfRange(0, 4).toInt()
            val version = ExtendedKeySerializer.Version.Public
            val prefix = version.getPrefix(networkType)

            if (decodedVersion != prefix.toInt()) {
                throw ExtendedKeySerializationError.WrongVersion
            }

            val depth = data[4].toInt()
            val parentFingerprint = data.copyOfRange(5, 9)
            val childNumber = data.copyOfRange(9, 13).toLong()
            val chainCode = data.copyOfRange(13, 45)
            val compressedKey = data.copyOfRange(45, 78)

            try {
                Secp256k1Key(compressedKey)
            } catch (e: Exception) {
                throw TangemSdkError.UnsupportedCurve()
            }

            return ExtendedPublicKey(
                publicKey = compressedKey,
                chainCode = chainCode,
                depth = depth,
                parentFingerprint = parentFingerprint,
                childNumber = childNumber,
            )
        }
    }
}

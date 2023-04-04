package com.tangem.crypto

class Secp256k1Key(private val secp256k1PubKey: ByteArray) {

    @Throws
    fun compress(): ByteArray {
        return CryptoUtils.compressPublicKey(secp256k1PubKey)
    }

    @Throws
    fun decompress(): ByteArray {
        return CryptoUtils.decompressPublicKey(secp256k1PubKey)
    }
}

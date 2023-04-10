package com.tangem.crypto

class Secp256k1Key {

    private val secp256k1PubKey: ByteArray

    @Throws
    constructor(secp256k1PubKey: ByteArray) {
        CryptoUtils.loadPublicKey(secp256k1PubKey)
        this.secp256k1PubKey = secp256k1PubKey
    }

    @Throws
    fun compress(): ByteArray {
        return CryptoUtils.compressPublicKey(secp256k1PubKey)
    }

    @Throws
    fun decompress(): ByteArray {
        return CryptoUtils.decompressPublicKey(secp256k1PubKey)
    }
}

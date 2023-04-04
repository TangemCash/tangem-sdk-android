package com.tangem.common.services.secure

import com.tangem.common.KeyPair
import com.tangem.crypto.Secp256k1
import com.tangem.crypto.generateKeyPair

/**
 * Created by Anton Zhilenkov on 26/07/2021.
 */
internal class SecureService(
    private val storage: SecureStorage,
) {
    private val enclavePublic = "enclavePublic"
    private val enclavePrivate = "enclavePrivate"

    private val keyPair: KeyPair by lazy { makeOrRestoreKey() }

    fun sign(data: ByteArray): ByteArray {
        return Secp256k1.sign(data, keyPair.privateKey)
    }

    fun verify(signature: ByteArray, message: ByteArray): Boolean {
        return Secp256k1.verify(keyPair.publicKey, message, signature)
    }

    private fun makeOrRestoreKey(): KeyPair {
        val publicKey = storage.readKey(enclavePublic)
        val privateKey = storage.readKey(enclavePrivate)
        return if (publicKey != null && privateKey != null) {
            KeyPair(publicKey, privateKey)
        } else {
            val key = Secp256k1.generateKeyPair()
            storage.storeKey(key.publicKey, enclavePublic)
            storage.storeKey(key.privateKey, enclavePrivate)
            key
        }
    }
}

package com.tangem.tangem_sdk_new.storage

import android.content.SharedPreferences
import com.tangem.common.KeyPair
import com.tangem.common.services.secure.SecureStorage
import com.tangem.common.services.secure.TerminalKeysService
import com.tangem.crypto.Secp256k1
import com.tangem.crypto.generateKeyPair

/**
 * Service for managing Terminal keypair, used for Linked Terminal feature.
 * Needs to be provided to [com.tangem.TangemSdk] by calling [com.tangem.TangemSdk.setTerminalKeysService]
 * Linked Terminal feature can be disabled manually by editing [com.tangem.Config].
 * @param context is required to retrieve an instance of [SharedPreferences]
 */
class TerminalKeysStorage(
    private val secureStorage: SecureStorage
) : TerminalKeysService {
    private val terminalPublicKey = "terminalPublicKey"
    private val terminalPrivateKey = "terminalPrivateKey"

    /**
     * Retrieves generated keys from encrypted shared preferences if the keys exist.
     * Generates new and stores them in encrypted shared preferences otherwise
     */
    override fun getKeys(): KeyPair {
        val privateKey = secureStorage.get(terminalPrivateKey)
        val publicKey = secureStorage.get(terminalPublicKey)
        return if (privateKey == null || publicKey == null) {
            generateAndSaveKeys()
        } else {
            KeyPair(publicKey, privateKey)
        }
    }

    private fun generateAndSaveKeys(): KeyPair {
        val keyPair = Secp256k1.generateKeyPair()
        secureStorage.store(keyPair.privateKey, terminalPrivateKey)
        secureStorage.store(keyPair.publicKey, terminalPublicKey)
        return keyPair
    }
}
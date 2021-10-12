package com.tangem.crypto

import com.tangem.common.card.EncryptionMode
import org.spongycastle.jce.interfaces.ECPublicKey
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import javax.crypto.KeyAgreement

interface EncryptionHelper {
    val keyA: ByteArray

    fun generateSecret(keyB: ByteArray): ByteArray

    companion object {
        fun create(encryptionMode: EncryptionMode): EncryptionHelper? {
            return when (encryptionMode) {
                EncryptionMode.None -> null
                EncryptionMode.Fast -> FastEncryptionHelper()
                EncryptionMode.Strong -> StrongEncryptionHelper()
            }
        }
    }
}

class StrongEncryptionHelper : EncryptionHelper {
    private val keyPair = generateKeyPair()
    private val keyAgreement = generateKeyAgreement(keyPair)
    override val keyA = provideKeyA(keyPair)

    override fun generateSecret(keyB: ByteArray): ByteArray {
        keyAgreement.doPhase(CryptoUtils.loadPublicKey(keyB), true)
        return keyAgreement.generateSecret()
    }

    private fun generateKeyPair(): KeyPair {
        val kpgen = KeyPairGenerator.getInstance("ECDH", "SC")
        kpgen.initialize(ECGenParameterSpec("secp256k1"), SecureRandom())
        return kpgen.generateKeyPair()
    }

    private fun generateKeyAgreement(keyPair: KeyPair): KeyAgreement {
        val keyAgreement = KeyAgreement.getInstance("ECDH", "SC")
        keyAgreement.init(keyPair.private)
        return keyAgreement
    }

    private fun provideKeyA(keyPair: KeyPair): ByteArray {
        val eckey = keyPair.public as ECPublicKey
        return eckey.q.getEncoded(false)
    }
}

class FastEncryptionHelper : EncryptionHelper {
    override val keyA = CryptoUtils.generateRandomBytes(16)

    override fun generateSecret(keyB: ByteArray): ByteArray {
        return keyA + keyB
    }
}
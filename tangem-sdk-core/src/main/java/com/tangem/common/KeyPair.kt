package com.tangem.common

import com.tangem.common.card.EllipticCurve
import com.tangem.crypto.CryptoUtils

/**
 * Pair of private and public key
 */
class KeyPair(
    val publicKey: ByteArray,
    val privateKey: ByteArray,
) {

    constructor(
        privateKey: ByteArray,
        curve: EllipticCurve = EllipticCurve.Secp256k1,
    ) : this(CryptoUtils.generatePublicKey(privateKey, curve), privateKey)
}

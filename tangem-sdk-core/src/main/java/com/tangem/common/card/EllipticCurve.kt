package com.tangem.common.card

/**
 * Elliptic curve used for wallet key operations.
 */
enum class EllipticCurve(val curve: String) {
    Secp256k1("secp256k1"),
    Secp256r1("secp256r1"),
    Ed25519("ed25519"),
    Bls12381_G2("bls12381_G2"),
    Bls12381_G2_AUG("bls12381_G2_AUG"),
    Bls12381_G2_POP("bls12381_G2_POP");

    companion object {
        private val values = values()
        fun byName(curve: String): EllipticCurve? = values.find { it.curve == curve }
    }
}
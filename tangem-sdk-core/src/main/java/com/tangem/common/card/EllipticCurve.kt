package com.tangem.common.card

import com.google.gson.annotations.SerializedName

/**
 * Elliptic curve used for wallet key operations.
 */
enum class EllipticCurve(val curve: String) {
    @SerializedName(value = "secp256k1")
    Secp256k1("secp256k1"),

    @SerializedName(value = "secp256r1")
    Secp256r1("secp256r1"),

    @SerializedName(value = "ed25519")
    Ed25519("ed25519");

    companion object {
        private val values = values()
        fun byName(curve: String): EllipticCurve? = values.find { it.curve == curve }
    }
}
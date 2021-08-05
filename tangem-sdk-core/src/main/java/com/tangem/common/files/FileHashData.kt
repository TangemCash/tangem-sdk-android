package com.tangem.common.files

data class FileHashData(
    val startingHash: ByteArray,
    val finalizingHash: ByteArray,
    val startingSignature: ByteArray? = null,
    val finalizingSignature: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FileHashData

        if (!startingHash.contentEquals(other.startingHash)) return false
        if (!finalizingHash.contentEquals(other.finalizingHash)) return false
        if (startingSignature != null) {
            if (other.startingSignature == null) return false
            if (!startingSignature.contentEquals(other.startingSignature)) return false
        } else if (other.startingSignature != null) return false
        if (finalizingSignature != null) {
            if (other.finalizingSignature == null) return false
            if (!finalizingSignature.contentEquals(other.finalizingSignature)) return false
        } else if (other.finalizingSignature != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = startingHash.contentHashCode()
        result = 31 * result + finalizingHash.contentHashCode()
        result = 31 * result + (startingSignature?.contentHashCode() ?: 0)
        result = 31 * result + (finalizingSignature?.contentHashCode() ?: 0)
        return result
    }
}
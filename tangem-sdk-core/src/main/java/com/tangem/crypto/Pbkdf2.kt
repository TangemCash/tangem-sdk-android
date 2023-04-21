package com.tangem.crypto

import org.spongycastle.crypto.CipherParameters
import org.spongycastle.crypto.digests.SHA256Digest
import org.spongycastle.crypto.macs.HMac
import org.spongycastle.crypto.params.KeyParameter
import java.security.InvalidKeyException
import java.util.*
import kotlin.experimental.xor
import kotlin.math.min
import kotlin.math.pow

@Suppress("MagicNumber", "VariableNaming")
class Pbkdf2 {
    private val F: HMac = HMac(SHA256Digest())

    fun deriveKey(password: ByteArray, salt: ByteArray, iterations: Int): ByteArray {
        val macSize = F.macSize
        // Check key length
        if (macSize > (2.0.pow(32.0) - 1) * macSize) throw InvalidKeyException("Derived key to long")

        val derivedKey = ByteArray(macSize)

        val J = 0
        val K: Int = macSize
        val U: Int = macSize shl 1
        val B = K + U
        val workingArray = ByteArray(K + U + 4)

        // Initialize F
        val macParams: CipherParameters = KeyParameter(password)
        F.init(macParams)

        // Perform iterations
        var kpos = 0
        var blk = 1
        while (kpos < macSize) {
            storeInt32BE(blk, workingArray, B)
            F.update(salt, 0, salt.size)
            F.reset()
            F.update(salt, 0, salt.size)
            F.update(workingArray, B, 4)
            F.doFinal(workingArray, U)
            System.arraycopy(workingArray, U, workingArray, J, K)
            var i = 1
            var j = J
            var k = K
            while (i < iterations) {
                F.init(macParams)
                F.update(workingArray, j, K)
                F.doFinal(workingArray, k)
                var u = U
                var v = k
                while (u < B) {
                    workingArray[u] = workingArray[u] xor workingArray[v]
                    u++
                    v++
                }
                val swp = k
                k = j
                j = swp
                i++
            }
            val tocpy = min(macSize - kpos, K)
            System.arraycopy(workingArray, U, derivedKey, kpos, tocpy)
            kpos += K
            blk++
        }
        Arrays.fill(workingArray, 0.toByte())
        return derivedKey
    }

    /**
     * Convert a 32-bit integer value into a big-endian byte array
     *
     * @param value  The integer value to convert
     * @param bytes  The byte array to store the converted value
     * @param offSet The offset in the output byte array
     */
    private fun storeInt32BE(value: Int, bytes: ByteArray, offSet: Int) {
        bytes[offSet + 3] = value.toByte()
        bytes[offSet + 2] = (value ushr 8).toByte()
        bytes[offSet + 1] = (value ushr 16).toByte()
        bytes[offSet] = (value ushr 24).toByte()
    }
}

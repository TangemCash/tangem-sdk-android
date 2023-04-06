package com.tangem.crypto

import org.spongycastle.crypto.CipherParameters
import org.spongycastle.crypto.digests.SHA256Digest
import org.spongycastle.crypto.macs.HMac
import org.spongycastle.crypto.params.KeyParameter
import java.security.InvalidKeyException
import java.util.Arrays
import kotlin.experimental.xor
import kotlin.math.min
import kotlin.math.pow

class Pbkdf2(private val hMac: HMac = HMac(SHA256Digest())) {

    @Suppress("MagicNumber")
    fun deriveKey(password: ByteArray, salt: ByteArray, iterations: Int): ByteArray {
        val macSize = hMac.macSize
        // Check key length
        if (macSize > (2.0.pow(32.0) - 1) * macSize) throw InvalidKeyException("Derived key to long")

        val derivedKey = ByteArray(macSize)

        val j = 0
        val k: Int = macSize
        val u: Int = macSize shl 1
        val b = k + u
        val workingArray = ByteArray(k + u + 4)

        // Initialize F
        val macParams: CipherParameters = KeyParameter(password)
        hMac.init(macParams)

        // Perform iterations
        var kpos = 0
        var blk = 1
        while (kpos < macSize) {
            storeInt32BE(blk, workingArray, b)
            hMac.update(salt, 0, salt.size)
            hMac.reset()
            hMac.update(salt, 0, salt.size)
            hMac.update(workingArray, b, 4)
            hMac.doFinal(workingArray, u)
            System.arraycopy(workingArray, u, workingArray, j, k)
            var i = 1
            var j = j
            var k = k
            while (i < iterations) {
                hMac.init(macParams)
                hMac.update(workingArray, j, k)
                hMac.doFinal(workingArray, k)
                var u = u
                var v = k
                while (u < b) {
                    workingArray[u] = workingArray[u] xor workingArray[v]
                    u++
                    v++
                }
                val swp = k
                k = j
                j = swp
                i++
            }
            val tocpy = min(macSize - kpos, k)
            System.arraycopy(workingArray, u, derivedKey, kpos, tocpy)
            kpos += k
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
    @Suppress("MagicNumber")
    private fun storeInt32BE(value: Int, bytes: ByteArray, offSet: Int) {
        bytes[offSet + 3] = value.toByte()
        bytes[offSet + 2] = (value ushr 8).toByte()
        bytes[offSet + 1] = (value ushr 16).toByte()
        bytes[offSet] = (value ushr 24).toByte()
    }
}

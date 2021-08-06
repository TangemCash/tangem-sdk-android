package com.tangem.crypto

import com.tangem.common.extensions.toHexString
import org.spongycastle.asn1.ASN1EncodableVector
import org.spongycastle.asn1.ASN1Integer
import org.spongycastle.asn1.DERSequence
import org.spongycastle.asn1.x9.X9ECParameters
import org.spongycastle.crypto.ec.CustomNamedCurves
import org.spongycastle.jce.ECNamedCurveTable
import org.spongycastle.jce.spec.ECPrivateKeySpec
import org.spongycastle.jce.spec.ECPublicKeySpec
import org.spongycastle.math.ec.ECPoint
import java.math.BigInteger
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature

object Secp256k1 {

    internal fun compressPublicKey(key: ByteArray): ByteArray = if (key.size == 65) {
        val spec = ECNamedCurveTable.getParameterSpec("secp256k1")
        val publicKeyPoint = spec.curve.decodePoint(key)
        publicKeyPoint.getEncoded(true)
    } else {
        key
    }

    internal fun decompressPublicKey(key: ByteArray): ByteArray = if (key.size == 33) {
        val spec = ECNamedCurveTable.getParameterSpec("secp256k1")
        val publicKeyPoint = spec.curve.decodePoint(key)
        publicKeyPoint.getEncoded(false)
    } else {
        key
    }

    internal fun verify(publicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean {
        val signatureInstance = Signature.getInstance("SHA256withECDSA")
        val loadedPublicKey = loadPublicKey(publicKey)
        signatureInstance.initVerify(loadedPublicKey)
        signatureInstance.update(message)

        val v = ASN1EncodableVector()
        val size = signature.size / 2
        v.add(calculateR(signature, size))
        v.add(calculateS(signature, size))
        val sigDer = DERSequence(v).encoded

        return signatureInstance.verify(sigDer)
    }

    internal fun loadPublicKey(publicKeyArray: ByteArray): PublicKey {
        val spec = ECNamedCurveTable.getParameterSpec("secp256k1")
        val factory = KeyFactory.getInstance("EC", "SC")

        val p1 = spec.curve.decodePoint(publicKeyArray)
        val keySpec = ECPublicKeySpec(p1, spec)

        return factory.generatePublic(keySpec)
    }

    private fun calculateR(signature: ByteArray, size: Int): ASN1Integer =
            ASN1Integer(BigInteger(1, signature.copyOfRange(0, size)))

    private fun calculateS(signature: ByteArray, size: Int): ASN1Integer =
            ASN1Integer(BigInteger(1, signature.copyOfRange(size, size * 2)))


    internal fun sign(data: ByteArray, privateKeyArray: ByteArray): ByteArray {
        val spec = ECNamedCurveTable.getParameterSpec("secp256k1")
        val factory = KeyFactory.getInstance("EC", "SC")

        val keySpecP = ECPrivateKeySpec(BigInteger(1, privateKeyArray), spec)

        val signature = Signature.getInstance("SHA256withECDSA")

        val privateKey = factory.generatePrivate(keySpecP)
        signature.initSign(privateKey)
        signature.update(data)

        val enc = signature.sign()
        checkSignatureForErrors(enc)

        val res = toByte64(enc)

        if (!verify(generatePublicKey(privateKeyArray), data, res)) {
            throw Exception("Signature self verify failed - ,enc:" + enc.toHexString() + ",res:" + res.toHexString())
        }

        return res
    }

    private fun checkSignatureForErrors(enc: ByteArray) {
        if (enc[0].toInt() != 0x30) throw Exception("bad encoding 1")
        if (enc[1].toInt() and 0x80 != 0) throw Exception("unsupported length encoding 1")
        if (enc[2].toInt() != 0x02) throw Exception("bad encoding 2")
        if (enc[3].toInt() and 0x80 != 0) throw Exception("unsupported length encoding 2")
        var rLength = enc[3].toInt()
        if (enc[4 + rLength].toInt() != 0x02) throw Exception("bad encoding 3")
        if (enc[5 + rLength].toInt() and 0x80 != 0)
            throw Exception("unsupported length encoding 3")
    }

    private fun toByte64(enc: ByteArray): ByteArray {
        var rLength = enc[3].toInt()
        var sLength = enc[5 + rLength].toInt()

        val sPos = 6 + rLength
        val res = ByteArray(64)
        if (rLength <= 32) {
            System.arraycopy(enc, 4, res, 32 - rLength, rLength)
            rLength = 32
        } else if (rLength == 33 && enc[4].toInt() == 0) {
            rLength--
            System.arraycopy(enc, 5, res, 0, rLength)
        } else {
            throw Exception("unsupported r-length - r-length:" + rLength.toString() + ",s-length:" + sLength.toString() + ",enc:" + enc.toHexString())
        }
        if (sLength <= 32) {
            System.arraycopy(enc, sPos, res, rLength + 32 - sLength, sLength)
            sLength = 32
        } else if (sLength == 33 && enc[sPos].toInt() == 0) {
            System.arraycopy(enc, sPos + 1, res, rLength, sLength - 1)
        } else {
            throw Exception("unsupported s-length - r-length:" + rLength.toString() + ",s-length:" + sLength.toString() + ",enc:" + enc.toHexString())
        }

        return res
    }

    internal fun generatePublicKey(privateKeyArray: ByteArray, compressed: Boolean = false): ByteArray {
        return multiply(privateKeyArray).getEncoded(compressed)
    }

    internal fun gMultiplyAndAddPoint(key: ByteArray, encodedPoint: ByteArray): ECPoint {
        val x9ECParameters = CustomNamedCurves.getByName("secp256k1")
        val multiplied = multiply(key, x9ECParameters)
        val decoded = x9ECParameters.curve.decodePoint(encodedPoint)
        return multiplied.add(decoded)
    }

    private fun multiply(privateKeyArray: ByteArray, curveSpec: X9ECParameters? = null): ECPoint {
        val spec = curveSpec ?: CustomNamedCurves.getByName("secp256k1")
        return spec.g.multiply(BigInteger(1, privateKeyArray))
    }
}
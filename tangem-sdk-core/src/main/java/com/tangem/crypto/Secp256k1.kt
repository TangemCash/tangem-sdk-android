package com.tangem.crypto

import com.tangem.common.extensions.toHexString
import org.spongycastle.asn1.ASN1EncodableVector
import org.spongycastle.asn1.ASN1Integer
import org.spongycastle.asn1.DERSequence
import org.spongycastle.jce.ECNamedCurveTable
import org.spongycastle.jce.spec.ECParameterSpec
import org.spongycastle.jce.spec.ECPrivateKeySpec
import org.spongycastle.jce.spec.ECPublicKeySpec
import org.spongycastle.math.ec.ECPoint
import java.math.BigInteger
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature

object Secp256k1 {

    fun sum(firstPublicKeyArray: ByteArray, secondPublicKeyArray: ByteArray): ByteArray {
        val ecSpec = createECSpec()
        val q1 = ecSpec.curve.decodePoint(firstPublicKeyArray)
        val q2 = ecSpec.curve.decodePoint(secondPublicKeyArray)

        return q1.add(q2).getEncoded(true)
    }

    internal fun sign(data: ByteArray, privateKeyArray: ByteArray): ByteArray {
        val privateKeySpec = ECPrivateKeySpec(BigInteger(1, privateKeyArray), createECSpec())
        val privateKey = createKeyFactory().generatePrivate(privateKeySpec)

        val signatureInstance = Signature.getInstance("SHA256withECDSA", "SC")
        signatureInstance.initSign(privateKey)
        signatureInstance.update(data)

        val enc = signatureInstance.sign()
        checkSignatureForErrors(enc)

        val res = toByte64(enc)

        if (!verify(generatePublicKey(privateKeyArray), data, res)) {
            error("Signature self verify failed - ,enc:" + enc.toHexString() + ",res:" + res.toHexString())
        }

        return res
    }

    internal fun verify(publicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean {
        val signatureInstance = Signature.getInstance("SHA256withECDSA", "SC")
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

    internal fun generatePublicKey(privateKeyArray: ByteArray, compressed: Boolean = false): ByteArray {
        return multiply(privateKeyArray).getEncoded(compressed)
    }

    internal fun loadPublicKey(publicKeyArray: ByteArray): PublicKey {
        val ecSpec = createECSpec()
        val p1 = ecSpec.curve.decodePoint(publicKeyArray)
        val publicKeySpec = ECPublicKeySpec(p1, ecSpec)

        return createKeyFactory().generatePublic(publicKeySpec)
    }

    @Suppress("MagicNumber")
    internal fun compressPublicKey(key: ByteArray): ByteArray = if (key.size == 65) {
        val publicKeyPoint = createECSpec().curve.decodePoint(key)
        publicKeyPoint.getEncoded(true)
    } else {
        key
    }

    @Suppress("MagicNumber")
    internal fun decompressPublicKey(key: ByteArray): ByteArray = if (key.size == 33) {
        val publicKeyPoint = createECSpec().curve.decodePoint(key)
        publicKeyPoint.getEncoded(false)
    } else {
        key
    }

    internal fun gMultiplyAndAddPoint(key: ByteArray, encodedPoint: ByteArray): ECPoint {
        val ecSpec = createECSpec()
        val multiplied = multiply(key, ecSpec)
        val decoded = ecSpec.curve.decodePoint(encodedPoint)

        return multiplied.add(decoded)
    }

    private fun multiply(privateKeyArray: ByteArray, ecSpec: ECParameterSpec? = null): ECPoint {
        val ecSpec = ecSpec ?: createECSpec()

        return ecSpec.g.multiply(BigInteger(1, privateKeyArray))
    }

    @Suppress("MagicNumber")
    internal fun normalize(signature: ByteArray): ByteArray {
        if (signature.size != 64) error("Invalid signature length")
        fun isCanonical(s: BigInteger, halfCurveOrder: BigInteger): Boolean = s <= halfCurveOrder

        val s = BigInteger(1, signature.copyOfRange(32, 64))
        val ecSpec = createECSpec()
        val halfCurveOrder: BigInteger = ecSpec.n shr 1

        if (!isCanonical(s, halfCurveOrder)) {
            val canonizedS = ecSpec.n.subtract(s)
            val baS = canonizedS.toByteArray()
            System.arraycopy(baS, 0, signature, 32, baS.size)
        }

        return signature
    }

    private fun createECSpec(): ECParameterSpec = ECNamedCurveTable.getParameterSpec("secp256k1")

    private fun createKeyFactory(): KeyFactory = KeyFactory.getInstance("EC", "SC")

    private fun calculateR(signature: ByteArray, size: Int): ASN1Integer =
        ASN1Integer(BigInteger(1, signature.copyOfRange(0, size)))

    private fun calculateS(signature: ByteArray, size: Int): ASN1Integer =
        ASN1Integer(BigInteger(1, signature.copyOfRange(size, size * 2)))

    @Suppress("MagicNumber")
    private fun checkSignatureForErrors(enc: ByteArray) {
        if (enc[0].toInt() != 0x30) error("bad encoding 1")
        if (enc[1].toInt() and 0x80 != 0) error("unsupported length encoding 1")
        if (enc[2].toInt() != 0x02) error("bad encoding 2")
        if (enc[3].toInt() and 0x80 != 0) error("unsupported length encoding 2")

        val rLength = enc[3].toInt()
        if (enc[4 + rLength].toInt() != 0x02) error("bad encoding 3")
        if (enc[5 + rLength].toInt() and 0x80 != 0) error("unsupported length encoding 3")
    }

    @Suppress("MagicNumber")
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
            error(
                "unsupported r-length - r-length:" + rLength.toString() + ",s-length:" + sLength.toString() +
                    ",enc:" + enc.toHexString()
            )
        }
        if (sLength <= 32) {
            System.arraycopy(enc, sPos, res, rLength + 32 - sLength, sLength)
            sLength = 32
        } else if (sLength == 33 && enc[sPos].toInt() == 0) {
            System.arraycopy(enc, sPos + 1, res, rLength, sLength - 1)
        } else {
            error(
                "unsupported s-length - r-length:" + rLength.toString() + ",s-length:" + sLength.toString() +
                    ",enc:" + enc.toHexString()
            )
        }

        return res
    }
}

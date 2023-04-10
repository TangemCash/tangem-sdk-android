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

/**
 * Created by Anton Zhilenkov on 09/04/2021.
 */
object Secp256r1 {

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

    private fun calculateR(signature: ByteArray, size: Int): ASN1Integer =
        ASN1Integer(BigInteger(1, signature.copyOfRange(0, size)))

    private fun calculateS(signature: ByteArray, size: Int): ASN1Integer =
        ASN1Integer(BigInteger(1, signature.copyOfRange(size, size * 2)))

    internal fun loadPublicKey(publicKeyArray: ByteArray): PublicKey {
        val spec = createECSpec()
        val factory = KeyFactory.getInstance("EC", "SC")

        val p1 = spec.curve.decodePoint(publicKeyArray)
        val keySpec = ECPublicKeySpec(p1, spec)

        return factory.generatePublic(keySpec)
    }

    internal fun generatePublicKey(privateKeyArray: ByteArray): ByteArray {
        return multiply(privateKeyArray).getEncoded(false)
    }

    private fun multiply(privateKeyArray: ByteArray, ecSpec: ECParameterSpec? = null): ECPoint {
        val ecSpec = ecSpec ?: createECSpec()
        return ecSpec.g.multiply(BigInteger(1, privateKeyArray))
    }

    internal fun sign(data: ByteArray, privateKeyArray: ByteArray): ByteArray {
        val spec = ECNamedCurveTable.getParameterSpec("secp256r1")
        val factory = KeyFactory.getInstance("EC", "SC")

        val keySpecP = ECPrivateKeySpec(BigInteger(1, privateKeyArray), spec)

        val signature = Signature.getInstance("SHA256withECDSA")

        val privateKey = factory.generatePrivate(keySpecP)
        signature.initSign(privateKey)
        signature.update(data)

        val enc = signature.sign()

        val res = toByte64(enc)

        if (!verify(generatePublicKey(privateKeyArray), data, res)) {
            error("Signature self verify failed - ,enc:" + enc.toHexString() + ",res:" + res.toHexString())
        }

        return res
    }

    @Suppress("MagicNumber")
    internal fun isPrivateKeyValid(privateKey: ByteArray): Boolean {
        val spec = createECSpec()
        val ecPoint = multiply(privateKey, spec)
        return !(ecPoint.isInfinity || BigInteger(privateKey.toHexString(), 16) >= spec.n)
    }

    private fun createECSpec(): ECParameterSpec = ECNamedCurveTable.getParameterSpec("secp256r1")

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
                    ",enc:" + enc.toHexString(),
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
                    ",enc:" + enc.toHexString(),
            )
        }

        return res
    }
}

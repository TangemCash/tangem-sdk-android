package com.tangem.operations.files

import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toByteArray
import com.tangem.crypto.sign

/**
 * Use this helper when creating signatures for files protected by Issuer's signature
 */
object FileHashHelper {
    /**
     * Creates hashes and signatures for [ByUser]
     *
     * @param cardId: CID, Unique Tangem card ID number.
     * @param fileData: File data that will be written on card
     * @param fileCounter: A counter that protects issuer data against replay attack.
     * @param privateKey: Optional private key that will be used for signing files hashes.
     * If it is provided, then [FileHashData] will contain signed file signatures.
     * @return [FileHashData] with hashes to sign and signatures if [privateKey] was provided.
     */
    fun prepareHashes(
        cardId: String,
        fileData: ByteArray,
        fileCounter: Int,
        fileName: String? = null,
        privateKey: ByteArray? = null,
    ): FileHashData {
        val fileData: ByteArray = fileName?.let { NamedFile(it, fileData).serialize() } ?: fileData

        val startingHash = cardId.hexToBytes() + fileCounter.toByteArray(size = 4) + fileData.size.toByteArray(size = 2)
        val finalizingHash = cardId.hexToBytes() + fileData + fileCounter.toByteArray(size = 4)

        val startingSignature = privateKey?.let { startingHash.sign(it) }
        val finalizingSignature = privateKey?.let { finalizingHash.sign(it) }

        return FileHashData(startingHash, finalizingHash, startingSignature, finalizingSignature)
    }
}

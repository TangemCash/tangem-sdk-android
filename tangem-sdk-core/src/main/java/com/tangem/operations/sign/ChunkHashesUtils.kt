package com.tangem.operations.sign

internal object ChunkHashesUtils {
    // The max answer is 1152 bytes (unencrypted) and 1120 (encrypted).
    // The worst case is 8 hashes * 64 bytes for ed + 512 bytes of signatures + cardId, SignedHashes + TLV + SW is ok.
    private const val PACKAGE_SIZE = 512
    // Card limitation
    private const val MAX_CHUNK_SIZE = 10

    fun chunkHashes(hashesRaw: Array<ByteArray>): List<Chunk> {
        val hashes = hashesRaw.mapIndexed { index, hash -> Hash(index = index, data = hash) }
        val hashesBySize = hashes.groupBy { it.data.size }

        return hashesBySize.flatMap { hashesGroup ->
            val hashSize = hashesGroup.key
            val chunkSize = getChunkSize(hashSize)

            hashesGroup.value
                .chunked(chunkSize)
                .map { Chunk(hashSize, it) }
        }
    }

    private fun getChunkSize(hashSize: Int) = (PACKAGE_SIZE / hashSize).coerceIn(1, MAX_CHUNK_SIZE)
}

package com.tangem.operations.sign

class ChunkedHashesContainer(
    hashes: Array<ByteArray>,
) {
    val isEmpty: Boolean = hashes.isEmpty()
    var currentChunkIndex: Int = 0
        private set

    private val chunks = ChunkHashesUtils.chunkHashes(hashes)
    val chunksCount = chunks.size

    private var signedChunks: MutableList<SignedChunk> = mutableListOf()

    fun getCurrentChunk(): Chunk {
        return chunks[currentChunkIndex]
    }

    fun addSignedChunk(signedChunk: SignedChunk) {
        signedChunks.add(signedChunk)
        currentChunkIndex++
    }

    fun getSignatures(): List<ByteArray> {
        return signedChunks
            .flatMap { it.signedHashes }
            .sortedBy { it.index }
            .map { it.signature }
    }
}

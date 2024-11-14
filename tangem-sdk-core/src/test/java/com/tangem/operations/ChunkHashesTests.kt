package com.tangem.operations

import com.tangem.common.extensions.hexToBytes
import com.tangem.operations.sign.Chunk
import com.tangem.operations.sign.ChunkHashesUtils
import com.tangem.operations.sign.ChunkedHashesContainer
import com.tangem.operations.sign.Hash
import com.tangem.operations.sign.SignedChunk
import com.tangem.operations.sign.SignedHash
import org.junit.Test
import kotlin.test.assertEquals

internal class ChunkHashesTests {

    @Test
    fun testSingleHashChunk() {
        val testData = listOf("f1642bb080e1f320924dde7238c1c5f8")
        val hashes = testData.map { it.hexToBytes() }.toTypedArray()

        val chunks = ChunkHashesUtils.chunkHashes(hashes)
        assertEquals(chunks.size, 1)

        val expectedChunk = Chunk(hashSize = 16, hashes = listOf(Hash(index = 0, data = hashes[0])))
        assertEquals(chunks, listOf(expectedChunk))
    }

    @Test
    fun testMultipleHashesChunk() {
        val testData = listOf(
            "f1642bb080e1f320924dde7238c1c5f8",
            "f1642bb080e1f320924dde7238c1c5f8f1642bb080e1f320924dde7238c1c5f8",
            "f1642bb080e1f320924dde7238c1c5f8f1642bb080e1f320924dde7238c1c5f8",
            "f1642bb080e1f320924dde7238c1c5f8f1642bb080e1f320924dde7238c1c5f0",
            "f1642bb080e1f320924dde7238c1c5f8f1642bb080e1f320924dde7238c1c5f1",
            "f1642bb080e1f320924dde7238c1c5f8f1642bb080e1f320924dde7238c1c5f2",
            "f1642bb080e1f320924dde7238c1c5f8f1642bb080e1f320924dde7238c1c5f3",
            "f1642bb080e1f320924dde7238c1c5f8f1642bb080e1f320924dde7238c1c5f4",
            "f1642bb080e1f320924dde7238c1c5f8f1642bb080e1f320924dde7238c1c5f5",
            "f1642bb080e1f320924dde7238c1c5f8f1642bb080e1f320924dde7238c1c5f6",
            "f1642bb080e1f320924dde7238c1c5f8f1642bb080e1f320924dde7238c1c5f7",
            "f1642bb080e1f320924dde7238c1c5f8f1642bb080e1f320924dde7238c1c5f9",
            "f1642bb080e1f320924dde7238c1c5f8",
            "f1642bb080e1f320924dde7238c1c5f8aa",
            "f1642bb080e1f320924dde7238c1c5f8ab",
        )

        val hashes = testData.map { it.hexToBytes() }.toTypedArray()

        val chunks = ChunkHashesUtils.chunkHashes(hashes)
        assertEquals(chunks.size, 4)

        val expectedChunks = listOf(
            Chunk(
                hashSize = 16,
                hashes = listOf(
                    Hash(index = 0, data = hashes[0]),
                    Hash(index = 12, data = hashes[12]),
                ),
            ),
            Chunk(
                hashSize = 17,
                hashes = listOf(
                    Hash(index = 13, data = hashes[13]),
                    Hash(index = 14, data = hashes[14]),
                ),
            ),
            Chunk(
                hashSize = 32,
                hashes = listOf(
                    Hash(index = 1, data = hashes[1]),
                    Hash(index = 2, data = hashes[2]),
                    Hash(index = 3, data = hashes[3]),
                    Hash(index = 4, data = hashes[4]),
                    Hash(index = 5, data = hashes[5]),
                    Hash(index = 6, data = hashes[6]),
                    Hash(index = 7, data = hashes[7]),
                    Hash(index = 8, data = hashes[8]),
                    Hash(index = 9, data = hashes[9]),
                    Hash(index = 10, data = hashes[10]),
                ),
            ),
            Chunk(
                hashSize = 32,
                hashes = listOf(
                    Hash(index = 11, data = hashes[11]),
                ),
            ),
        )

        assertEquals(chunks.sortedBy { it.hashSize }, expectedChunks.sortedBy { it.hashSize })
    }

    @Test
    fun testStrictSignaturesOrder() {
        val testHashesData = listOf(
            "f1642bb080e1f320924dde7238c1c5f8",
            "f1642bb080e1f320924dde7238c1c5f8f1642bb080e1f320924dde7238c1c5f8",
            "f1642bb080e1f320924dde7238c1c5f8f1642bb080e1f320924dde7238c1c5f8",
            "f1642bb080e1f320924dde7238c1c5f8f1642bb080e1f320924dde7238c1c5f0",
            "f1642bb080e1f320924dde7238c1c5f8f1642bb080e1f320924dde7238c1c5f1",
            "f1642bb080e1f320924dde7238c1c5f8f1642bb080e1f320924dde7238c1c5f2",
            "f1642bb080e1f320924dde7238c1c5f8f1642bb080e1f320924dde7238c1c5f3",
            "f1642bb080e1f320924dde7238c1c5f8f1642bb080e1f320924dde7238c1c5f4",
            "f1642bb080e1f320924dde7238c1c5f8f1642bb080e1f320924dde7238c1c5f5",
            "f1642bb080e1f320924dde7238c1c5f8f1642bb080e1f320924dde7238c1c5f6",
            "f1642bb080e1f320924dde7238c1c5f8f1642bb080e1f320924dde7238c1c5f7",
            "f1642bb080e1f320924dde7238c1c5f8f1642bb080e1f320924dde7238c1c5f9",
            "f1642bb080e1f320924dde7238c1c5f8",
            "f1642bb080e1f320924dde7238c1c5f8aa",
            "f1642bb080e1f320924dde7238c1c5f8ab",
        )

        val testSignaturesData = listOf(
            "0001",
            "0002",
            "0003",
            "0004",
            "0005",
            "0006",
            "0007",
            "0008",
            "0009",
            "0010",
            "0011",
            "0012",
            "0013",
            "0014",
            "0015",
        )

        val hashes = testHashesData.map { it.hexToBytes() }.toTypedArray()
        val expectedSignatures = testSignaturesData.map { it.hexToBytes() }

        val container = ChunkedHashesContainer(hashes = hashes)

        repeat(container.chunksCount) {
            val chunk = container.getCurrentChunk()

            val signedHashes = chunk.hashes.map {
                SignedHash(
                    index = it.index,
                    data = it.data,
                    signature = expectedSignatures[it.index],
                )
            }
            container.addSignedChunk(SignedChunk(signedHashes = signedHashes))
        }

        val signatures = container.getSignatures()
        assertEquals(signatures, expectedSignatures)
    }
}

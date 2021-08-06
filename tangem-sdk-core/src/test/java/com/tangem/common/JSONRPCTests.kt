package com.tangem.common

import com.tangem.Message
import com.tangem.common.card.Card
import com.tangem.common.card.CardWallet
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.json.*
import com.tangem.operations.CommandResponse
import com.tangem.operations.personalization.DepersonalizeResponse
import com.tangem.operations.sign.SignHashResponse
import com.tangem.operations.sign.SignHashesResponse
import com.tangem.operations.wallet.CreateWalletResponse
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

/**
 * Created by Anton Zhilenkov on 29/07/2021.
 */
class JSONRPCTests {
    private lateinit var workingDir: Path
    private lateinit var converter: MoshiJsonConverter
    private lateinit var jsonRpcConverter: JSONRPCConverter
    private lateinit var testCard: Card

    @Before
    fun init() {
        workingDir = Path.of("", "src/test/resources/jsonRpc")!!
        converter = MoshiJsonConverter.INSTANCE
        jsonRpcConverter = JSONRPCConverter.shared()
        initCard()
    }

    private fun initCard() {
        val json = readJson("Card")
        testCard = converter.fromJson(json)!!
    }

    @Test
    fun testCard() {
        val json = readJson("Card")

        val mapFromJson = converter.toMap(json)
        val mapFromObject = converter.toMap(converter.toJson(testCard))
        assert(jsonMapVerifier(mapFromJson, mapFromObject))
    }

    @Test
    fun testJsonRPCRequestParse() {
        val json = "{\"jsonrpc\": \"2.0\", \"method\": \"subtract\", \"params\": {\"subtrahend\": 23, \"minuend\": 42}, \"id\": 3}"

        val request = JSONRPCRequest(json)
        assert(request.method == "subtract")
        assert(request.params["subtrahend"] == 23.0)
    }

    @Test
    fun testInitialMessageInit() {
        val json = "{\"header\": \"Some header\", \"body\": \"Some body\"}"

        val message: Message = converter.fromJson(json)!!
        assert(message.header == "Some header")
        assert(message.body == "Some body")
    }

    @Test
    fun testJsonResponse() {
        val response = SuccessResponse("c000111122223333")
        val result: CompletionResult<SuccessResponse> = CompletionResult.Success(response)
        val testResponse = "{\n \"jsonrpc\" : \"2.0\",\n \"result\" : {\n \"cardId\" : \"c000111122223333\"\n},\n\"id\" : 1\n}"

        val mapJsonResponse = converter.toMap(result.toJSONRPCResponse(1).toJson())
        val mapTestJsonResponse = converter.toMap(testResponse)
        assert(jsonMapVerifier(mapJsonResponse, mapTestJsonResponse))
    }

    @Test
    fun testPersonalize_v3ADA() {
        testMethod("Personalize_v3ADA", testCard)
    }

    @Test
    fun testPersonalize_v4() {
        testMethod("Personalize_v4", testCard)
    }

    @Test
    fun testDepersonalize() {
        val response = DepersonalizeResponse(true)
        testMethod("Depersonalize", response)
    }

    @Test
    fun testPreflightRead() {
        testMethod("PreflightRead", testCard)
    }

    @Test
    fun testScan() {
        testMethod("Scan", testCard)
    }

    @Test
    fun testCreateWallet() {
        val wallet = CardWallet("5130869115a2ff91959774c99d4dc2873f0c41af3e0bb23d027ab16d39de1348".hexToBytes(),
                null,
                EllipticCurve.Secp256r1,
                CardWallet.Settings(true),
                10,
                100,
                1)
        val response = CreateWalletResponse("c000111122223333", wallet)
        testMethod("CreateWallet", response)
    }

    @Test
    fun testPurgeWallet() {
        val response = SuccessResponse("c000111122223333")
        testMethod("PurgeWallet", response)
    }

    @Test
    fun testSignHash() {
        val hash = "EB7411C2B7D871C06DAD51E58E44746583AD134F4E214E4899F2FC84802232A1".hexToBytes()
        val response = SignHashResponse("c000111122223333", hash, 1)
        testMethod("SignHash", response)
    }

    @Test
    fun testSignHashes() {
        val hashes = listOf(
                "EB7411C2B7D871C06DAD51E58E44746583AD134F4E214E4899F2FC84802232A1".hexToBytes(),
                "33443BD93F350B62A90A0C23D30C6D4E9BB164606E809CCACE60CF0E2591E58C".hexToBytes()
        )
        val response = SignHashesResponse("c000111122223333", hashes, 2)
        testMethod("SignHashes", response)
    }

    @Test
    fun testSetAccessCode() {
        testMethod("SetAccessCode", null)
    }

    @Test
    fun testSetPasscode() {
        testMethod("SetPasscode", null)
    }

    private fun testMethod(name: String, response: CommandResponse?) {
        val structure: HandlersStructure
        try {
            structure = converter.fromJson(readJson(name))!!
        } catch (ex: Exception) {
            throw Exception("Json conversion failed to structure for $name", ex)
        }

        structure.requests.forEachIndexed { index, request ->
            try {
                jsonRpcConverter.convert(request)
            } catch (ex: Exception) {
                throw Exception("Conversion to JSONRPC Failed. File: ${name}, index: $index", ex)
            }
        }

        val result: CompletionResult.Success<CommandResponse>? = response?.let { CompletionResult.Success(it) }
        if (structure.response != null && result != null) {
            val jsonResponseMap = converter.toMap(structure.response)
            val resultJsonRpcResponse = result.toJSONRPCResponse(structure.response.id)
            val testResponseMap = converter.toMap(resultJsonRpcResponse)
            assert(jsonMapVerifier(testResponseMap, jsonResponseMap))
        }
    }

    private fun readJson(fileName: String): String {
        val file: Path = workingDir.resolve("$fileName.json")
        return Files.readString(file)
    }

    private fun jsonMapVerifier(lMap: Map<String, *>, rMap: Map<String, *>): Boolean {
        val mapKeys = lMap.mapNotNull { if (rMap.containsKey(it.key)) it.key else null }
        if (lMap.size != mapKeys.size) throw Exception("Property counts not equals (${lMap.size} & ${mapKeys.size})")

        for (key in mapKeys) {
            val lValue = lMap[key]
            val rValue = rMap[key]

            if (lValue is Map<*, *> && rValue is Map<*, *>) {
                jsonMapVerifier(lValue as Map<String, *>, rValue as Map<String, *>)
                continue
            }

            if (lValue is List<*> && rValue is List<*>) {
                if (lValue.size != rValue.size) throw Exception("Arrays [$key] size not equals")

                lValue.forEachIndexed { index, lValueByIndex ->
                    val rValueByIndex = rValue[index]
                    if (lValueByIndex != rValueByIndex) {
                        throw Exception("Value in arrays [$key] not equals ($lValueByIndex & $rValueByIndex)")
                    }
                }
                continue
            }

            if (lValue is ByteArray && rValue is ByteArray) {
                if (lValue.contentEquals(rValue)) {
                    continue
                } else {
                    throw Exception("Values for [$key] not equals ($lValue & $rValue)")
                }
            }
            if (lValue == rValue) continue else throw Exception("Values for [$key] not equals ($lValue & $rValue)")
        }
        return true
    }

    private data class HandlersStructure(
        val requests: List<JSONRPCRequest>,
        val response: JSONRPCResponse? = null
    )
}
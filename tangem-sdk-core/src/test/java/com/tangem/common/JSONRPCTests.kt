package com.tangem.common

import com.tangem.Message
import com.tangem.common.card.Card
import com.tangem.common.card.CardWallet
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.files.File
import com.tangem.common.files.FileSettings
import com.tangem.common.json.*
import com.tangem.operations.CommandResponse
import com.tangem.operations.files.ReadFilesResponse
import com.tangem.operations.files.WriteFilesResponse
import com.tangem.operations.personalization.DepersonalizeResponse
import com.tangem.operations.sign.SignHashResponse
import com.tangem.operations.sign.SignHashesResponse
import com.tangem.operations.wallet.CreateWalletResponse
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Created by Anton Zhilenkov on 29/07/2021.
 */
class JSONRPCTests {
    private lateinit var converter: MoshiJsonConverter
    private lateinit var jsonRpcConverter: JSONRPCConverter
    private lateinit var testCard: Card

    @BeforeEach
    fun init() {
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
        assertTrue(jsonMapVerifier(mapFromJson, mapFromObject))
    }

    @Test
    fun testJsonRPCRequestParse() {
        JSONRPCRequest("{\"jsonrpc\": \"2.0\", \"method\": \"any\", \"params\": {}}")

        val json = "{\"jsonrpc\": \"2.0\", \"method\": \"subtract\", \"params\": {\"subtrahend\": 23, \"minuend\": 42}, \"id\": 3}"
        val request = JSONRPCRequest(json)
        assertEquals(request.method, "subtract")
        assertEquals(request.params["subtrahend"], 23.0)
    }

    @Test
    fun testInitialMessageInit() {
        val json = "{\"header\": \"Some header\", \"body\": \"Some body\"}"

        val message: Message = converter.fromJson(json)!!
        assertEquals(message.header, "Some header")
        assertEquals(message.body, "Some body")
    }

    @Test
    fun testJsonResponse() {
        val response = SuccessResponse("c000111122223333")
        val result: CompletionResult<SuccessResponse> = CompletionResult.Success(response)
        val testResponse = "{\n \"jsonrpc\" : \"2.0\",\n \"result\" : {\n \"cardId\" : \"c000111122223333\"\n},\n\"id\" : 1\n}"

        val jsonRpcResponse = when (result) {
            is CompletionResult.Success -> JSONRPCResponse(result.data, null, 1)
            is CompletionResult.Failure -> JSONRPCResponse(null, result.error.toJSONRPCError(), 1)
        }
        val mapJsonResponse = converter.toMap(jsonRpcResponse.toJson())
        val mapTestJsonResponse = converter.toMap(testResponse)
        assertTrue(jsonMapVerifier(mapJsonResponse, mapTestJsonResponse))
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
                1,
                false
        )
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

    @Test
    fun testFiles() {
        testMethod("files/ReadFiles", ReadFilesResponse(listOf(
                File(0, FileSettings.Public, "00AABBCCDD".hexToBytes())))
        )
        testMethod("files/ReadFilesByIndex", ReadFilesResponse(listOf(
                File(1, FileSettings.Private, "00AABBCCDD".hexToBytes()),
                File(2, FileSettings.Public, "00AABBCCDD".hexToBytes())))
        )
        testMethod("files/DeleteFiles", SuccessResponse("c000111122223333"))
        testMethod("files/WriteFiles", WriteFilesResponse("c000111122223333", listOf(0, 1)))
        testMethod("files/ChangeFileSettings", SuccessResponse("c000111122223333"))
    }

    @Test
    fun testParseArrayOfRequests() {
        val fileText = readJson("TestArray")
        val fileMap = converter.toMap(fileText)
        val requestsJson = converter.toJson(fileMap["requests"])
        val linkersList = JSONRPCLinker.parse(requestsJson, converter)
        linkersList.forEach { it.initRunnable(jsonRpcConverter) }

        assertFalse(linkersList.any { it.hasError() })
    }

    private fun testMethod(name: String, response: CommandResponse?) {
        val jsonMap = converter.toMap(readJson(name))
        val jsonRequest = converter.toJson(jsonMap["request"])
        val request: JSONRPCRequest = assertDoesNotThrow("Json conversion failed to structure for $name") {
            JSONRPCRequest(jsonRequest)
        }

        assertDoesNotThrow("Conversion to JSONRPC Failed. File: ${name}") {
            jsonRpcConverter.convert(request)
        }

        val jsonResponse: JSONRPCResponse? = jsonMap["response"]?.let { converter.toJson(it)?.let { converter.fromJson(it) } }
        val result: CompletionResult<CommandResponse>? = response?.let { CompletionResult.Success(it) }
        if (jsonResponse != null && result != null) {
            val jsonResponseMap = converter.toMap(jsonResponse)
            val resultJsonRpcResponse = when (result) {
                is CompletionResult.Success -> JSONRPCResponse(result.data, null, jsonResponse.id)
                is CompletionResult.Failure -> JSONRPCResponse(null, result.error.toJSONRPCError(), jsonResponse.id)
            }
            val testResponseMap = converter.toMap(resultJsonRpcResponse)
            assertTrue(jsonMapVerifier(testResponseMap, jsonResponseMap))
        }
    }

    private fun readJson(fileName: String): String {
        val workingDir: Path = Paths.get("src/test/resources/jsonRpc", "$fileName.json")!!
        return String(Files.readAllBytes(workingDir))
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
}
package com.tangem.operations.attestation

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.Log
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.toHexString
import com.tangem.common.services.Result
import com.tangem.common.services.performRequest
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.plus
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.io.PrintWriter
import java.io.StringWriter

class OnlineCardVerifier {

    val scope = CoroutineScope(Dispatchers.IO) + CoroutineExceptionHandler { _, ex ->
        val sw = StringWriter()
        ex.printStackTrace(PrintWriter(sw))
        Log.error { sw.toString() }
    }

    private val tangemVerifyApi: TangemApi by lazy {
        val builder = Retrofit.Builder()
            .baseUrl(TangemApi.Companion.BaseUrl.VERIFY.url)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        return@lazy builder.create(TangemApi::class.java)
    }

    private val tangemCardDataApi: TangemApi by lazy {
        val builder = Retrofit.Builder()
            .baseUrl(TangemApi.Companion.BaseUrl.CARD_DATA.url)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        return@lazy builder.create(TangemApi::class.java)
    }

    suspend fun getCardInfo(cardId: String, cardPublicKey: ByteArray): Result<CardVerifyAndGetInfo.Response.Item> {
        val requestsBody = CardVerifyAndGetInfo.Request()
        requestsBody.requests =
            listOf(CardVerifyAndGetInfo.Request.Item(cardId, cardPublicKey.toHexString()))

        return when (
            val result =
                performRequest { tangemVerifyApi.getCardVerifyAndGetInfo(requestsBody) }
        ) {
            is Result.Success -> {
                val firstResult = result.data.results?.firstOrNull()
                when {
                    firstResult == null -> Result.Failure(TangemSdkError.NetworkError("Empty response"))
                    !firstResult.passed -> Result.Failure(TangemSdkError.CardVerificationFailed())
                    else -> Result.Success(firstResult)
                }
            }
            is Result.Failure -> result
        }
    }

    suspend fun getArtwork(cardId: String, cardPublicKey: String, artworkId: String): Result<ResponseBody> {
        return performRequest { tangemVerifyApi.getArtwork(artworkId, cardId, cardPublicKey) }
    }

    suspend fun getCardData(cardId: String, cardPublicKey: ByteArray): Result<CardDataResponse> {
        return try {
            performRequest {
                tangemCardDataApi.getCardData(
//                    cardId, cardPublicKey.toHexString()
                    TangemApi.getCardDataHeaders(cardId, cardPublicKey.toHexString()),
                )
            }
        } catch (exception: Exception) {
            Result.Failure(TangemSdkError.NetworkError(exception.localizedMessage))
        }
    }

    companion object {
        private val moshi: Moshi by lazy {
            Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
        }

        fun getUrlForArtwork(cardId: String, cardPublicKey: String, artworkId: String): String {
            return TangemApi.Companion.BaseUrl.VERIFY.url + TangemApi.ARTWORK +
                "?artworkId=$artworkId&CID=$cardId&publicKey=$cardPublicKey"
        }
    }
}

interface TangemApi {

    @Headers("Content-Type: application/json")
    @POST(VERIFY_AND_GET_INFO)
    suspend fun getCardVerifyAndGetInfo(@Body requestBody: CardVerifyAndGetInfo.Request): CardVerifyAndGetInfo.Response

    @Headers("Content-Type: application/json")
    @GET(ARTWORK)
    suspend fun getArtwork(
        @Query("artworkId") artworkId: String,
        @Query("CID") cid: String,
        @Query("publicKey") publicKey: String,
    ): ResponseBody

    @Headers("Content-Type: application/json")
    @GET(CARD_DATA)
    suspend fun getCardData(@HeaderMap headers: Map<String, String>): CardDataResponse

    companion object {
        enum class BaseUrl(val url: String) {
            CARD_DATA("https://api.tangem-tech.com/"),
            VERIFY("https://verify.tangem.com/"),
        }

        const val VERIFY_AND_GET_INFO = "card/verify-and-get-info"
        const val ARTWORK = "card/artwork"
        const val CARD_DATA = "card"

        fun getCardDataHeaders(cardId: String, publicKey: String): Map<String, String> {
            return mapOf("card_id" to cardId, "card_public_key" to publicKey)
        }
    }
}

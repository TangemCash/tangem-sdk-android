package com.tangem.operations.attestation

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.common.services.Result
import com.tangem.common.services.performRequest
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*

class OnlineCardVerifier {

    private val tangemApi: TangemApi by lazy {
        val builder = Retrofit.Builder()
                .baseUrl(TangemApi.ENDPOINT)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
        return@lazy builder.create(TangemApi::class.java)
    }

    suspend fun getCardInfo(cardId: String, cardPublicKey: String): Result<CardVerifyAndGetInfo.Response> {
        val requestsBody = CardVerifyAndGetInfo.Request()
        requestsBody.requests = listOf(CardVerifyAndGetInfo.Request.Item(cardId, cardPublicKey))
        return performRequest { tangemApi.getCardVerifyAndGetInfo(requestsBody) }
    }

    suspend fun getArtwork(
        cardId: String,
        cardPublicKey: String,
        artworkId: String
    ): Result<ResponseBody> {
        return performRequest { tangemApi.getArtwork(artworkId, cardId, cardPublicKey) }
    }

    companion object {
        fun getUrlForArtwork(cardId: String, cardPublicKey: String, artworkId: String): String {
            return TangemApi.ENDPOINT + TangemApi.ARTWORK +
                    "?artworkId=${artworkId}&CID=${cardId}&publicKey=$cardPublicKey"
        }

        private val moshi: Moshi by lazy {
            Moshi.Builder()
                    .add(KotlinJsonAdapterFactory())
                    .build()
        }
    }
}

interface TangemApi {

    @Headers("Content-Type: application/json")
    @POST(VERIFY_AND_GET_INFO)
    suspend fun getCardVerifyAndGetInfo(
        @Body requestBody: CardVerifyAndGetInfo.Request
    ): CardVerifyAndGetInfo.Response

    @Headers("Content-Type: application/json")
    @GET(ARTWORK)
    suspend fun getArtwork(
        @Query("artworkId") artworkId: String,
        @Query("CID") CID: String,
        @Query("publicKey") publicKey: String
    ): ResponseBody

    companion object {
        const val ENDPOINT: String = "https://verify.tangem.com/"

        const val VERIFY = "verify"
        const val VERIFY_AND_GET_INFO = "card/verify-and-get-info"
        const val ARTWORK = "card/artwork"
    }
}
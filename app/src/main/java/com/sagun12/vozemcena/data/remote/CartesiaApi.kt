package com.sagun12.vozemcena.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Streaming

interface CartesiaApi {

    @Streaming
    @POST("tts/bytes")
    suspend fun synthesizeBytes(
        @Body request: CartesiaTtsRequest
    ): Response<ResponseBody>

    @GET("voices")
    suspend fun listVoices(): Response<CartesiaVoiceListResponse>

    @Multipart
    @POST("voices/clone/clip")
    suspend fun cloneVoiceFromClip(
        @Part clip: MultipartBody.Part,
        @Part("name") name: RequestBody,
        @Part("description") description: RequestBody?,
        @Part("language") language: RequestBody?
    ): Response<CartesiaCloneResponse>
}

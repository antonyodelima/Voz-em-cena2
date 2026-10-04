package com.sagun12.vozemcena.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Endpoint REST da API Google AI (Gemini).
 *
 * A chave de API é enviada via query param `key`, conforme documentado em
 * https://ai.google.dev/api/generate-content
 */
interface GoogleAiApi {

    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): Response<GeminiResponse>
}

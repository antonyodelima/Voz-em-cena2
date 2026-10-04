package com.sagun12.vozemcena.data.remote

import com.sagun12.vozemcena.data.settings.CartesiaSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Cliente de alto nível da API Google AI (Gemini).
 *
 * Lê a chave via [CartesiaSettingsStore.getEffectiveGoogleAiApiKey] (secrets do
 * build ou valor salvo pelo usuário) e chama `generateContent` no endpoint
 * `https://generativelanguage.googleapis.com`.
 */
class GoogleAiClient(
    private val api: GoogleAiApi,
    private val settingsStore: CartesiaSettingsStore
) {

    val isConfigured: Boolean
        get() = settingsStore.hasValidGoogleAiApiKey()

    suspend fun generateText(
        prompt: String,
        model: String = DEFAULT_MODEL,
        systemInstruction: String? = DEFAULT_SYSTEM_INSTRUCTION,
        temperature: Float? = 0.7f,
        maxOutputTokens: Int? = 1024
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = settingsStore.getEffectiveGoogleAiApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException(
                    "Chave da API Google AI não configurada. Adicione GOOGLE_API_KEY no painel de Secrets/Keys."
                )
            )
        }

        runCatching {
            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(parts = listOf(GeminiPart(text = prompt)))
                ),
                systemInstruction = systemInstruction?.let { instruction ->
                    GeminiContent(parts = listOf(GeminiPart(text = instruction)))
                },
                generationConfig = GeminiGenerationConfig(
                    temperature = temperature,
                    maxOutputTokens = maxOutputTokens
                )
            )

            val response = api.generateContent(model = model, apiKey = apiKey, request = request)
            val body = response.body()

            if (!response.isSuccessful || body == null) {
                val errorBody = response.errorBody()?.string() ?: response.message()
                throw IllegalStateException("Erro Google AI (${response.code()}): $errorBody")
            }

            body.error?.message?.let { message ->
                throw IllegalStateException("Erro Google AI: $message")
            }

            val text = body.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.joinToString(separator = "") { it.text }
                ?.trim()

            if (text.isNullOrBlank()) {
                throw IllegalStateException("O Google AI não retornou texto para o prompt informado.")
            }

            text
        }
    }

    companion object {
        const val DEFAULT_MODEL = "gemini-2.0-flash"

        const val DEFAULT_SYSTEM_INSTRUCTION =
            "Você é um assistente de criação de roteiros e áudio do aplicativo Voz em Cena. " +
                "Responda sempre em português do Brasil, de forma concisa e pronta para dublagem."
    }
}

package com.sagun12.vozemcena.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleAiDtosTest {

    @Test
    fun generateContentRequest_serializationStructure() {
        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = "Crie um roteiro de teste")))
            ),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = "Você é um assistente"))),
            generationConfig = GeminiGenerationConfig(temperature = 0.7f, maxOutputTokens = 512)
        )

        assertEquals(1, request.contents.size)
        assertEquals("Crie um roteiro de teste", request.contents.first().parts.first().text)
        assertEquals("Você é um assistente", request.systemInstruction?.parts?.first()?.text)
        assertEquals(0.7f, request.generationConfig?.temperature)
        assertEquals(512, request.generationConfig?.maxOutputTokens)
    }

    @Test
    fun response_candidateTextExtraction() {
        val response = GeminiResponse(
            candidates = listOf(
                GeminiCandidate(
                    content = GeminiContent(parts = listOf(GeminiPart(text = "Roteiro gerado."))),
                    finishReason = "STOP"
                )
            ),
            error = null
        )

        val text = response.candidates
            ?.firstOrNull()
            ?.content
            ?.parts
            ?.joinToString(separator = "") { it.text }
            ?.trim()

        assertEquals("Roteiro gerado.", text)
        assertNull(response.error)
        assertTrue(response.candidates!!.isNotEmpty())
    }
}

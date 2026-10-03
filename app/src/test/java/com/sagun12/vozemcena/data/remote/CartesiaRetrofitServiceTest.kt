package com.sagun12.vozemcena.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CartesiaRetrofitServiceTest {

    @Test
    fun service_instantiationWithApiKey() {
        val service = CartesiaRetrofitService(apiKey = "test_cartesia_key_123")
        assertNotNull(service.api)
    }

    @Test
    fun ttsRequest_serializationStructure() {
        val request = CartesiaTtsRequest(
            modelId = "sonic-multilingual",
            transcript = "Teste de fala",
            voice = CartesiaVoiceSpec(mode = "id", id = "test-voice-id"),
            outputFormat = CartesiaOutputFormat(
                container = "wav",
                encoding = "pcm_s16le",
                sampleRate = 44100
            ),
            language = "pt"
        )

        assertEquals("sonic-multilingual", request.modelId)
        assertEquals("Teste de fala", request.transcript)
        assertEquals("test-voice-id", request.voice.id)
        assertEquals("wav", request.outputFormat.container)
        assertEquals(44100, request.outputFormat.sampleRate)
        assertEquals("pt", request.language)
    }
}

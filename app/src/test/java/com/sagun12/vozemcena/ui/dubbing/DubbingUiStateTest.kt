package com.sagun12.vozemcena.ui.dubbing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class DubbingUiStateTest {

    @Test
    fun defaultState_isCorrect() {
        val state = DubbingUiState()
        assertFalse(state.isGenerating)
        assertNull(state.generatedAudioFile)
        assertEquals("sonic-multilingual", state.selectedModel)
        assertEquals("pt", state.selectedLanguage)
        assertEquals("cartesia-pt-helena", state.selectedVoiceId)
    }

    @Test
    fun stateUpdate_preservesFields() {
        val state = DubbingUiState(
            scriptText = "Texto de teste",
            selectedVoiceId = "custom-id",
            selectedVoiceName = "Voz Personalizada"
        )
        assertEquals("Texto de teste", state.scriptText)
        assertEquals("custom-id", state.selectedVoiceId)
        assertEquals("Voz Personalizada", state.selectedVoiceName)
    }
}

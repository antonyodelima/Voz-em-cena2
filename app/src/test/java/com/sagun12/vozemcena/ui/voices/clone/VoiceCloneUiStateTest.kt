package com.sagun12.vozemcena.ui.voices.clone

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceCloneUiStateTest {

    @Test
    fun defaultState_isCorrect() {
        val state = VoiceCloneUiState()
        assertFalse(state.isRecording)
        assertFalse(state.isUploading)
        assertNull(state.recordedAudioFile)
        assertNull(state.createdVoice)
        assertEquals("pt", state.language)
        assertEquals(0f, state.recordingDurationSec)
    }

    @Test
    fun stateUpdate_preservesFields() {
        val state = VoiceCloneUiState(
            voiceName = "Minha Voz",
            voiceDescription = "Tom natural",
            language = "en"
        )
        assertEquals("Minha Voz", state.voiceName)
        assertEquals("Tom natural", state.voiceDescription)
        assertEquals("en", state.language)
    }
}

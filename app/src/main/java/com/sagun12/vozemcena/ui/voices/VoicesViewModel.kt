package com.sagun12.vozemcena.ui.voices

import android.content.Context
import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sagun12.vozemcena.data.repository.VoiceRepository
import com.sagun12.vozemcena.domain.model.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class VoicesUiState(
    val searchQuery: String = "",
    val selectedLanguageFilter: String = "ALL", // ALL, pt-BR, en-US
    val playingVoiceId: String? = null,
    val isCloning: Boolean = false,
    val cloneSuccessMessage: String? = null,
    val errorMessage: String? = null
)

class VoicesViewModel(
    private val context: Context,
    private val voiceRepository: VoiceRepository
) : ViewModel() {

    val audioRecorder = AudioRecorder(context)
    private var mediaPlayer: MediaPlayer? = null

    private val _uiState = MutableStateFlow(VoicesUiState())
    val uiState: StateFlow<VoicesUiState> = _uiState.asStateFlow()

    private val rawVoices = voiceRepository.getAllVoices()

    val filteredVoices: StateFlow<List<Voice>> = combine(rawVoices, _uiState) { voices, state ->
        voices.filter { voice ->
            val matchesSearch = state.searchQuery.isBlank() ||
                    voice.name.contains(state.searchQuery, ignoreCase = true) ||
                    voice.description.contains(state.searchQuery, ignoreCase = true) ||
                    voice.tags.any { it.contains(state.searchQuery, ignoreCase = true) }

            val matchesLang = when (state.selectedLanguageFilter) {
                "ALL" -> true
                "pt-BR" -> voice.language.startsWith("pt", ignoreCase = true)
                "en-US" -> voice.language.startsWith("en", ignoreCase = true)
                else -> true
            }

            matchesSearch && matchesLang
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun onLanguageFilterChange(filter: String) {
        _uiState.value = _uiState.value.copy(selectedLanguageFilter = filter)
    }

    fun playPreview(voice: Voice, testText: String = "Olá, esta é uma demonstração da minha voz no Voz em Cena!") {
        if (_uiState.value.playingVoiceId == voice.id) {
            stopAudioPreview()
            return
        }

        stopAudioPreview()
        _uiState.value = _uiState.value.copy(playingVoiceId = voice.id)

        viewModelScope.launch {
            val previewFile = File(context.cacheDir, "preview_${voice.id}.wav")
            val result = voiceRepository.synthesizeSpeechToFile(testText, voice.id, previewFile)
            if (result.isSuccess) {
                try {
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(previewFile.absolutePath)
                        prepare()
                        setOnCompletionListener {
                            stopAudioPreview()
                        }
                        start()
                    }
                } catch (e: Exception) {
                    stopAudioPreview()
                }
            } else {
                stopAudioPreview()
            }
        }
    }

    fun stopAudioPreview() {
        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }
        mediaPlayer = null
        _uiState.value = _uiState.value.copy(playingVoiceId = null)
    }

    fun startRecordingVoice() {
        val tempFile = File(context.cacheDir, "recording_${UUID.randomUUID()}.m4a")
        audioRecorder.startRecording(tempFile, viewModelScope)
    }

    fun stopAndSaveRecordedVoice(name: String, description: String) {
        val recordedFile = audioRecorder.stopRecording() ?: return
        _uiState.value = _uiState.value.copy(isCloning = true, errorMessage = null)

        viewModelScope.launch {
            val result = voiceRepository.cloneVoice(name, description, recordedFile, "pt")
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isCloning = false,
                    cloneSuccessMessage = "Voz '${name}' adicionada com sucesso!"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isCloning = false,
                    errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Erro ao salvar voz."
                )
            }
        }
    }

    fun deleteCustomVoice(voiceId: String) {
        viewModelScope.launch {
            voiceRepository.deleteVoice(voiceId)
        }
    }

    fun clearFeedbackMessages() {
        _uiState.value = _uiState.value.copy(cloneSuccessMessage = null, errorMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        stopAudioPreview()
        audioRecorder.stopRecording()
    }
}

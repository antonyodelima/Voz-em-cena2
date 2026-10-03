package com.sagun12.vozemcena.ui.home

import android.content.Context
import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sagun12.vozemcena.data.repository.ProjectRepository
import com.sagun12.vozemcena.data.repository.VoiceRepository
import com.sagun12.vozemcena.domain.model.AspectRatio
import com.sagun12.vozemcena.domain.model.Project
import com.sagun12.vozemcena.domain.model.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class HomeUiState(
    val isCreatingProject: Boolean = false,
    val playingVoiceId: String? = null,
    val quickTestText: String = "Bem-vindo ao Voz em Cena! Escolha sua voz e crie vídeos incríveis.",
    val isGeneratingQuickAudio: Boolean = false,
    val createdProjectId: String? = null
)

class HomeViewModel(
    private val context: Context,
    private val projectRepository: ProjectRepository,
    private val voiceRepository: VoiceRepository
) : ViewModel() {

    private var mediaPlayer: MediaPlayer? = null

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val recentProjects: StateFlow<List<Project>> = projectRepository.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val featuredVoices: StateFlow<List<Voice>> = voiceRepository.getAllVoices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateQuickTestText(text: String) {
        _uiState.value = _uiState.value.copy(quickTestText = text)
    }

    fun playVoiceQuickTest(voice: Voice) {
        if (_uiState.value.playingVoiceId == voice.id) {
            stopAudio()
            return
        }

        stopAudio()
        _uiState.value = _uiState.value.copy(playingVoiceId = voice.id, isGeneratingQuickAudio = true)

        viewModelScope.launch {
            val previewFile = File(context.cacheDir, "quick_test_${voice.id}.wav")
            val text = _uiState.value.quickTestText.ifBlank { "Olá! Teste de síntese de voz." }
            val result = voiceRepository.synthesizeSpeechToFile(text, voice.id, previewFile)

            _uiState.value = _uiState.value.copy(isGeneratingQuickAudio = false)
            if (result.isSuccess) {
                try {
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(previewFile.absolutePath)
                        prepare()
                        setOnCompletionListener {
                            stopAudio()
                        }
                        start()
                    }
                } catch (e: Exception) {
                    stopAudio()
                }
            } else {
                stopAudio()
            }
        }
    }

    fun stopAudio() {
        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }
        mediaPlayer = null
        _uiState.value = _uiState.value.copy(playingVoiceId = null, isGeneratingQuickAudio = false)
    }

    fun createNewProject(aspectRatio: AspectRatio, onProjectCreated: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreatingProject = true)
            val newProject = projectRepository.createNewProject(
                title = "Meu Projeto ${System.currentTimeMillis() % 1000}",
                aspectRatio = aspectRatio
            )
            _uiState.value = _uiState.value.copy(isCreatingProject = false, createdProjectId = newProject.id)
            onProjectCreated(newProject.id)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAudio()
    }
}

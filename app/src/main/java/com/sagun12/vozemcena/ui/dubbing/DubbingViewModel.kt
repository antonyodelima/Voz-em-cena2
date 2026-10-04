package com.sagun12.vozemcena.ui.dubbing

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sagun12.vozemcena.data.remote.CartesiaRetrofitService
import com.sagun12.vozemcena.data.repository.GoogleAiRepository
import com.sagun12.vozemcena.data.repository.VoiceRepository
import com.sagun12.vozemcena.data.settings.CartesiaSettingsStore
import com.sagun12.vozemcena.domain.model.Voice
import com.sagun12.vozemcena.domain.model.VoiceProvider
import com.sagun12.vozemcena.video.WavAudio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class DubbingUiState(
    val scriptText: String = "No silêncio do cinema, cada palavra ganha vida. Bem-vindo à experiência definitiva de dublagem neural com Voz em Cena.",
    val selectedVoiceId: String = "cartesia-pt-helena",
    val selectedVoiceName: String = "Helena (Narradora)",
    val selectedLanguage: String = "pt",
    val selectedModel: String = "sonic-multilingual",
    val isGenerating: Boolean = false,
    val generatedAudioFile: File? = null,
    val audioDurationSeconds: Float = 0f,
    val generationLatencyMs: Long = 0L,
    val isSuggestingScript: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class DubbingViewModel(
    private val context: Context,
    private val voiceRepository: VoiceRepository,
    private val cartesiaRetrofitService: CartesiaRetrofitService,
    private val settingsStore: CartesiaSettingsStore,
    private val googleAiRepository: GoogleAiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DubbingUiState())
    val uiState: StateFlow<DubbingUiState> = _uiState.asStateFlow()

    val availableVoices: StateFlow<List<Voice>> = voiceRepository.getAllVoices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        val model = settingsStore.getSelectedModel()
        _uiState.value = _uiState.value.copy(selectedModel = model)
    }

    fun onScriptChange(newScript: String) {
        _uiState.value = _uiState.value.copy(scriptText = newScript)
    }

    fun selectVoice(voice: Voice) {
        _uiState.value = _uiState.value.copy(
            selectedVoiceId = voice.id,
            selectedVoiceName = voice.name,
            selectedLanguage = if (voice.language.startsWith("en", ignoreCase = true)) "en" else "pt"
        )
    }

    fun selectModel(model: String) {
        _uiState.value = _uiState.value.copy(selectedModel = model)
    }

    fun selectLanguage(lang: String) {
        _uiState.value = _uiState.value.copy(selectedLanguage = lang)
    }

    fun applyPresetScript(presetText: String) {
        _uiState.value = _uiState.value.copy(scriptText = presetText)
    }

    fun generateDubbing() {
        val text = _uiState.value.scriptText.trim()
        if (text.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Por favor, digite um texto para gerar a dublagem.")
            return
        }

        _uiState.value = _uiState.value.copy(
            isGenerating = true,
            errorMessage = null,
            successMessage = null
        )

        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val dubbingDir = File(context.cacheDir, "dubbing").apply { mkdirs() }
            val outputFile = File(dubbingDir, "dubbing_${UUID.randomUUID()}.wav")

            val effectiveVoiceId = when (_uiState.value.selectedVoiceId) {
                "cartesia-pt-helena" -> "794f9389-aac1-45b6-b726-9d9369183238"
                "cartesia-pt-thiago" -> "a0e99841-438c-4a64-b679-ae501e7d6091"
                "cartesia-pt-sofia" -> "b7d50908-b47c-442d-ad6f-1a7ecab0b519"
                "cartesia-pt-gabriel" -> "638efaaa-4d0c-442e-b701-3fae16aa63c0"
                "cartesia-pt-beatriz" -> "ee7ea9f8-c0c1-498c-9f7d-4e929d2de252"
                else -> _uiState.value.selectedVoiceId
            }

            val hasApiKey = settingsStore.hasValidApiKey()

            val result = if (hasApiKey) {
                // Call Cartesia Retrofit Service
                cartesiaRetrofitService.synthesizeSpeechToFile(
                    text = text,
                    voiceId = effectiveVoiceId,
                    outputFile = outputFile,
                    modelId = _uiState.value.selectedModel,
                    language = _uiState.value.selectedLanguage,
                    sampleRate = 44100
                )
            } else {
                // Fallback to voiceRepository synthesis
                voiceRepository.synthesizeSpeechToFile(text, _uiState.value.selectedVoiceId, outputFile)
            }

            val elapsedMs = System.currentTimeMillis() - startTime

            if (result.isSuccess) {
                val duration = WavAudio.getWavDurationSeconds(outputFile)
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    generatedAudioFile = outputFile,
                    audioDurationSeconds = duration,
                    generationLatencyMs = elapsedMs,
                    successMessage = "Áudio sintetizado com sucesso em ${elapsedMs}ms!"
                )
            } else {
                val error = result.exceptionOrNull()?.localizedMessage ?: "Erro desconhecido ao sintetizar voz."
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    errorMessage = error
                )
            }
        }
    }

    /**
     * Consulta o Google AI (Gemini) para sugerir um roteiro de dublagem a partir do
     * texto atual (ou de um tema padrão) e o injeta no campo de roteiro.
     */
    fun suggestScriptWithAi() {
        if (_uiState.value.isSuggestingScript) return

        val current = _uiState.value.scriptText.trim()
        val topic = current.ifBlank { "uma narração curta e impactante para um vídeo" }

        _uiState.value = _uiState.value.copy(
            isSuggestingScript = true,
            errorMessage = null,
            successMessage = null
        )

        viewModelScope.launch {
            val result = googleAiRepository.suggestDubbingScript(
                topic = topic,
                language = _uiState.value.selectedLanguage
            )

            _uiState.value = result.fold(
                onSuccess = { script ->
                    _uiState.value.copy(
                        isSuggestingScript = false,
                        scriptText = script,
                        successMessage = "Roteiro sugerido pelo Google AI."
                    )
                },
                onFailure = { error ->
                    _uiState.value.copy(
                        isSuggestingScript = false,
                        errorMessage = error.localizedMessage
                            ?: "Não foi possível consultar o Google AI."
                    )
                }
            )
        }
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}

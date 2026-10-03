package com.sagun12.vozemcena.ui.voices.clone

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sagun12.vozemcena.data.remote.CartesiaRetrofitService
import com.sagun12.vozemcena.data.repository.VoiceRepository
import com.sagun12.vozemcena.data.settings.CartesiaSettingsStore
import com.sagun12.vozemcena.domain.model.Voice
import com.sagun12.vozemcena.domain.model.VoiceProvider
import com.sagun12.vozemcena.ui.voices.AudioRecorder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class VoiceCloneUiState(
    val voiceName: String = "",
    val voiceDescription: String = "",
    val language: String = "pt",
    val recordedAudioFile: File? = null,
    val isRecording: Boolean = false,
    val recordingDurationSec: Float = 0f,
    val recordingAmplitude: Float = 0f,
    val isUploading: Boolean = false,
    val uploadProgress: Float = 0f,
    val createdVoice: Voice? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class VoiceCloneViewModel(
    private val context: Context,
    private val voiceRepository: VoiceRepository,
    private val cartesiaRetrofitService: CartesiaRetrofitService,
    private val settingsStore: CartesiaSettingsStore
) : ViewModel() {

    val audioRecorder = AudioRecorder(context)

    private val _uiState = MutableStateFlow(VoiceCloneUiState())
    val uiState: StateFlow<VoiceCloneUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            audioRecorder.isRecording.collect { recording ->
                _uiState.value = _uiState.value.copy(isRecording = recording)
            }
        }
        viewModelScope.launch {
            audioRecorder.recordDurationSec.collect { duration ->
                _uiState.value = _uiState.value.copy(recordingDurationSec = duration)
            }
        }
        viewModelScope.launch {
            audioRecorder.amplitude.collect { amp ->
                _uiState.value = _uiState.value.copy(recordingAmplitude = amp)
            }
        }
    }

    fun onVoiceNameChange(name: String) {
        _uiState.value = _uiState.value.copy(voiceName = name)
    }

    fun onVoiceDescriptionChange(desc: String) {
        _uiState.value = _uiState.value.copy(voiceDescription = desc)
    }

    fun onLanguageChange(lang: String) {
        _uiState.value = _uiState.value.copy(language = lang)
    }

    fun startRecording() {
        val audioDir = File(context.cacheDir, "voice_samples").apply { mkdirs() }
        val sampleFile = File(audioDir, "voice_sample_${UUID.randomUUID()}.m4a")
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            recordedAudioFile = null,
            createdVoice = null
        )
        audioRecorder.startRecording(sampleFile, viewModelScope)
    }

    fun stopRecording() {
        val recordedFile = audioRecorder.stopRecording()
        if (recordedFile != null && recordedFile.exists()) {
            _uiState.value = _uiState.value.copy(
                recordedAudioFile = recordedFile
            )
        }
    }

    fun discardRecording() {
        audioRecorder.stopRecording()
        _uiState.value.recordedAudioFile?.let {
            runCatching { it.delete() }
        }
        _uiState.value = _uiState.value.copy(
            recordedAudioFile = null,
            recordingDurationSec = 0f,
            errorMessage = null
        )
    }

    fun submitVoiceClone() {
        val name = _uiState.value.voiceName.trim()
        val desc = _uiState.value.voiceDescription.trim()
        val file = _uiState.value.recordedAudioFile

        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Por favor, digite o nome da voz.")
            return
        }

        if (file == null || !file.exists() || _uiState.value.recordingDurationSec < 3.0f) {
            _uiState.value = _uiState.value.copy(errorMessage = "Grave pelo menos 3 a 10 segundos de áudio para clonagem.")
            return
        }

        _uiState.value = _uiState.value.copy(
            isUploading = true,
            errorMessage = null,
            successMessage = null
        )

        viewModelScope.launch {
            val hasApiKey = settingsStore.hasValidApiKey()

            if (hasApiKey) {
                // Call Cartesia API via Retrofit service
                val result = cartesiaRetrofitService.cloneVoiceFromClip(
                    audioFile = file,
                    name = name,
                    description = desc,
                    language = _uiState.value.language
                )

                if (result.isSuccess) {
                    val cloneResponse = result.getOrNull()!!
                    val newVoice = Voice(
                        id = cloneResponse.id,
                        name = cloneResponse.name.ifBlank { name },
                        description = desc.ifBlank { "Voz clonada com IA Cartesia" },
                        language = _uiState.value.language,
                        gender = "Personalizada",
                        provider = VoiceProvider.CARTESIA,
                        tags = listOf("Cartesia AI", "Voz Clonada", _uiState.value.language),
                        isCustom = true,
                        localAudioPath = file.absolutePath
                    )

                    // Persist to local database
                    voiceRepository.saveVoice(newVoice)

                    _uiState.value = _uiState.value.copy(
                        isUploading = false,
                        createdVoice = newVoice,
                        successMessage = "Perfil de voz '${newVoice.name}' criado com sucesso no Cartesia!"
                    )
                } else {
                    val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Erro ao enviar amostra para Cartesia."
                    _uiState.value = _uiState.value.copy(
                        isUploading = false,
                        errorMessage = errorMsg
                    )
                }
            } else {
                // Local profile fallback when no API key is set
                val savedFile = File(context.filesDir, "custom_voice_${UUID.randomUUID()}.m4a")
                file.copyTo(savedFile, overwrite = true)

                val localVoice = Voice(
                    id = "custom-${UUID.randomUUID()}",
                    name = name,
                    description = desc.ifBlank { "Voz gravada no dispositivo" },
                    language = _uiState.value.language,
                    gender = "Personalizada",
                    provider = VoiceProvider.LOCAL_RECORDING,
                    tags = listOf("Gravada Local", "Personalizada"),
                    isCustom = true,
                    localAudioPath = savedFile.absolutePath
                )

                voiceRepository.saveVoice(localVoice)

                _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    createdVoice = localVoice,
                    successMessage = "Voz salva localmente! (Configure sua chave Cartesia em Ajustes para sincronizar com a IA)."
                )
            }
        }
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorder.stopRecording()
    }
}

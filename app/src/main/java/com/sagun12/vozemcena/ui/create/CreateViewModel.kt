package com.sagun12.vozemcena.ui.create

import android.content.Context
import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sagun12.vozemcena.data.repository.ProjectRepository
import com.sagun12.vozemcena.data.repository.VoiceRepository
import com.sagun12.vozemcena.domain.model.AspectRatio
import com.sagun12.vozemcena.domain.model.ExportProgress
import com.sagun12.vozemcena.domain.model.MediaType
import com.sagun12.vozemcena.domain.model.Project
import com.sagun12.vozemcena.domain.model.ProjectStatus
import com.sagun12.vozemcena.domain.model.Scene
import com.sagun12.vozemcena.domain.model.TransitionType
import com.sagun12.vozemcena.domain.model.Voice
import com.sagun12.vozemcena.video.VideoExporter
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class CreateUiState(
    val project: Project? = null,
    val selectedSceneIndex: Int = 0,
    val isPlayingPreview: Boolean = false,
    val playingSceneId: String? = null,
    val exportProgress: ExportProgress? = null,
    val isExporting: Boolean = false,
    val infoMessage: String? = null,
    val errorMessage: String? = null
)

class CreateViewModel(
    private val context: Context,
    private val projectId: String,
    private val projectRepository: ProjectRepository,
    private val voiceRepository: VoiceRepository
) : ViewModel() {

    private val videoExporter = VideoExporter(context, voiceRepository)
    private var mediaPlayer: MediaPlayer? = null
    private var exportJob: Job? = null

    val availableVoices: StateFlow<List<Voice>> = voiceRepository.getAllVoices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(CreateUiState())
    val uiState: StateFlow<CreateUiState> = _uiState.asStateFlow()

    init {
        loadProject()
    }

    private fun loadProject() {
        viewModelScope.launch {
            projectRepository.getProjectById(projectId).collect { proj ->
                if (proj != null) {
                    _uiState.value = _uiState.value.copy(project = proj)
                } else {
                    // Create new project with this id if not found
                    val newProj = projectRepository.createNewProject(title = "Projeto Sem Título")
                    _uiState.value = _uiState.value.copy(project = newProj)
                }
            }
        }
    }

    fun updateProjectTitle(title: String) {
        val current = _uiState.value.project ?: return
        val updated = current.copy(title = title)
        _uiState.value = _uiState.value.copy(project = updated)
        saveCurrentProject(updated)
    }

    fun updateAspectRatio(aspectRatio: AspectRatio) {
        val current = _uiState.value.project ?: return
        val updated = current.copy(aspectRatio = aspectRatio)
        _uiState.value = _uiState.value.copy(project = updated)
        saveCurrentProject(updated)
    }

    fun toggleSubtitles(include: Boolean) {
        val current = _uiState.value.project ?: return
        val updated = current.copy(includeSubtitles = include)
        _uiState.value = _uiState.value.copy(project = updated)
        saveCurrentProject(updated)
    }

    fun selectScene(index: Int) {
        _uiState.value = _uiState.value.copy(selectedSceneIndex = index)
    }

    fun addScene() {
        val current = _uiState.value.project ?: return
        val newScene = Scene(
            id = UUID.randomUUID().toString(),
            projectId = current.id,
            sequenceOrder = current.scenes.size,
            mediaUri = null,
            mediaType = MediaType.IMAGE,
            textScript = "",
            voiceId = "cartesia-pt-helena",
            voiceName = "Helena (Narradora)",
            durationSec = 4.0f,
            transition = TransitionType.FADE
        )
        val updatedScenes = current.scenes + newScene
        val updatedProject = current.copy(scenes = updatedScenes)
        _uiState.value = _uiState.value.copy(
            project = updatedProject,
            selectedSceneIndex = updatedScenes.lastIndex
        )
        saveCurrentProject(updatedProject)
    }

    fun updateScene(scene: Scene) {
        val current = _uiState.value.project ?: return
        val updatedScenes = current.scenes.map { if (it.id == scene.id) scene else it }
        val updatedProject = current.copy(scenes = updatedScenes)
        _uiState.value = _uiState.value.copy(project = updatedProject)
        saveCurrentProject(updatedProject)
    }

    fun deleteScene(sceneId: String) {
        val current = _uiState.value.project ?: return
        if (current.scenes.size <= 1) {
            _uiState.value = _uiState.value.copy(errorMessage = "O projeto precisa ter pelo menos 1 cena.")
            return
        }
        val updatedScenes = current.scenes.filter { it.id != sceneId }
            .mapIndexed { idx, sc -> sc.copy(sequenceOrder = idx) }
        val updatedProject = current.copy(scenes = updatedScenes)
        val newIndex = _uiState.value.selectedSceneIndex.coerceAtMost(updatedScenes.lastIndex)
        _uiState.value = _uiState.value.copy(
            project = updatedProject,
            selectedSceneIndex = newIndex
        )
        saveCurrentProject(updatedProject)
    }

    fun previewSceneVoice(scene: Scene) {
        if (_uiState.value.playingSceneId == scene.id) {
            stopAudio()
            return
        }
        stopAudio()
        _uiState.value = _uiState.value.copy(playingSceneId = scene.id, isPlayingPreview = true)

        viewModelScope.launch {
            val previewText = scene.textScript.ifBlank { "Esta cena ainda não tem texto digitado." }
            val tempAudioFile = File(context.cacheDir, "scene_prev_${scene.id}.wav")
            val result = voiceRepository.synthesizeSpeechToFile(previewText, scene.voiceId, tempAudioFile)

            if (result.isSuccess) {
                try {
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(tempAudioFile.absolutePath)
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
        _uiState.value = _uiState.value.copy(playingSceneId = null, isPlayingPreview = false)
    }

    fun exportVideo() {
        val current = _uiState.value.project ?: return
        if (current.scenes.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Adicione ao menos uma cena para exportar.")
            return
        }

        stopAudio()
        _uiState.value = _uiState.value.copy(isExporting = true)

        exportJob?.cancel()
        exportJob = viewModelScope.launch {
            videoExporter.exportProject(current).collect { progress ->
                _uiState.value = _uiState.value.copy(exportProgress = progress)
                if (progress.status == ProjectStatus.COMPLETED) {
                    projectRepository.updateProjectStatus(current.id, ProjectStatus.COMPLETED, progress.outputUri)
                    _uiState.value = _uiState.value.copy(isExporting = false)
                } else if (progress.status == ProjectStatus.ERROR) {
                    projectRepository.updateProjectStatus(current.id, ProjectStatus.ERROR)
                    _uiState.value = _uiState.value.copy(isExporting = false)
                }
            }
        }
    }

    fun dismissExportDialog() {
        _uiState.value = _uiState.value.copy(exportProgress = null)
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(infoMessage = null, errorMessage = null)
    }

    private fun saveCurrentProject(project: Project) {
        viewModelScope.launch {
            projectRepository.saveProject(project)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAudio()
        exportJob?.cancel()
    }
}

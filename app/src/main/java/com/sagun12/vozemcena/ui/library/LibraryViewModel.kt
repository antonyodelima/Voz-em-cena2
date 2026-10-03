package com.sagun12.vozemcena.ui.library

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sagun12.vozemcena.data.repository.ProjectRepository
import com.sagun12.vozemcena.domain.model.Project
import com.sagun12.vozemcena.video.MediaStoreExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class LibraryUiState(
    val selectedTab: Int = 0, // 0 = Todos os Projetos, 1 = Exportados, 2 = Rascunhos
    val searchQuery: String = "",
    val message: String? = null
)

class LibraryViewModel(
    private val context: Context,
    private val projectRepository: ProjectRepository
) : ViewModel() {

    private val mediaStoreExporter = MediaStoreExporter(context)

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val allProjects = projectRepository.getAllProjects()

    val filteredProjects: StateFlow<List<Project>> = combine(allProjects, _uiState) { projects, state ->
        projects.filter { proj ->
            val matchesSearch = state.searchQuery.isBlank() ||
                    proj.title.contains(state.searchQuery, ignoreCase = true) ||
                    proj.description.contains(state.searchQuery, ignoreCase = true)

            val matchesTab = when (state.selectedTab) {
                1 -> proj.outputVideoUri != null // Exportados
                2 -> proj.outputVideoUri == null // Rascunhos
                else -> true
            }

            matchesSearch && matchesTab
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onTabSelected(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tabIndex)
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            projectRepository.deleteProject(projectId)
            _uiState.value = _uiState.value.copy(message = "Projeto excluído.")
        }
    }

    fun duplicateProject(projectId: String) {
        viewModelScope.launch {
            val duplicated = projectRepository.duplicateProject(projectId)
            if (duplicated != null) {
                _uiState.value = _uiState.value.copy(message = "Projeto duplicado com sucesso.")
            }
        }
    }

    fun shareProjectVideo(project: Project) {
        val uriStr = project.outputVideoUri ?: return
        val file = File(uriStr)
        if (file.exists()) {
            mediaStoreExporter.shareVideo(context, file, project.title)
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}

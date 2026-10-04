package com.sagun12.vozemcena.ui.admin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sagun12.vozemcena.data.remote.CartesiaApi
import com.sagun12.vozemcena.data.remote.CronJobDto
import com.sagun12.vozemcena.data.remote.CronJobPreset
import com.sagun12.vozemcena.data.repository.CronJobRepository
import com.sagun12.vozemcena.data.settings.CartesiaSettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class AdminUiState(
    val currentApiKey: String = "",
    val hasKeyConfigured: Boolean = false,
    val selectedModel: String = "sonic-multilingual",
    val isTestingConnection: Boolean = false,
    val connectionStatus: String? = null,
    val isSuccess: Boolean = false,
    val cacheSizeMb: Double = 0.0,
    val infoMessage: String? = null,
    val hasCronKey: Boolean = false,
    val cronJobs: List<CronJobDto> = emptyList(),
    val isLoadingCronJobs: Boolean = false,
    val isCreatingCronJob: Boolean = false
)

class AdminViewModel(
    private val context: Context,
    private val settingsStore: CartesiaSettingsStore,
    private val cartesiaApi: CartesiaApi,
    private val cronJobRepository: CronJobRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AdminUiState(
            currentApiKey = settingsStore.getEffectiveApiKey(),
            hasKeyConfigured = settingsStore.hasValidApiKey(),
            selectedModel = settingsStore.getSelectedModel(),
            cacheSizeMb = calculateCacheSize(),
            hasCronKey = settingsStore.hasValidCronJobApiKey()
        )
    )
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    fun updateApiKey(newKey: String) {
        settingsStore.setApiKey(newKey)
        _uiState.value = _uiState.value.copy(
            currentApiKey = newKey,
            hasKeyConfigured = settingsStore.hasValidApiKey(),
            connectionStatus = null
        )
    }

    fun selectModel(modelId: String) {
        settingsStore.setSelectedModel(modelId)
        _uiState.value = _uiState.value.copy(selectedModel = modelId)
    }

    fun testConnection() {
        if (!settingsStore.hasValidApiKey()) {
            _uiState.value = _uiState.value.copy(
                connectionStatus = "Nenhuma chave de API detectada. Adicione sua chave no painel Secrets do AI Studio ou insira acima.",
                isSuccess = false
            )
            return
        }

        _uiState.value = _uiState.value.copy(isTestingConnection = true, connectionStatus = null)
        viewModelScope.launch {
            try {
                val response = cartesiaApi.listVoices()
                if (response.isSuccessful) {
                    val count = response.body()?.data?.size ?: 0
                    _uiState.value = _uiState.value.copy(
                        isTestingConnection = false,
                        connectionStatus = "Conexão com Cartesia AI bem-sucedida! ($count vozes disponíveis)",
                        isSuccess = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isTestingConnection = false,
                        connectionStatus = "Erro ${response.code()}: ${response.message()}",
                        isSuccess = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isTestingConnection = false,
                    connectionStatus = "Falha ao conectar: ${e.localizedMessage}",
                    isSuccess = false
                )
            }
        }
    }

    fun clearAppCache() {
        viewModelScope.launch {
            val cacheDir = context.cacheDir
            cacheDir.deleteRecursively()
            cacheDir.mkdirs()

            val exportsDir = File(context.filesDir, "exports")
            exportsDir.deleteRecursively()
            exportsDir.mkdirs()

            _uiState.value = _uiState.value.copy(
                cacheSizeMb = 0.0,
                infoMessage = "Cache e arquivos temporários limpos."
            )
        }
    }

    /** Atualiza a lista de tarefas do cron-job.org. */
    fun loadCronJobs() {
        if (!settingsStore.hasValidCronJobApiKey()) {
            _uiState.value = _uiState.value.copy(
                hasCronKey = false,
                cronJobs = emptyList(),
                isLoadingCronJobs = false,
                infoMessage = "Adicione CRONJOB_API_KEY no painel de Secrets/Keys para gerenciar agendamentos."
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoadingCronJobs = true)
        viewModelScope.launch {
            cronJobRepository.listJobs()
                .onSuccess { jobs ->
                    _uiState.value = _uiState.value.copy(
                        hasCronKey = true,
                        cronJobs = jobs,
                        isLoadingCronJobs = false
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingCronJobs = false,
                        infoMessage = error.localizedMessage ?: "Falha ao listar agendamentos."
                    )
                }
        }
    }

    /** Cria uma tarefa habilitada com o preset escolhido e recarrega a lista. */
    fun createCronJob(title: String, url: String, preset: CronJobPreset) {
        if (url.isBlank()) {
            _uiState.value = _uiState.value.copy(infoMessage = "Informe a URL que será chamada pelo agendamento.")
            return
        }

        _uiState.value = _uiState.value.copy(isCreatingCronJob = true)
        viewModelScope.launch {
            cronJobRepository.createJob(title = title.ifBlank { "Voz em Cena" }, url = url.trim(), preset = preset)
                .onSuccess { jobId ->
                    _uiState.value = _uiState.value.copy(
                        isCreatingCronJob = false,
                        infoMessage = "Agendamento criado (job $jobId)."
                    )
                    loadCronJobs()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isCreatingCronJob = false,
                        infoMessage = error.localizedMessage ?: "Falha ao criar agendamento."
                    )
                }
        }
    }

    /** Habilita/desabilita uma tarefa existente. */
    fun toggleCronJob(job: CronJobDto) {
        val jobId = job.jobId ?: return
        viewModelScope.launch {
            cronJobRepository.setJobEnabled(jobId = jobId, enabled = !job.enabled)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        infoMessage = if (!job.enabled) "Agendamento ativado." else "Agendamento pausado."
                    )
                    loadCronJobs()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        infoMessage = error.localizedMessage ?: "Falha ao atualizar agendamento."
                    )
                }
        }
    }

    /** Exclui uma tarefa existente. */
    fun deleteCronJob(job: CronJobDto) {
        val jobId = job.jobId ?: return
        viewModelScope.launch {
            cronJobRepository.deleteJob(jobId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(infoMessage = "Agendamento excluído.")
                    loadCronJobs()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        infoMessage = error.localizedMessage ?: "Falha ao excluir agendamento."
                    )
                }
        }
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(infoMessage = null)
    }

    private fun calculateCacheSize(): Double {
        val cacheBytes = context.cacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
        val exportBytes = File(context.filesDir, "exports").walkTopDown().filter { it.isFile }.map { it.length() }.sum()
        return (cacheBytes + exportBytes) / (1024.0 * 1024.0)
    }
}

package com.sagun12.vozemcena.ui.admin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sagun12.vozemcena.data.remote.CartesiaApi
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
    val infoMessage: String? = null
)

class AdminViewModel(
    private val context: Context,
    private val settingsStore: CartesiaSettingsStore,
    private val cartesiaApi: CartesiaApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AdminUiState(
            currentApiKey = settingsStore.getEffectiveApiKey(),
            hasKeyConfigured = settingsStore.hasValidApiKey(),
            selectedModel = settingsStore.getSelectedModel(),
            cacheSizeMb = calculateCacheSize()
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

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(infoMessage = null)
    }

    private fun calculateCacheSize(): Double {
        val cacheBytes = context.cacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
        val exportBytes = File(context.filesDir, "exports").walkTopDown().filter { it.isFile }.map { it.length() }.sum()
        return (cacheBytes + exportBytes) / (1024.0 * 1024.0)
    }
}

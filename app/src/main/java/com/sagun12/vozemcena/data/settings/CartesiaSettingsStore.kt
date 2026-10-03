package com.sagun12.vozemcena.data.settings

import android.content.Context
import android.content.SharedPreferences
import com.sagun12.vozemcena.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CartesiaSettingsStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("cartesia_settings", Context.MODE_PRIVATE)

    private val _apiKeyFlow = MutableStateFlow(getEffectiveApiKey())
    val apiKeyFlow: StateFlow<String> = _apiKeyFlow.asStateFlow()

    private val _selectedModelFlow = MutableStateFlow(getSelectedModel())
    val selectedModelFlow: StateFlow<String> = _selectedModelFlow.asStateFlow()

    fun getEffectiveApiKey(): String {
        val userKey = prefs.getString(KEY_API_KEY, "") ?: ""
        if (userKey.isNotBlank()) return userKey.trim()

        // Fallback to BuildConfig if configured via Secrets Plugin
        val buildConfigKey = runCatching {
            val field = BuildConfig::class.java.getField("CARTESIA_API_KEY")
            field.get(null) as? String
        }.getOrNull() ?: ""

        return if (buildConfigKey != "YOUR_CARTESIA_API_KEY") buildConfigKey.trim() else ""
    }

    fun hasValidApiKey(): Boolean {
        return getEffectiveApiKey().isNotBlank()
    }

    fun setApiKey(apiKey: String) {
        prefs.edit().putString(KEY_API_KEY, apiKey.trim()).apply()
        _apiKeyFlow.value = getEffectiveApiKey()
    }

    fun getSelectedModel(): String {
        return prefs.getString(KEY_SELECTED_MODEL, "sonic-multilingual") ?: "sonic-multilingual"
    }

    fun setSelectedModel(model: String) {
        prefs.edit().putString(KEY_SELECTED_MODEL, model).apply()
        _selectedModelFlow.value = model
    }

    fun getPreferredLanguage(): String {
        return prefs.getString(KEY_PREFERRED_LANG, "pt") ?: "pt"
    }

    fun setPreferredLanguage(lang: String) {
        prefs.edit().putString(KEY_PREFERRED_LANG, lang).apply()
    }

    fun clearApiKey() {
        prefs.edit().remove(KEY_API_KEY).apply()
        _apiKeyFlow.value = getEffectiveApiKey()
    }

    companion object {
        private const val KEY_API_KEY = "cartesia_custom_api_key"
        private const val KEY_SELECTED_MODEL = "cartesia_model"
        private const val KEY_PREFERRED_LANG = "cartesia_preferred_lang"
    }
}

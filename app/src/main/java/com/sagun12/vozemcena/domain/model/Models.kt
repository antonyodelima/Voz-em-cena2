package com.sagun12.vozemcena.domain.model

enum class MediaType {
    IMAGE,
    VIDEO
}

enum class TransitionType {
    NONE,
    FADE,
    CROSSFADE
}

enum class ProjectStatus {
    DRAFT,
    EXPORTING,
    COMPLETED,
    ERROR
}

enum class AspectRatio(val label: String, val ratio: Float, val width: Int, val height: Int) {
    RATIO_16_9("16:9 (Horizontal)", 16f / 9f, 1920, 1080),
    RATIO_9_16("9:16 (Vertical / Reels / TikTok)", 9f / 16f, 1080, 1920),
    RATIO_1_1("1:1 (Quadrado / Feed)", 1f, 1080, 1080);

    companion object {
        fun fromString(value: String?): AspectRatio {
            return entries.find { it.name.equals(value, ignoreCase = true) || it.label.startsWith(value ?: "") }
                ?: RATIO_16_9
        }
    }
}

enum class VoiceProvider {
    CARTESIA,
    LOCAL_RECORDING,
    SYSTEM_TTS
}

data class Voice(
    val id: String,
    val name: String,
    val description: String,
    val language: String = "pt-BR",
    val gender: String = "Neutro",
    val provider: VoiceProvider = VoiceProvider.CARTESIA,
    val previewAudioUrl: String? = null,
    val tags: List<String> = emptyList(),
    val isCustom: Boolean = false,
    val localAudioPath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class Scene(
    val id: String,
    val projectId: String,
    val sequenceOrder: Int,
    val mediaUri: String?,
    val mediaType: MediaType = MediaType.IMAGE,
    val textScript: String = "",
    val voiceId: String? = null,
    val voiceName: String? = null,
    val durationSec: Float = 4.0f,
    val transition: TransitionType = TransitionType.FADE,
    val volume: Float = 1.0f,
    val generatedAudioPath: String? = null
)

data class Project(
    val id: String,
    val title: String,
    val description: String = "",
    val aspectRatio: AspectRatio = AspectRatio.RATIO_16_9,
    val status: ProjectStatus = ProjectStatus.DRAFT,
    val scenes: List<Scene> = emptyList(),
    val outputVideoUri: String? = null,
    val backgroundMusicUri: String? = null,
    val backgroundMusicVolume: Float = 0.2f,
    val includeSubtitles: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val totalDurationSec: Float
        get() = scenes.sumOf { it.durationSec.toDouble() }.toFloat()
}

data class ExportProgress(
    val status: ProjectStatus = ProjectStatus.DRAFT,
    val progress: Float = 0f,
    val currentStep: String = "",
    val outputUri: String? = null,
    val errorMessage: String? = null
)

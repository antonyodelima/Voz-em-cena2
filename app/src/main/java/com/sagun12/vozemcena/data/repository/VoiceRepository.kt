package com.sagun12.vozemcena.data.repository

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.sagun12.vozemcena.data.local.VoiceDao
import com.sagun12.vozemcena.data.local.toDomain
import com.sagun12.vozemcena.data.local.toEntity
import com.sagun12.vozemcena.data.remote.CartesiaApi
import com.sagun12.vozemcena.data.remote.CartesiaOutputFormat
import com.sagun12.vozemcena.data.remote.CartesiaRetrofitService
import com.sagun12.vozemcena.data.remote.CartesiaTtsRequest
import com.sagun12.vozemcena.data.remote.CartesiaVoiceSpec
import com.sagun12.vozemcena.data.settings.CartesiaSettingsStore
import com.sagun12.vozemcena.domain.model.Voice
import com.sagun12.vozemcena.domain.model.VoiceProvider
import com.sagun12.vozemcena.video.WavAudio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume

class VoiceRepository(
    private val context: Context,
    private val voiceDao: VoiceDao,
    private val cartesiaApi: CartesiaApi,
    private val settingsStore: CartesiaSettingsStore,
    private val cartesiaService: CartesiaRetrofitService = CartesiaRetrofitService(settingsStore)
) {
    private val tag = "VoiceRepository"
    private var textToSpeech: TextToSpeech? = null
    private var ttsInitialized = false

    init {
        initSystemTts()
    }

    private fun initSystemTts() {
        textToSpeech = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale("pt", "BR")
                ttsInitialized = true
            }
        }
    }

    fun getAllVoices(): Flow<List<Voice>> {
        return voiceDao.getAllVoices().map { list -> list.map { it.toDomain() } }
    }

    fun getCustomVoices(): Flow<List<Voice>> {
        return voiceDao.getCustomVoices().map { list -> list.map { it.toDomain() } }
    }

    suspend fun getVoiceById(voiceId: String): Voice? {
        return withContext(Dispatchers.IO) {
            voiceDao.getVoiceById(voiceId)?.toDomain()
        }
    }

    suspend fun saveVoice(voice: Voice) {
        withContext(Dispatchers.IO) {
            voiceDao.insertVoice(voice.toEntity())
        }
    }

    suspend fun deleteVoice(voiceId: String) {
        withContext(Dispatchers.IO) {
            val voice = voiceDao.getVoiceById(voiceId)
            voice?.localAudioPath?.let { path ->
                runCatching { File(path).delete() }
            }
            voiceDao.deleteVoiceById(voiceId)
        }
    }

    suspend fun fetchRemoteCartesiaVoices(): Result<List<Voice>> = withContext(Dispatchers.IO) {
        if (!settingsStore.hasValidApiKey()) {
            return@withContext Result.failure(IllegalStateException("API Key do Cartesia não configurada."))
        }
        return@withContext runCatching {
            val response = cartesiaApi.listVoices()
            if (response.isSuccessful && response.body() != null) {
                val dtoList = response.body()!!.data
                val remoteVoices = dtoList.map { dto ->
                    Voice(
                        id = dto.id,
                        name = dto.name,
                        description = dto.description ?: "Voz Cartesia AI",
                        language = dto.language ?: "pt",
                        gender = dto.gender ?: "Neutro",
                        provider = VoiceProvider.CARTESIA,
                        tags = listOf("Cartesia AI", dto.language ?: "pt"),
                        isCustom = false
                    )
                }
                voiceDao.insertVoices(remoteVoices.map { it.toEntity() })
                remoteVoices
            } else {
                throw IllegalStateException("Falha ao buscar vozes: ${response.code()} ${response.message()}")
            }
        }
    }

    suspend fun cloneVoice(
        name: String,
        description: String,
        audioFile: File,
        language: String = "pt"
    ): Result<Voice> = withContext(Dispatchers.IO) {
        if (!settingsStore.hasValidApiKey()) {
            // Local custom voice fallback if no Cartesia API Key
            val savedFile = File(context.filesDir, "custom_voice_${UUID.randomUUID()}.wav")
            audioFile.copyTo(savedFile, overwrite = true)

            val customVoice = Voice(
                id = "custom-${UUID.randomUUID()}",
                name = name,
                description = description,
                language = language,
                gender = "Personalizada",
                provider = VoiceProvider.LOCAL_RECORDING,
                tags = listOf("Personalizada", "Gravada Localmente"),
                isCustom = true,
                localAudioPath = savedFile.absolutePath
            )
            saveVoice(customVoice)
            return@withContext Result.success(customVoice)
        }

        return@withContext runCatching {
            val requestBody = audioFile.asRequestBody("audio/wav".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("clip", audioFile.name, requestBody)
            val nameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
            val descBody = description.toRequestBody("text/plain".toMediaTypeOrNull())
            val langBody = language.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = cartesiaApi.cloneVoiceFromClip(filePart, nameBody, descBody, langBody)
            if (response.isSuccessful && response.body() != null) {
                val clonedDto = response.body()!!
                val newVoice = Voice(
                    id = clonedDto.id,
                    name = clonedDto.name,
                    description = description,
                    language = language,
                    gender = "Clonada",
                    provider = VoiceProvider.CARTESIA,
                    tags = listOf("Cartesia Clonada", language),
                    isCustom = true,
                    localAudioPath = audioFile.absolutePath
                )
                saveVoice(newVoice)
                newVoice
            } else {
                throw IllegalStateException("Erro ao clonar voz no Cartesia: ${response.code()} ${response.message()}")
            }
        }
    }

    suspend fun synthesizeSpeechToFile(
        text: String,
        voiceId: String?,
        outputFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        if (text.isBlank()) {
            // Generate 1 second silence WAV
            WavAudio.createSilenceWav(outputFile, durationSeconds = 1.0f)
            return@withContext Result.success(outputFile)
        }

        val hasApiKey = settingsStore.hasValidApiKey()
        val voice = voiceId?.let { voiceDao.getVoiceById(it)?.toDomain() }

        if (hasApiKey && voice?.provider != VoiceProvider.SYSTEM_TTS) {
            val cartesiaResult = synthesizeWithCartesia(text, voiceId ?: "cartesia-pt-helena", outputFile)
            if (cartesiaResult.isSuccess) {
                return@withContext cartesiaResult
            }
            Log.w(tag, "Cartesia TTS falhou, tentando fallback local...", cartesiaResult.exceptionOrNull())
        }

        // Fallback: System TextToSpeech synthesize to file
        return@withContext synthesizeWithSystemTts(text, outputFile)
    }

    private suspend fun synthesizeWithCartesia(
        text: String,
        voiceId: String,
        outputFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        // Map known mock voice IDs to standard Cartesia voice IDs or use requested ID
        val effectiveVoiceId = when (voiceId) {
            "cartesia-pt-helena" -> "794f9389-aac1-45b6-b726-9d9369183238" // Portuguese natural voice
            "cartesia-pt-thiago" -> "a0e99841-438c-4a64-b679-ae501e7d6091" // Deep trailer voice
            "cartesia-pt-sofia" -> "b7d50908-b47c-442d-ad6f-1a7ecab0b519"
            "cartesia-pt-gabriel" -> "638efaaa-4d0c-442e-b701-3fae16aa63c0"
            "cartesia-pt-beatriz" -> "ee7ea9f8-c0c1-498c-9f7d-4e929d2de252"
            else -> voiceId
        }

        cartesiaService.synthesizeSpeechToFile(
            text = text,
            voiceId = effectiveVoiceId,
            outputFile = outputFile,
            modelId = settingsStore.getSelectedModel(),
            language = if (voiceId.contains("-en-")) "en" else "pt",
            sampleRate = 44100
        )
    }

    private suspend fun synthesizeWithSystemTts(
        text: String,
        outputFile: File
    ): Result<File> = suspendCancellableCoroutine { continuation ->
        val tts = textToSpeech
        if (tts == null || !ttsInitialized) {
            // Create a pleasant synthetic sine chime WAV as ultimate fallback
            WavAudio.createSyntheticSpeechTone(outputFile, text.length * 0.08f + 1.0f)
            continuation.resume(Result.success(outputFile))
            return@suspendCancellableCoroutine
        }

        outputFile.parentFile?.mkdirs()
        val utteranceId = "tts_${UUID.randomUUID()}"

        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}

            override fun onDone(id: String?) {
                if (id == utteranceId) {
                    if (outputFile.exists() && outputFile.length() > 44) {
                        continuation.resume(Result.success(outputFile))
                    } else {
                        // Fallback tone
                        WavAudio.createSyntheticSpeechTone(outputFile, 2.0f)
                        continuation.resume(Result.success(outputFile))
                    }
                }
            }

            override fun onError(id: String?) {
                if (id == utteranceId) {
                    WavAudio.createSyntheticSpeechTone(outputFile, 2.0f)
                    continuation.resume(Result.success(outputFile))
                }
            }
        })

        val result = tts.synthesizeToFile(text, null, outputFile, utteranceId)
        if (result != TextToSpeech.SUCCESS) {
            WavAudio.createSyntheticSpeechTone(outputFile, 2.0f)
            continuation.resume(Result.success(outputFile))
        }
    }
}

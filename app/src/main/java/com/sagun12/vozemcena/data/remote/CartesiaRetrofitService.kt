package com.sagun12.vozemcena.data.remote

import android.util.Log
import com.sagun12.vozemcena.data.settings.CartesiaSettingsStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * Serviço Retrofit para comunicação com a API do Cartesia AI.
 * Permite envio de textos para síntese neural e recebimento de fluxos/arquivos de áudio (WAV/PCM).
 */
class CartesiaRetrofitService(
    private val apiKeyProvider: () -> String
) {
    /**
     * Construtor de conveniência que recebe a chave de API diretamente em formato String.
     */
    constructor(apiKey: String) : this({ apiKey })

    /**
     * Construtor de conveniência que recebe o CartesiaSettingsStore da aplicação.
     */
    constructor(settingsStore: CartesiaSettingsStore) : this({ settingsStore.getEffectiveApiKey() })

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val key = apiKeyProvider().trim()

        val requestBuilder = originalRequest.newBuilder()
            .header("X-API-Key", key)
            .header("Authorization", "Bearer $key")
            .header("Cartesia-Version", "2024-06-10")
            .header("Content-Type", "application/json")

        chain.proceed(requestBuilder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.HEADERS
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val api: CartesiaApi = Retrofit.Builder()
        .baseUrl("https://api.cartesia.ai/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(CartesiaApi::class.java)

    /**
     * Envia um texto para a API do Cartesia e retorna os bytes do áudio sintetizado em formato WAV.
     */
    suspend fun synthesizeSpeech(
        text: String,
        voiceId: String,
        modelId: String = "sonic-multilingual",
        language: String = "pt",
        sampleRate: Int = 44100
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        val currentKey = apiKeyProvider()
        if (currentKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Chave da API Cartesia não informada.")
            )
        }

        runCatching {
            val request = CartesiaTtsRequest(
                modelId = modelId,
                transcript = text,
                voice = CartesiaVoiceSpec(mode = "id", id = voiceId),
                outputFormat = CartesiaOutputFormat(
                    container = "wav",
                    encoding = "pcm_s16le",
                    sampleRate = sampleRate
                ),
                language = language
            )

            val response = api.synthesizeBytes(request)
            if (response.isSuccessful && response.body() != null) {
                response.body()!!.bytes()
            } else {
                val errorBody = response.errorBody()?.string() ?: response.message()
                throw IllegalStateException("Erro Cartesia HTTP ${response.code()}: $errorBody")
            }
        }
    }

    /**
     * Envia o texto para a API e grava o áudio sintetizado diretamente em um arquivo destino.
     */
    suspend fun synthesizeSpeechToFile(
        text: String,
        voiceId: String,
        outputFile: File,
        modelId: String = "sonic-multilingual",
        language: String = "pt",
        sampleRate: Int = 44100
    ): Result<File> = withContext(Dispatchers.IO) {
        val currentKey = apiKeyProvider()
        if (currentKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Chave da API Cartesia não informada.")
            )
        }

        runCatching {
            val request = CartesiaTtsRequest(
                modelId = modelId,
                transcript = text,
                voice = CartesiaVoiceSpec(mode = "id", id = voiceId),
                outputFormat = CartesiaOutputFormat(
                    container = "wav",
                    encoding = "pcm_s16le",
                    sampleRate = sampleRate
                ),
                language = language
            )

            val response = api.synthesizeBytes(request)
            if (response.isSuccessful && response.body() != null) {
                outputFile.parentFile?.mkdirs()
                response.body()!!.byteStream().use { input ->
                    FileOutputStream(outputFile).use { output ->
                        input.copyTo(output)
                    }
                }
                outputFile
            } else {
                val errorBody = response.errorBody()?.string() ?: response.message()
                throw IllegalStateException("Erro Cartesia HTTP ${response.code()}: $errorBody")
            }
        }
    }

    /**
     * Retorna a lista de vozes disponíveis na API do Cartesia.
     */
    suspend fun listVoices(): Result<List<CartesiaVoiceDto>> = withContext(Dispatchers.IO) {
        val currentKey = apiKeyProvider()
        if (currentKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Chave da API Cartesia não configurada.")
            )
        }

        runCatching {
            val response = api.listVoices()
            if (response.isSuccessful && response.body() != null) {
                response.body()!!.data
            } else {
                val errorBody = response.errorBody()?.string() ?: response.message()
                throw IllegalStateException("Falha ao obter vozes (${response.code()}): $errorBody")
            }
        }
    }

    /**
     * Clona uma nova voz a partir de um arquivo de áudio gravado e envia para a API do Cartesia.
     */
    suspend fun cloneVoiceFromClip(
        audioFile: File,
        name: String,
        description: String = "",
        language: String = "pt"
    ): Result<CartesiaCloneResponse> = withContext(Dispatchers.IO) {
        val currentKey = apiKeyProvider()
        if (currentKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Chave da API Cartesia não informada.")
            )
        }

        runCatching {
            val requestBody = audioFile.asRequestBody("audio/wav".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("clip", audioFile.name, requestBody)
            val nameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
            val descBody = description.toRequestBody("text/plain".toMediaTypeOrNull())
            val langBody = language.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = api.cloneVoiceFromClip(filePart, nameBody, descBody, langBody)
            if (response.isSuccessful && response.body() != null) {
                response.body()!!
            } else {
                val errorBody = response.errorBody()?.string() ?: response.message()
                throw IllegalStateException("Erro ao clonar voz no Cartesia (${response.code()}): $errorBody")
            }
        }
    }

    companion object {
        fun create(apiKey: String): CartesiaRetrofitService {
            return CartesiaRetrofitService(apiKey)
        }

        fun create(settingsStore: CartesiaSettingsStore): CartesiaRetrofitService {
            return CartesiaRetrofitService(settingsStore)
        }
    }
}

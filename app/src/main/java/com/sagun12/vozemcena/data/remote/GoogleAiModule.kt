package com.sagun12.vozemcena.data.remote

import com.sagun12.vozemcena.data.settings.CartesiaSettingsStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Módulo Retrofit da API Google AI (Gemini).
 *
 * Reaproveita o mesmo stack do Cartesia (Retrofit + OkHttp + Moshi) e a chave
 * lida de [CartesiaSettingsStore], que resolve `GOOGLE_API_KEY` (BuildConfig via
 * Secrets Plugin) ou a chave informada pelo usuário.
 */
class GoogleAiModule(private val settingsStore: CartesiaSettingsStore) {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val api: GoogleAiApi = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(GoogleAiApi::class.java)

    val client: GoogleAiClient = GoogleAiClient(api, settingsStore)
}

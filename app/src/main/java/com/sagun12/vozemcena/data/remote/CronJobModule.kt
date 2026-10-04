package com.sagun12.vozemcena.data.remote

import com.sagun12.vozemcena.data.settings.CartesiaSettingsStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Módulo Retrofit da API do cron-job.org.
 *
 * Reaproveita o mesmo stack do Cartesia/Google AI (Retrofit + OkHttp + Moshi) e
 * resolve a chave lida de [CartesiaSettingsStore], que retorna `CRONJOB_API_KEY`
 * (BuildConfig via Secrets Plugin) ou a chave informada pelo usuário.
 *
 * Autenticação conforme https://docs.cron-job.org/rest-api.html:
 * `Authorization: Bearer <API key>` em todas as requisições.
 */
class CronJobModule(private val settingsStore: CartesiaSettingsStore) {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val key = settingsStore.getEffectiveCronJobApiKey().trim()

        val requestBuilder = originalRequest.newBuilder()
            .header("Authorization", "Bearer $key")

        // A API exige Content-Type: application/json quando há payload.
        if (originalRequest.body != null) {
            requestBuilder.header("Content-Type", "application/json")
        }

        chain.proceed(requestBuilder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val api: CronJobApi = Retrofit.Builder()
        .baseUrl("https://api.cron-job.org/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(CronJobApi::class.java)

    val client: CronJobClient = CronJobClient(api, settingsStore)
}

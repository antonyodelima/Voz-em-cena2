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

class CartesiaModule(private val settingsStore: CartesiaSettingsStore) {

    val retrofitService: CartesiaRetrofitService = CartesiaRetrofitService(settingsStore)

    val api: CartesiaApi get() = retrofitService.api
}

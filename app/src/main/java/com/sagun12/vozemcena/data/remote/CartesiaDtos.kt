package com.sagun12.vozemcena.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CartesiaTtsRequest(
    @Json(name = "model_id") val modelId: String = "sonic-multilingual",
    @Json(name = "transcript") val transcript: String,
    @Json(name = "voice") val voice: CartesiaVoiceSpec,
    @Json(name = "output_format") val outputFormat: CartesiaOutputFormat = CartesiaOutputFormat(),
    @Json(name = "language") val language: String = "pt"
)

@JsonClass(generateAdapter = true)
data class CartesiaVoiceSpec(
    @Json(name = "mode") val mode: String = "id",
    @Json(name = "id") val id: String? = null,
    @Json(name = "embedding") val embedding: List<Float>? = null
)

@JsonClass(generateAdapter = true)
data class CartesiaOutputFormat(
    @Json(name = "container") val container: String = "wav",
    @Json(name = "encoding") val encoding: String = "pcm_s16le",
    @Json(name = "sample_rate") val sampleRate: Int = 44100
)

@JsonClass(generateAdapter = true)
data class CartesiaVoiceDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "language") val language: String? = "pt",
    @Json(name = "gender") val gender: String? = null,
    @Json(name = "is_public") val isPublic: Boolean? = true
)

@JsonClass(generateAdapter = true)
data class CartesiaVoiceListResponse(
    @Json(name = "data") val data: List<CartesiaVoiceDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class CartesiaCloneResponse(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "embedding") val embedding: List<Float>? = null
)

package com.sagun12.vozemcena.data.mock

import com.sagun12.vozemcena.domain.model.AspectRatio
import com.sagun12.vozemcena.domain.model.MediaType
import com.sagun12.vozemcena.domain.model.Project
import com.sagun12.vozemcena.domain.model.ProjectStatus
import com.sagun12.vozemcena.domain.model.Scene
import com.sagun12.vozemcena.domain.model.TransitionType
import com.sagun12.vozemcena.domain.model.Voice
import com.sagun12.vozemcena.domain.model.VoiceProvider
import java.util.UUID

object MockData {
    val defaultVoices: List<Voice> = listOf(
        Voice(
            id = "cartesia-pt-helena",
            name = "Helena (Narradora)",
            description = "Voz feminina suave e expressiva, ideal para narração de histórias, documentários e audiolivros.",
            language = "pt-BR",
            gender = "Feminino",
            provider = VoiceProvider.CARTESIA,
            tags = listOf("Narração", "Suave", "Expressiva", "Documentário"),
            isCustom = false
        ),
        Voice(
            id = "cartesia-pt-thiago",
            name = "Thiago (Cinema & Trailer)",
            description = "Voz masculina encorpada, profunda e cinematográfica, perfeita para trailers e anúncios impactantes.",
            language = "pt-BR",
            gender = "Masculino",
            provider = VoiceProvider.CARTESIA,
            tags = listOf("Cinema", "Trailer", "Grave", "Impactante"),
            isCustom = false
        ),
        Voice(
            id = "cartesia-pt-sofia",
            name = "Sofia (Apresentadora)",
            description = "Tom vibrante, comunicativo e moderno. Excelente para vídeos curtos do TikTok, Reels e tutoriais.",
            language = "pt-BR",
            gender = "Feminino",
            provider = VoiceProvider.CARTESIA,
            tags = listOf("Social Media", "Reels", "Jovem", "Dinâmica"),
            isCustom = false
        ),
        Voice(
            id = "cartesia-pt-gabriel",
            name = "Gabriel (Educacional)",
            description = "Voz clara, didática e confiável, recomendada para cursos, explicativos e podcasts técnicos.",
            language = "pt-BR",
            gender = "Masculino",
            provider = VoiceProvider.CARTESIA,
            tags = listOf("Didático", "Podcast", "Neutro", "Claro"),
            isCustom = false
        ),
        Voice(
            id = "cartesia-pt-beatriz",
            name = "Beatriz (Dublagem Anime/Game)",
            description = "Voz jovem, enérgica e teatral, perfeita para animações, games e dublagem de personagens.",
            language = "pt-BR",
            gender = "Feminino",
            provider = VoiceProvider.CARTESIA,
            tags = listOf("Games", "Anime", "Dublagem", "Energia"),
            isCustom = false
        ),
        Voice(
            id = "cartesia-en-sarah",
            name = "Sarah (Global Storyteller)",
            description = "English native narrator with natural pacing and cinematic tone.",
            language = "en-US",
            gender = "Feminino",
            provider = VoiceProvider.CARTESIA,
            tags = listOf("English", "Storytelling", "Global"),
            isCustom = false
        ),
        Voice(
            id = "cartesia-en-marcus",
            name = "Marcus (Deep Impact)",
            description = "Authoritative and charismatic English voice for promo videos.",
            language = "en-US",
            gender = "Masculino",
            provider = VoiceProvider.CARTESIA,
            tags = listOf("English", "Commercial", "Authoritative"),
            isCustom = false
        ),
        Voice(
            id = "system-default",
            name = "Voz Padrão do Sistema (Offline)",
            description = "Mecanismo TTS local do dispositivo Android. Funciona sem conexão com internet.",
            language = "pt-BR",
            gender = "Neutro",
            provider = VoiceProvider.SYSTEM_TTS,
            tags = listOf("Offline", "Local", "Rápido"),
            isCustom = false
        )
    )

    fun createSampleProjects(): List<Project> {
        val proj1Id = UUID.randomUUID().toString()
        val proj2Id = UUID.randomUUID().toString()

        val sample1 = Project(
            id = proj1Id,
            title = "O Mistério das Estrelas",
            description = "Um conto sci-fi sobre a exploração de galáxias distantes.",
            aspectRatio = AspectRatio.RATIO_16_9,
            status = ProjectStatus.DRAFT,
            scenes = listOf(
                Scene(
                    id = UUID.randomUUID().toString(),
                    projectId = proj1Id,
                    sequenceOrder = 0,
                    mediaUri = null,
                    mediaType = MediaType.IMAGE,
                    textScript = "No silêncio do cosmo, ecos de civilizações antigas sussurram segredos esquecidos.",
                    voiceId = "cartesia-pt-helena",
                    voiceName = "Helena (Narradora)",
                    durationSec = 5.5f,
                    transition = TransitionType.FADE
                ),
                Scene(
                    id = UUID.randomUUID().toString(),
                    projectId = proj1Id,
                    sequenceOrder = 1,
                    mediaUri = null,
                    mediaType = MediaType.IMAGE,
                    textScript = "Nossa jornada apenas começou... prepare-se para o desconhecido.",
                    voiceId = "cartesia-pt-thiago",
                    voiceName = "Thiago (Cinema & Trailer)",
                    durationSec = 4.5f,
                    transition = TransitionType.CROSSFADE
                )
            ),
            createdAt = System.currentTimeMillis() - 86400000L * 2,
            updatedAt = System.currentTimeMillis() - 3600000L * 3
        )

        val sample2 = Project(
            id = proj2Id,
            title = "Dicas Rápidas de Produtividade",
            description = "Vídeo no formato Reels com 3 técnicas para foco diário.",
            aspectRatio = AspectRatio.RATIO_9_16,
            status = ProjectStatus.COMPLETED,
            scenes = listOf(
                Scene(
                    id = UUID.randomUUID().toString(),
                    projectId = proj2Id,
                    sequenceOrder = 0,
                    mediaUri = null,
                    mediaType = MediaType.IMAGE,
                    textScript = "Quer triplicar seu foco hoje? Comece aplicando a regra dos 2 minutos!",
                    voiceId = "cartesia-pt-sofia",
                    voiceName = "Sofia (Apresentadora)",
                    durationSec = 4.0f,
                    transition = TransitionType.FADE
                )
            ),
            createdAt = System.currentTimeMillis() - 86400000L * 5,
            updatedAt = System.currentTimeMillis() - 86400000L * 4
        )

        return listOf(sample1, sample2)
    }
}

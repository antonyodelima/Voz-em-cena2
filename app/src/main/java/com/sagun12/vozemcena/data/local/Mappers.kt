package com.sagun12.vozemcena.data.local

import com.sagun12.vozemcena.domain.model.AspectRatio
import com.sagun12.vozemcena.domain.model.MediaType
import com.sagun12.vozemcena.domain.model.Project
import com.sagun12.vozemcena.domain.model.ProjectStatus
import com.sagun12.vozemcena.domain.model.Scene
import com.sagun12.vozemcena.domain.model.TransitionType
import com.sagun12.vozemcena.domain.model.Voice
import com.sagun12.vozemcena.domain.model.VoiceProvider

fun ProjectEntity.toDomain(scenes: List<SceneEntity> = emptyList()): Project {
    return Project(
        id = id,
        title = title,
        description = description,
        aspectRatio = AspectRatio.fromString(aspectRatio),
        status = runCatching { ProjectStatus.valueOf(status) }.getOrDefault(ProjectStatus.DRAFT),
        scenes = scenes.sortedBy { it.sequenceOrder }.map { it.toDomain() },
        outputVideoUri = outputVideoUri,
        backgroundMusicUri = backgroundMusicUri,
        backgroundMusicVolume = backgroundMusicVolume,
        includeSubtitles = includeSubtitles,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Project.toEntity(): ProjectEntity {
    return ProjectEntity(
        id = id,
        title = title,
        description = description,
        aspectRatio = aspectRatio.name,
        status = status.name,
        outputVideoUri = outputVideoUri,
        backgroundMusicUri = backgroundMusicUri,
        backgroundMusicVolume = backgroundMusicVolume,
        includeSubtitles = includeSubtitles,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun SceneEntity.toDomain(): Scene {
    return Scene(
        id = id,
        projectId = projectId,
        sequenceOrder = sequenceOrder,
        mediaUri = mediaUri,
        mediaType = runCatching { MediaType.valueOf(mediaType) }.getOrDefault(MediaType.IMAGE),
        textScript = textScript,
        voiceId = voiceId,
        voiceName = voiceName,
        durationSec = durationSec,
        transition = runCatching { TransitionType.valueOf(transition) }.getOrDefault(TransitionType.FADE),
        volume = volume,
        generatedAudioPath = generatedAudioPath
    )
}

fun Scene.toEntity(): SceneEntity {
    return SceneEntity(
        id = id,
        projectId = projectId,
        sequenceOrder = sequenceOrder,
        mediaUri = mediaUri,
        mediaType = mediaType.name,
        textScript = textScript,
        voiceId = voiceId,
        voiceName = voiceName,
        durationSec = durationSec,
        transition = transition.name,
        volume = volume,
        generatedAudioPath = generatedAudioPath
    )
}

fun VoiceEntity.toDomain(): Voice {
    return Voice(
        id = id,
        name = name,
        description = description,
        language = language,
        gender = gender,
        provider = runCatching { VoiceProvider.valueOf(provider) }.getOrDefault(VoiceProvider.CARTESIA),
        previewAudioUrl = previewAudioUrl,
        tags = if (tagsCsv.isBlank()) emptyList() else tagsCsv.split(",").map { it.trim() },
        isCustom = isCustom,
        localAudioPath = localAudioPath,
        createdAt = createdAt
    )
}

fun Voice.toEntity(): VoiceEntity {
    return VoiceEntity(
        id = id,
        name = name,
        description = description,
        language = language,
        gender = gender,
        provider = provider.name,
        previewAudioUrl = previewAudioUrl,
        tagsCsv = tags.joinToString(","),
        isCustom = isCustom,
        localAudioPath = localAudioPath,
        createdAt = createdAt
    )
}

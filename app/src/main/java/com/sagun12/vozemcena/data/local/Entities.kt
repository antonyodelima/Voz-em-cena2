package com.sagun12.vozemcena.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val aspectRatio: String,
    val status: String,
    val outputVideoUri: String?,
    val backgroundMusicUri: String?,
    val backgroundMusicVolume: Float,
    val includeSubtitles: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "scenes",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["projectId"])]
)
data class SceneEntity(
    @PrimaryKey
    val id: String,
    val projectId: String,
    val sequenceOrder: Int,
    val mediaUri: String?,
    val mediaType: String,
    val textScript: String,
    val voiceId: String?,
    val voiceName: String?,
    val durationSec: Float,
    val transition: String,
    val volume: Float,
    val generatedAudioPath: String?
)

@Entity(tableName = "voices")
data class VoiceEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val language: String,
    val gender: String,
    val provider: String,
    val previewAudioUrl: String?,
    val tagsCsv: String,
    val isCustom: Boolean,
    val localAudioPath: String?,
    val createdAt: Long
)

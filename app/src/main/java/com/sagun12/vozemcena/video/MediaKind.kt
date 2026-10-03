package com.sagun12.vozemcena.video

enum class MediaKind {
    IMAGE,
    VIDEO,
    AUDIO,
    UNKNOWN;

    companion object {
        fun fromMimeType(mimeType: String?): MediaKind {
            return when {
                mimeType == null -> UNKNOWN
                mimeType.startsWith("image/") -> IMAGE
                mimeType.startsWith("video/") -> VIDEO
                mimeType.startsWith("audio/") -> AUDIO
                else -> UNKNOWN
            }
        }

        fun fromFilename(filename: String): MediaKind {
            val lower = filename.lowercase()
            return when {
                lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp") -> IMAGE
                lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".mov") || lower.endsWith(".webm") -> VIDEO
                lower.endsWith(".wav") || lower.endsWith(".mp3") || lower.endsWith(".m4a") || lower.endsWith(".aac") -> AUDIO
                else -> UNKNOWN
            }
        }
    }
}

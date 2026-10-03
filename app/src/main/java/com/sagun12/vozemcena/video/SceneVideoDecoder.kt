package com.sagun12.vozemcena.video

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri

class SceneVideoDecoder(private val context: Context) {

    fun extractFrameAt(videoUriStr: String, timeUs: Long = 0L): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            val uri = Uri.parse(videoUriStr)
            retriever.setDataSource(context, uri)
            retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (e: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    fun getVideoDurationMs(videoUriStr: String): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            val uri = Uri.parse(videoUriStr)
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationStr?.toLongOrNull() ?: 3000L
        } catch (e: Exception) {
            3000L
        } finally {
            runCatching { retriever.release() }
        }
    }
}

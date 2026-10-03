package com.sagun12.vozemcena.ui.create

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import java.io.File
import java.io.FileOutputStream

object MediaImportHelper {

    fun copyUriToAppStorage(context: Context, uri: Uri, prefix: String = "scene_media"): String? {
        return try {
            val extension = when (context.contentResolver.getType(uri)) {
                "image/png" -> ".png"
                "image/jpeg", "image/jpg" -> ".jpg"
                "image/webp" -> ".webp"
                "video/mp4" -> ".mp4"
                else -> ".jpg"
            }
            val destinationDir = File(context.filesDir, "media").apply { mkdirs() }
            val destFile = File(destinationDir, "${prefix}_${System.currentTimeMillis()}$extension")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }
}

@Composable
fun rememberMediaPickerLauncher(
    onMediaSelected: (Uri) -> Unit
) = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
) { uri ->
    uri?.let { onMediaSelected(it) }
}

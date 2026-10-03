package com.sagun12.vozemcena.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import com.sagun12.vozemcena.domain.model.AspectRatio
import com.sagun12.vozemcena.domain.model.Scene
import java.io.File
import kotlin.math.max

class SceneFrameRenderer(private val context: Context) {

    fun renderSceneBitmap(
        scene: Scene,
        sceneIndex: Int,
        totalScenes: Int,
        aspectRatio: AspectRatio,
        targetWidth: Int = 1280,
        targetHeight: Int = 720,
        includeSubtitles: Boolean = true
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Draw Background Image or Dynamic Studio Canvas
        val userBitmap = loadUserBitmap(scene.mediaUri, targetWidth, targetHeight)
        if (userBitmap != null) {
            drawScaledImage(canvas, userBitmap, targetWidth, targetHeight)
        } else {
            drawCinematicBackground(canvas, sceneIndex, targetWidth, targetHeight)
        }

        // 2. Draw Subtle Vignette / Bottom Gradient for Subtitle Readability
        drawBottomGradientOverlay(canvas, targetWidth, targetHeight)

        // 3. Draw Subtitles / Narration Text
        if (includeSubtitles && scene.textScript.isNotBlank()) {
            drawSubtitles(canvas, scene.textScript, targetWidth, targetHeight)
        }

        // 4. Draw Minimal Scene Indicator Badge (Top Right)
        drawSceneBadge(canvas, sceneIndex + 1, totalScenes, targetWidth, targetHeight)

        return bitmap
    }

    private fun loadUserBitmap(mediaUriStr: String?, targetWidth: Int, targetHeight: Int): Bitmap? {
        if (mediaUriStr.isNullOrBlank()) return null
        return try {
            val uri = Uri.parse(mediaUriStr)
            if (uri.scheme == "file" || !uri.scheme.isNullOrEmpty()) {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } else {
                val file = File(mediaUriStr)
                if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun drawScaledImage(canvas: Canvas, srcBitmap: Bitmap, targetW: Int, targetH: Int) {
        val srcW = srcBitmap.width.toFloat()
        val srcH = srcBitmap.height.toFloat()

        // Center Crop scaling
        val scale = max(targetW.toFloat() / srcW, targetH.toFloat() / srcH)
        val scaledW = srcW * scale
        val scaledH = srcH * scale

        val dx = (targetW - scaledW) / 2f
        val dy = (targetH - scaledH) / 2f

        val destRect = RectF(dx, dy, dx + scaledW, dy + scaledH)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(srcBitmap, null, destRect, paint)
    }

    private fun drawCinematicBackground(canvas: Canvas, index: Int, width: Int, height: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Gradients tailored for theater/cinematic feel
        val colorPalettes = listOf(
            Pair(Color.rgb(18, 20, 32), Color.rgb(45, 27, 78)), // Obsidian to Deep Violet
            Pair(Color.rgb(15, 28, 38), Color.rgb(22, 60, 80)), // Deep Teal
            Pair(Color.rgb(30, 20, 18), Color.rgb(74, 38, 24)), // Amber Bronze
            Pair(Color.rgb(20, 24, 32), Color.rgb(38, 48, 70))  // Slate Navy
        )
        val palette = colorPalettes[index % colorPalettes.size]

        val gradient = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            palette.first, palette.second,
            Shader.TileMode.CLAMP
        )
        paint.shader = gradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // Draw Studio Watermark & Ambient Accent Rings
        paint.shader = null
        paint.color = Color.argb(30, 255, 255, 255)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawCircle(width * 0.5f, height * 0.5f, minOf(width, height) * 0.35f, paint)
        canvas.drawCircle(width * 0.5f, height * 0.5f, minOf(width, height) * 0.22f, paint)

        // Center Icon / Motif text
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(80, 255, 255, 255)
            textSize = (minOf(width, height) * 0.08f)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("VOZ EM CENA", width * 0.5f, height * 0.52f, textPaint)
    }

    private fun drawBottomGradientOverlay(canvas: Canvas, width: Int, height: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val startY = height * 0.55f
        val gradient = LinearGradient(
            0f, startY, 0f, height.toFloat(),
            Color.TRANSPARENT, Color.argb(220, 0, 0, 0),
            Shader.TileMode.CLAMP
        )
        paint.shader = gradient
        canvas.drawRect(0f, startY, width.toFloat(), height.toFloat(), paint)
    }

    private fun drawSubtitles(canvas: Canvas, script: String, width: Int, height: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = (minOf(width, height) * 0.045f).coerceIn(24f, 48f)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            setShadowLayer(8f, 0f, 4f, Color.BLACK)
            textAlign = Paint.Align.CENTER
        }

        val maxTextWidth = width * 0.85f
        val lines = wrapText(script, paint, maxTextWidth)

        val lineHeight = paint.textSize * 1.35f
        val totalTextHeight = lines.size * lineHeight
        val startY = height - (height * 0.08f) - (totalTextHeight - lineHeight)

        lines.forEachIndexed { i, line ->
            canvas.drawText(line, width / 2f, startY + (i * lineHeight), paint)
        }
    }

    private fun drawSceneBadge(canvas: Canvas, current: Int, total: Int, width: Int, height: Int) {
        val badgeText = "Cena $current / $total"
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = (minOf(width, height) * 0.032f).coerceIn(18f, 32f)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        val textWidth = paint.measureText(badgeText)
        val bounds = Rect()
        paint.getTextBounds(badgeText, 0, badgeText.length, bounds)

        val paddingH = 20f
        val paddingV = 12f
        val margin = 28f

        val right = width - margin
        val top = margin
        val left = right - textWidth - (paddingH * 2)
        val bottom = top + bounds.height() + (paddingV * 2)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(160, 0, 0, 0)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(left, top, right, bottom), 16f, 16f, bgPaint)

        paint.color = Color.rgb(255, 215, 0) // Amber gold
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText(badgeText, left + paddingH, top + paddingV + bounds.height(), paint)
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine.append(if (currentLine.isEmpty()) word else " $word")
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                }
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return if (lines.isEmpty()) listOf(text) else lines
    }
}

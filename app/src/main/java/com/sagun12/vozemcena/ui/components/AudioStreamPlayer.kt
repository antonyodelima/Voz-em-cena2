package com.sagun12.vozemcena.ui.components

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sagun12.vozemcena.ui.theme.CyanSecondary
import com.sagun12.vozemcena.ui.theme.GoldPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/**
 * Reusable Composable component that uses Android MediaPlayer to playback
 * audio streams or audio files received from the Cartesia Retrofit service.
 * Includes play/pause controls, scrubbing, visual progress indicator, and waveform animation.
 */
@Composable
fun AudioStreamPlayer(
    audioFile: File?,
    modifier: Modifier = Modifier,
    audioBytes: ByteArray? = null,
    title: String? = null,
    subtitle: String? = null,
    autoPlay: Boolean = false,
    onPlaybackEnded: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var isPrepared by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableIntStateOf(0) }
    var durationMs by remember { mutableIntStateOf(1) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }

    // Resolve effective playback file
    val effectiveFile = remember(audioFile, audioBytes) {
        when {
            audioFile != null && audioFile.exists() -> audioFile
            audioBytes != null && audioBytes.isNotEmpty() -> {
                val temp = File(context.cacheDir, "stream_playback_${System.currentTimeMillis()}.wav")
                FileOutputStream(temp).use { it.write(audioBytes) }
                temp
            }
            else -> null
        }
    }

    fun releasePlayer() {
        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.reset()
            mediaPlayer?.release()
        }
        mediaPlayer = null
        isPlaying = false
        isPrepared = false
        currentPositionMs = 0
    }

    fun prepareAndPlay(file: File, startImmediately: Boolean) {
        releasePlayer()
        isLoading = true

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(file.absolutePath)
                setOnPreparedListener { mp ->
                    isPrepared = true
                    isLoading = false
                    durationMs = mp.duration.coerceAtLeast(1)
                    if (startImmediately) {
                        mp.start()
                        isPlaying = true
                    }
                }
                setOnCompletionListener {
                    isPlaying = false
                    currentPositionMs = durationMs
                    onPlaybackEnded?.invoke()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("AudioStreamPlayer", "MediaPlayer error: what=$what, extra=$extra")
                    releasePlayer()
                    isLoading = false
                    true
                }
                prepareAsync()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            Log.e("AudioStreamPlayer", "Failed to initialize MediaPlayer", e)
            isLoading = false
        }
    }

    LaunchedEffect(effectiveFile) {
        if (effectiveFile != null) {
            prepareAndPlay(effectiveFile, startImmediately = autoPlay)
        } else {
            releasePlayer()
        }
    }

    // Polling current playback position while playing
    LaunchedEffect(isPlaying, isPrepared) {
        while (isActive && isPlaying && isPrepared) {
            val pos = runCatching { mediaPlayer?.currentPosition ?: 0 }.getOrDefault(0)
            if (!isDraggingSlider) {
                currentPositionMs = pos
            }
            delay(100)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            releasePlayer()
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("audio_stream_player")
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Header: Title & Subtitle or Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title ?: "Áudio Sintetizado (Cartesia AI)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    subtitle?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Waveform indicator
                AudioWaveformVisualizer(
                    isPlaying = isPlaying,
                    color = if (isPlaying) CyanSecondary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    barCount = 12
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Visual Progress Slider
            val progressFraction = if (durationMs > 0) {
                if (isDraggingSlider) sliderPosition else (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            } else 0f

            Slider(
                value = progressFraction,
                onValueChange = { newFraction ->
                    isDraggingSlider = true
                    sliderPosition = newFraction
                },
                onValueChangeFinished = {
                    isDraggingSlider = false
                    val targetMs = (sliderPosition * durationMs).toInt()
                    runCatching {
                        mediaPlayer?.seekTo(targetMs)
                        currentPositionMs = targetMs
                    }
                },
                enabled = isPrepared && !isLoading,
                colors = SliderDefaults.colors(
                    thumbColor = GoldPrimary,
                    activeTrackColor = GoldPrimary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("audio_player_slider")
            )

            // Time labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val displayCurrent = if (isDraggingSlider) (sliderPosition * durationMs).toInt() else currentPositionMs
                Text(
                    text = formatMs(displayCurrent),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatMs(durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Playback Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rewind 10s
                IconButton(
                    onClick = {
                        val newPos = (currentPositionMs - 10_000).coerceAtLeast(0)
                        runCatching {
                            mediaPlayer?.seekTo(newPos)
                            currentPositionMs = newPos
                        }
                    },
                    enabled = isPrepared && !isLoading,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("audio_player_rewind_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = "Voltar 10 segundos",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Primary Play / Pause Button
                FilledIconButton(
                    onClick = {
                        if (isLoading) return@FilledIconButton
                        if (isPlaying) {
                            runCatching {
                                mediaPlayer?.pause()
                                isPlaying = false
                            }
                        } else {
                            if (effectiveFile != null) {
                                if (mediaPlayer == null || !isPrepared) {
                                    prepareAndPlay(effectiveFile, startImmediately = true)
                                } else {
                                    // If at end, replay from beginning
                                    if (currentPositionMs >= durationMs - 200) {
                                        mediaPlayer?.seekTo(0)
                                        currentPositionMs = 0
                                    }
                                    mediaPlayer?.start()
                                    isPlaying = true
                                }
                            }
                        }
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = GoldPrimary,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("audio_player_play_pause_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(26.dp),
                            color = Color.Black,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pausar" else "Reproduzir",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Forward 10s
                IconButton(
                    onClick = {
                        val newPos = (currentPositionMs + 10_000).coerceAtMost(durationMs)
                        runCatching {
                            mediaPlayer?.seekTo(newPos)
                            currentPositionMs = newPos
                        }
                    },
                    enabled = isPrepared && !isLoading,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("audio_player_forward_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Forward10,
                        contentDescription = "Avançar 10 segundos",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private fun formatMs(millis: Int): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

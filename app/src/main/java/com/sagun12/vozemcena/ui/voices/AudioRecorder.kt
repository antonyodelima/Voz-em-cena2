package com.sagun12.vozemcena.ui.voices

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var amplitudeJob: Job? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _recordDurationSec = MutableStateFlow(0f)
    val recordDurationSec: StateFlow<Float> = _recordDurationSec.asStateFlow()

    fun startRecording(outputFile: File, scope: CoroutineScope) {
        stopRecording()
        currentOutputFile = outputFile
        outputFile.parentFile?.mkdirs()

        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128_000)
            setAudioSamplingRate(44100)
            setOutputFile(outputFile.absolutePath)
            try {
                prepare()
                start()
                _isRecording.value = true
                _recordDurationSec.value = 0f

                amplitudeJob = scope.launch(Dispatchers.IO) {
                    var elapsed = 0L
                    while (isActive && _isRecording.value) {
                        val maxAmp = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)
                        _amplitude.value = (maxAmp / 32767f).coerceIn(0f, 1f)
                        delay(100)
                        elapsed += 100
                        _recordDurationSec.value = elapsed / 1000f
                    }
                }
            } catch (e: Exception) {
                Log.e("AudioRecorder", "Falha ao iniciar gravação", e)
                _isRecording.value = false
            }
        }
    }

    fun stopRecording(): File? {
        amplitudeJob?.cancel()
        amplitudeJob = null
        _isRecording.value = false
        _amplitude.value = 0f

        return try {
            recorder?.apply {
                stop()
                reset()
                release()
            }
            recorder = null
            currentOutputFile
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Erro ao parar gravação", e)
            recorder = null
            null
        }
    }
}

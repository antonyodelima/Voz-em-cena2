package com.sagun12.vozemcena.video

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin

object WavAudio {

    fun createSilenceWav(
        outputFile: File,
        durationSeconds: Float,
        sampleRate: Int = 44100,
        channels: Short = 1,
        bitsPerSample: Short = 16
    ) {
        val numSamples = (durationSeconds * sampleRate).toInt()
        val byteRate = sampleRate * channels * (bitsPerSample / 8)
        val dataSize = numSamples * channels * (bitsPerSample / 8)

        outputFile.parentFile?.mkdirs()
        FileOutputStream(outputFile).use { fos ->
            writeWavHeader(fos, dataSize, sampleRate, channels, bitsPerSample)
            val silenceChunk = ByteArray(4096)
            var bytesWritten = 0
            while (bytesWritten < dataSize) {
                val toWrite = minOf(silenceChunk.size, dataSize - bytesWritten)
                fos.write(silenceChunk, 0, toWrite)
                bytesWritten += toWrite
            }
        }
    }

    fun createSyntheticSpeechTone(
        outputFile: File,
        durationSeconds: Float,
        sampleRate: Int = 44100
    ) {
        val numSamples = (durationSeconds * sampleRate).toInt()
        val dataSize = numSamples * 2 // 16-bit mono

        outputFile.parentFile?.mkdirs()
        FileOutputStream(outputFile).use { fos ->
            writeWavHeader(fos, dataSize, sampleRate, 1, 16)
            val buffer = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                // Harmonic warm chime
                val freq = 440.0 + 40.0 * sin(2.0 * Math.PI * 3.0 * t)
                val envelope = minOf(1.0, (durationSeconds - t).coerceAtLeast(0.0) * 2.0)
                val sample = (sin(2.0 * Math.PI * freq * t) * 8000 * envelope).toInt().toShort()
                buffer.clear()
                buffer.putShort(sample)
                fos.write(buffer.array())
            }
        }
    }

    fun writeWavHeader(
        out: java.io.OutputStream,
        totalAudioLen: Int,
        sampleRate: Int = 44100,
        channels: Short = 1,
        bitsPerSample: Short = 16
    ) {
        val totalDataLen = totalAudioLen + 36
        val byteRate = sampleRate * channels * (bitsPerSample / 8)
        val blockAlign = (channels * (bitsPerSample / 8)).toShort()

        val header = ByteArray(44)
        val bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        bb.put("RIFF".toByteArray())
        bb.putInt(totalDataLen)
        bb.put("WAVE".toByteArray())
        bb.put("fmt ".toByteArray())
        bb.putInt(16) // Subchunk1Size for PCM
        bb.putShort(1) // AudioFormat (1 = PCM)
        bb.putShort(channels)
        bb.putInt(sampleRate)
        bb.putInt(byteRate)
        bb.putShort(blockAlign)
        bb.putShort(bitsPerSample)
        bb.put("data".toByteArray())
        bb.putInt(totalAudioLen)

        out.write(header)
    }

    fun concatenateWavFiles(wavFiles: List<File>, outputFile: File, targetSampleRate: Int = 44100) {
        outputFile.parentFile?.mkdirs()
        var totalAudioBytes = 0

        val pcmStreams = mutableListOf<ByteArray>()

        for (file in wavFiles) {
            if (!file.exists() || file.length() < 44) {
                continue
            }
            FileInputStream(file).use { fis ->
                // Skip 44 byte header
                fis.skip(44)
                val pcm = fis.readBytes()
                pcmStreams.add(pcm)
                totalAudioBytes += pcm.size
            }
        }

        FileOutputStream(outputFile).use { fos ->
            writeWavHeader(fos, totalAudioBytes, targetSampleRate, 1, 16)
            for (pcm in pcmStreams) {
                fos.write(pcm)
            }
        }
    }

    fun getWavDurationSeconds(wavFile: File): Float {
        if (!wavFile.exists() || wavFile.length() <= 44) return 0f
        return try {
            val audioBytes = wavFile.length() - 44
            val sampleRate = 44100f
            val bytesPerSample = 2f // 16 bit mono
            (audioBytes / (sampleRate * bytesPerSample)).coerceAtLeast(0.1f)
        } catch (e: Exception) {
            1.0f
        }
    }
}

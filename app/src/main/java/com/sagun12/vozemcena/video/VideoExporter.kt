package com.sagun12.vozemcena.video

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import com.sagun12.vozemcena.data.repository.VoiceRepository
import com.sagun12.vozemcena.domain.model.ExportProgress
import com.sagun12.vozemcena.domain.model.Project
import com.sagun12.vozemcena.domain.model.ProjectStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.util.UUID

class VideoExporter(
    private val context: Context,
    private val voiceRepository: VoiceRepository,
    private val frameRenderer: SceneFrameRenderer = SceneFrameRenderer(context),
    private val mediaStoreExporter: MediaStoreExporter = MediaStoreExporter(context)
) {
    private val tag = "VideoExporter"

    fun exportProject(project: Project): Flow<ExportProgress> = flow {
        emit(ExportProgress(ProjectStatus.EXPORTING, 0.05f, "Preparando cenas e áudios..."))

        val exportDir = File(context.filesDir, "exports").apply { mkdirs() }
        val outputVideoFile = File(exportDir, "export_${project.id}_${System.currentTimeMillis()}.mp4")

        try {
            // 1. Synthesize audio for each scene if needed
            val sceneWavFiles = mutableListOf<File>()
            val totalScenes = project.scenes.size

            for (index in project.scenes.indices) {
                val scene = project.scenes[index]
                val stepProgress = 0.05f + (0.35f * (index.toFloat() / totalScenes.coerceAtLeast(1)))
                emit(ExportProgress(
                    ProjectStatus.EXPORTING,
                    stepProgress,
                    "Gerando voz para Cena ${index + 1} de $totalScenes..."
                ))

                val sceneAudioFile = File(exportDir, "scene_${scene.id}.wav")
                if (scene.generatedAudioPath != null && File(scene.generatedAudioPath).exists()) {
                    sceneWavFiles.add(File(scene.generatedAudioPath))
                } else if (scene.textScript.isNotBlank()) {
                    val result = voiceRepository.synthesizeSpeechToFile(
                        text = scene.textScript,
                        voiceId = scene.voiceId,
                        outputFile = sceneAudioFile
                    )
                    if (result.isSuccess) {
                        sceneWavFiles.add(sceneAudioFile)
                    } else {
                        WavAudio.createSilenceWav(sceneAudioFile, scene.durationSec)
                        sceneWavFiles.add(sceneAudioFile)
                    }
                } else {
                    WavAudio.createSilenceWav(sceneAudioFile, scene.durationSec)
                    sceneWavFiles.add(sceneAudioFile)
                }
            }

            // 2. Concatenate audio tracks
            emit(ExportProgress(ProjectStatus.EXPORTING, 0.45f, "Mixando trilha sonora..."))
            val masterAudioFile = File(exportDir, "master_${project.id}.wav")
            WavAudio.concatenateWavFiles(sceneWavFiles, masterAudioFile)

            // 3. Render and encode video
            emit(ExportProgress(ProjectStatus.EXPORTING, 0.50f, "Renderizando quadros de vídeo..."))
            encodeVideoAndAudio(
                project = project,
                audioWavFile = masterAudioFile,
                outputFile = outputVideoFile
            ) { frameProgress ->
                // frameProgress 0.0 -> 1.0 maps to 0.50 -> 0.90
                emit(ExportProgress(
                    ProjectStatus.EXPORTING,
                    0.50f + (0.40f * frameProgress),
                    "Codificando vídeo em alta definição (${(frameProgress * 100).toInt()}%)..."
                ))
            }

            // 4. Register to MediaStore
            emit(ExportProgress(ProjectStatus.EXPORTING, 0.95f, "Salvando na galeria..."))
            val publicUri = mediaStoreExporter.exportVideoToPublicGallery(outputVideoFile, project.title)
            val finalUriString = publicUri?.toString() ?: outputVideoFile.absolutePath

            emit(ExportProgress(
                status = ProjectStatus.COMPLETED,
                progress = 1.0f,
                currentStep = "Exportação concluída com sucesso!",
                outputUri = finalUriString
            ))
        } catch (e: Exception) {
            Log.e(tag, "Erro durante a exportação", e)
            emit(ExportProgress(
                status = ProjectStatus.ERROR,
                progress = 0f,
                currentStep = "Falha ao exportar",
                errorMessage = e.localizedMessage ?: "Erro desconhecido durante exportação."
            ))
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun encodeVideoAndAudio(
        project: Project,
        audioWavFile: File,
        outputFile: File,
        onProgress: suspend (Float) -> Unit
    ) = withContext(Dispatchers.IO) {
        val width = if (project.aspectRatio.width % 16 != 0) 1280 else project.aspectRatio.width
        val height = if (project.aspectRatio.height % 16 != 0) 720 else project.aspectRatio.height
        val frameRate = 30
        val bitRate = 4_000_000 // 4 Mbps
        val iFrameInterval = 1 // 1 sec keyframe

        // Calculate total frames
        val sceneBitmaps = project.scenes.mapIndexed { idx, sc ->
            frameRenderer.renderSceneBitmap(
                scene = sc,
                sceneIndex = idx,
                totalScenes = project.scenes.size,
                aspectRatio = project.aspectRatio,
                targetWidth = width,
                targetHeight = height,
                includeSubtitles = project.includeSubtitles
            )
        }

        val totalDurationSec = project.scenes.sumOf { it.durationSec.toDouble() }.toFloat().coerceAtLeast(1.0f)
        val totalVideoFrames = (totalDurationSec * frameRate).toInt().coerceAtLeast(frameRate)

        // Setup MediaCodec Video Encoder
        val videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval)
        }

        val videoEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        videoEncoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        videoEncoder.start()

        // Setup MediaCodec Audio Encoder (AAC)
        val sampleRate = 44100
        val audioFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, 1).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, 128_000)
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
        }

        val audioEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        audioEncoder.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        audioEncoder.start()

        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        var videoTrackIndex = -1
        var audioTrackIndex = -1
        var muxerStarted = false

        val videoBufferInfo = MediaCodec.BufferInfo()
        val audioBufferInfo = MediaCodec.BufferInfo()

        var currentFrame = 0
        var sceneAccFrames = 0

        // Pre-convert bitmaps to NV12
        val sceneYuvList = sceneBitmaps.map { YuvConverter.fromBitmapToNV12(it, width, height) }

        // Audio stream setup
        val audioFis = if (audioWavFile.exists() && audioWavFile.length() > 44) {
            FileInputStream(audioWavFile).apply { skip(44) } // skip WAV header
        } else null

        val pcmAudioBuffer = ByteArray(4096)
        var audioEosReached = false
        var audioSampleIndex = 0L

        var videoInputEos = false
        var videoOutputEos = false
        var audioOutputEos = false

        while (!videoOutputEos || !audioOutputEos) {
            // Feed Video Frames
            if (!videoInputEos) {
                val inputIndex = videoEncoder.dequeueInputBuffer(10_000L)
                if (inputIndex >= 0) {
                    if (currentFrame >= totalVideoFrames) {
                        videoEncoder.queueInputBuffer(inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        videoInputEos = true
                    } else {
                        val inputBuf = videoEncoder.getInputBuffer(inputIndex)
                        if (inputBuf != null) {
                            inputBuf.clear()

                            // Find which scene we are on
                            var sceneIdx = 0
                            var accumulated = 0
                            for (s in project.scenes.indices) {
                                val sFrames = (project.scenes[s].durationSec * frameRate).toInt()
                                if (currentFrame < accumulated + sFrames) {
                                    sceneIdx = s
                                    break
                                }
                                accumulated += sFrames
                            }
                            val yuv = sceneYuvList[sceneIdx.coerceIn(0, sceneYuvList.lastIndex)]
                            inputBuf.put(yuv)

                            val ptsUs = (currentFrame * 1_000_000L) / frameRate
                            videoEncoder.queueInputBuffer(inputIndex, 0, yuv.size, ptsUs, 0)
                            currentFrame++

                            if (currentFrame % 15 == 0) {
                                onProgress(currentFrame.toFloat() / totalVideoFrames.toFloat())
                            }
                        }
                    }
                }
            }

            // Feed Audio Samples
            if (!audioEosReached) {
                val aInIndex = audioEncoder.dequeueInputBuffer(10_000L)
                if (aInIndex >= 0) {
                    val readBytes = audioFis?.read(pcmAudioBuffer) ?: -1
                    if (readBytes <= 0) {
                        audioEncoder.queueInputBuffer(aInIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        audioEosReached = true
                        audioFis?.close()
                    } else {
                        val aInBuf = audioEncoder.getInputBuffer(aInIndex)
                        if (aInBuf != null) {
                            aInBuf.clear()
                            aInBuf.put(pcmAudioBuffer, 0, readBytes)
                            val ptsUs = (audioSampleIndex * 1_000_000L) / (sampleRate * 2)
                            audioEncoder.queueInputBuffer(aInIndex, 0, readBytes, ptsUs, 0)
                            audioSampleIndex += readBytes
                        }
                    }
                }
            }

            // Drain Video Encoder
            var vOutIndex = videoEncoder.dequeueOutputBuffer(videoBufferInfo, 10_000L)
            while (vOutIndex >= 0) {
                val encodedBuf = videoEncoder.getOutputBuffer(vOutIndex)
                if ((videoBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    videoBufferInfo.size = 0
                }

                if (videoBufferInfo.size > 0 && muxerStarted && encodedBuf != null) {
                    encodedBuf.position(videoBufferInfo.offset)
                    encodedBuf.limit(videoBufferInfo.offset + videoBufferInfo.size)
                    muxer.writeSampleData(videoTrackIndex, encodedBuf, videoBufferInfo)
                }

                videoEncoder.releaseOutputBuffer(vOutIndex, false)
                if ((videoBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    videoOutputEos = true
                    break
                }
                vOutIndex = videoEncoder.dequeueOutputBuffer(videoBufferInfo, 0L)
            }

            if (vOutIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED && videoTrackIndex == -1) {
                videoTrackIndex = muxer.addTrack(videoEncoder.outputFormat)
                if (audioTrackIndex != -1 && !muxerStarted) {
                    muxer.start()
                    muxerStarted = true
                }
            }

            // Drain Audio Encoder
            var aOutIndex = audioEncoder.dequeueOutputBuffer(audioBufferInfo, 10_000L)
            while (aOutIndex >= 0) {
                val aEncodedBuf = audioEncoder.getOutputBuffer(aOutIndex)
                if ((audioBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    audioBufferInfo.size = 0
                }

                if (audioBufferInfo.size > 0 && muxerStarted && aEncodedBuf != null) {
                    aEncodedBuf.position(audioBufferInfo.offset)
                    aEncodedBuf.limit(audioBufferInfo.offset + audioBufferInfo.size)
                    muxer.writeSampleData(audioTrackIndex, aEncodedBuf, audioBufferInfo)
                }

                audioEncoder.releaseOutputBuffer(aOutIndex, false)
                if ((audioBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    audioOutputEos = true
                    break
                }
                aOutIndex = audioEncoder.dequeueOutputBuffer(audioBufferInfo, 0L)
            }

            if (aOutIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED && audioTrackIndex == -1) {
                audioTrackIndex = muxer.addTrack(audioEncoder.outputFormat)
                if (videoTrackIndex != -1 && !muxerStarted) {
                    muxer.start()
                    muxerStarted = true
                }
            }
        }

        // Clean up codecs and muxer
        runCatching { videoEncoder.stop() }
        runCatching { videoEncoder.release() }
        runCatching { audioEncoder.stop() }
        runCatching { audioEncoder.release() }
        if (muxerStarted) {
            runCatching { muxer.stop() }
        }
        runCatching { muxer.release() }
        runCatching { audioFis?.close() }
    }
}

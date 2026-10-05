package com.shotposeai.analysis

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class VideoPoseAnalyzer(private val context: Context) {
    suspend fun analyze(
        uri: Uri,
        movement: String,
        onProgress: suspend (Int) -> Unit,
        onFrameAnalyzed: suspend (Bitmap, PoseLandmarkerResult, Int, Int) -> Unit = { _, _, _, _ -> },
        onPoseSample: suspend (Long, PoseLandmarkerResult?, Int, Int) -> Unit = { _, _, _, _ -> }
    ): MovementFeedback = withContext(Dispatchers.Default) {
        if (!PoseLandmarkerHelper.isModelAvailable(context)) return@withContext failure("Model belum tersedia", PoseLandmarkerHelper.MODEL_MISSING_MESSAGE)
        val retriever = MediaMetadataRetriever()
        var landmarker: PoseLandmarker? = null
        try {
            retriever.setDataSource(context, uri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            if (durationMs < 500L) return@withContext failure("Video terlalu singkat", "Gunakan video berdurasi minimal satu detik dengan seluruh tubuh terlihat.")
            val safeDuration = min(durationMs, 30_000L)
            // Replay samples at 8 Hz; scoring retains the original 250 ms cadence.
            val stepMs = 125L
            val totalSteps = max(1, ceil(safeDuration.toDouble() / stepMs).toInt() + 1)
            landmarker = PoseLandmarker.createFromOptions(context, PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(BaseOptions.builder().setDelegate(Delegate.CPU).setModelAssetPath(PoseLandmarkerHelper.MODEL_ASSET).build())
                .setRunningMode(RunningMode.VIDEO).setNumPoses(2)
                .setMinPoseDetectionConfidence(0.55f).setMinPosePresenceConfidence(0.55f).setMinTrackingConfidence(0.5f).build())

            val feedbacks = mutableListOf<MovementFeedback>()
            var decodedFrames = 0
            var previousPose: List<NormalizedLandmark>? = null
            var accumulatedMotion = 0f
            var motionPairs = 0
            var step = 0
            var timestamp = 0L
            while (timestamp <= safeDuration) {
                coroutineContext.ensureActive()
                val frame = retriever.getFrameAtTime(timestamp * 1000L, MediaMetadataRetriever.OPTION_CLOSEST)
                if (frame != null) {
                    val image = BitmapImageBuilder(frame).build()
                    try {
                        val result = landmarker.detectForVideo(image, timestamp)
                        onPoseSample(timestamp, result, frame.width, frame.height)
                        if (timestamp % 500L == 0L) {
                            onFrameAnalyzed(
                                frame.copy(Bitmap.Config.ARGB_8888, false),
                                result,
                                frame.width,
                                frame.height
                            )
                        }
                        if (timestamp % 250L == 0L) {
                            decodedFrames++
                            val feedback = MovementClassifier.classify(result, movement)
                            if (feedback.detected) feedbacks += feedback
                            val pose = result.landmarks().singleOrNull()
                            if (pose != null && previousPose != null) {
                                accumulatedMotion += poseMotion(previousPose!!, pose)
                                motionPairs++
                            }
                            previousPose = pose
                        }
                    } finally {
                        image.close(); frame.recycle()
                    }
                } else {
                    onPoseSample(timestamp, null, 0, 0)
                }
                step++
                onProgress((step * 100 / totalSteps).coerceIn(0, 100))
                timestamp += stepMs
            }
            onProgress(100)
            if (decodedFrames == 0) return@withContext failure("Video tidak dapat dibaca", "Format video tidak dapat didekode di perangkat ini. Pilih video MP4/H.264 atau rekam ulang dari kamera.")
            if (feedbacks.size < 3) return@withContext failure("Pose video belum terbaca", "Hanya ${feedbacks.size} frame berpose yang terbaca. Pastikan kepala sampai kaki terlihat dan pencahayaan cukup.")

            val sortedScores = feedbacks.map { it.score }.sorted()
            val median = sortedScores[sortedScores.size / 2]
            val percentile75 = sortedScores[((sortedScores.lastIndex) * 0.75).roundToInt()]
            val coverage = feedbacks.size.toFloat() / decodedFrames.coerceAtLeast(1)
            val averageMotion = if (motionPairs == 0) 0f else accumulatedMotion / motionPairs
            var score = (median * 0.55f + percentile75 * 0.45f).roundToInt()
            score = (score * (0.65f + 0.35f * coverage.coerceIn(0f, 1f))).roundToInt()
            if (averageMotion < 0.0045f) score = min(score, 55)
            score = score.coerceIn(0, 95)
            val representative = feedbacks.minByOrNull { abs(it.score - score) } ?: feedbacks.first()
            val movementDetected = averageMotion >= 0.0045f && coverage >= 0.25f
            if (!movementDetected) return@withContext failure("Gerakan belum cukup terlihat", "Pose tubuh terbaca, tetapi perubahan gerakan antar-frame belum cukup untuk dinilai. Rekam seluruh rangkaian gerakan dengan kamera stabil.")
            representative.copy(
                movement = movement,
                score = score,
                feedback = "${representative.feedback} Dinilai dari ${feedbacks.size} frame valid, bukan dari satu frame terbaik.",
                detected = true
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (security: SecurityException) {
            failure("Akses video berakhir", "Pilih ulang video agar aplikasi memperoleh izin membacanya.")
        } catch (e: Exception) {
            failure("Analisis video gagal", e.message ?: "Terjadi kesalahan saat membaca video.")
        } finally {
            landmarker?.close(); runCatching { retriever.release() }
        }
    }

    private fun poseMotion(previous: List<NormalizedLandmark>, current: List<NormalizedLandmark>): Float {
        val indices = intArrayOf(11, 12, 15, 16, 23, 24, 27, 28)
        return indices.map { index ->
            if (index >= previous.size || index >= current.size) 0f
            else abs(previous[index].x() - current[index].x()) + abs(previous[index].y() - current[index].y())
        }.average().toFloat()
    }

    private fun failure(title: String, detail: String) = MovementFeedback(title, 0, detail, false)
}

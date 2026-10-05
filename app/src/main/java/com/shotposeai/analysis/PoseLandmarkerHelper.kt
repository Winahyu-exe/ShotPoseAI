package com.shotposeai.analysis

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

class PoseLandmarkerHelper(
    private val context: Context,
    private val listener: LandmarkerListener
) : AutoCloseable {

    private var poseLandmarker: PoseLandmarker? = null
    private var lastInferenceTime = 0L

    init {
        setupLiveStreamLandmarker()
    }

    fun detectLiveStream(imageProxy: ImageProxy) {
        val landmarker = poseLandmarker
        if (landmarker == null) {
            imageProxy.close()
            return
        }

        try {
            val frameTime = SystemClock.uptimeMillis()
            val bitmap = Bitmap.createBitmap(
                imageProxy.width,
                imageProxy.height,
                Bitmap.Config.ARGB_8888
            )
            imageProxy.planes[0].buffer.rewind()
            bitmap.copyPixelsFromBuffer(imageProxy.planes[0].buffer)
            val rotatedBitmap = bitmap.rotate(imageProxy.imageInfo.rotationDegrees)
            val mpImage = BitmapImageBuilder(rotatedBitmap).build()
            lastInferenceTime = SystemClock.uptimeMillis()
            landmarker.detectAsync(mpImage, frameTime)
        } catch (e: Exception) {
            listener.onError("Gagal memproses frame kamera: ${e.message.orEmpty()}")
        } finally {
            imageProxy.close()
        }
    }

    private fun setupLiveStreamLandmarker() {
        try {
            if (!isModelAvailable(context)) {
                listener.onError(MODEL_MISSING_MESSAGE)
                return
            }

            val baseOptions = BaseOptions.builder()
                .setDelegate(Delegate.CPU)
                .setModelAssetPath(MODEL_ASSET)
                .build()

            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumPoses(2)
                .setMinPoseDetectionConfidence(0.5f)
                .setMinPosePresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setResultListener(::returnLiveStreamResult)
                .setErrorListener { error ->
                    listener.onError(error.message ?: "MediaPipe mengalami kesalahan.")
                }
                .build()

            poseLandmarker = PoseLandmarker.createFromOptions(context, options)
        } catch (e: Exception) {
            listener.onError("Gagal menyiapkan MediaPipe: ${e.message.orEmpty()}")
        }
    }

    private fun returnLiveStreamResult(result: PoseLandmarkerResult, input: MPImage) {
        val inferenceTime = SystemClock.uptimeMillis() - lastInferenceTime
        listener.onResults(
            ResultBundle(
                poseLandmarkerResult = result,
                inputImageWidth = input.width,
                inputImageHeight = input.height,
                inferenceTime = inferenceTime
            )
        )
    }

    override fun close() {
        poseLandmarker?.close()
        poseLandmarker = null
    }

    interface LandmarkerListener {
        fun onError(error: String)
        fun onResults(resultBundle: ResultBundle)
    }

    data class ResultBundle(
        val poseLandmarkerResult: PoseLandmarkerResult,
        val inputImageWidth: Int,
        val inputImageHeight: Int,
        val inferenceTime: Long
    )

    companion object {
        const val MODEL_ASSET = "pose_landmarker_lite.task"
        const val MODEL_MISSING_MESSAGE = "Model pose_landmarker_lite.task belum ada di app/src/main/assets."

        fun isModelAvailable(context: Context): Boolean {
            return context.assets.list("")?.contains(MODEL_ASSET) == true
        }
    }
}

private fun Bitmap.rotate(rotationDegrees: Int): Bitmap {
    if (rotationDegrees == 0) return this
    val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

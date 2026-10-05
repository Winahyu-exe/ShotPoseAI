package com.shotposeai.ui.analysis

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlin.math.max
import kotlin.math.min

class PoseOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var result: PoseLandmarkerResult? = null
    private var imageWidth = 1
    private var imageHeight = 1
    private var isMirrored = false
    private var scaleMode = ScaleMode.FILL_CENTER

    private val density = resources.displayMetrics.density

    private val pointPaint = Paint().apply {
        color = Color.rgb(34, 197, 94)
        strokeWidth = 2.5f * density
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val linePaint = Paint().apply {
        color = Color.rgb(56, 189, 248)
        strokeWidth = 2.5f * density
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }

    private val jointRadius: Float
        get() = 4.2f * density

    fun setResults(
        result: PoseLandmarkerResult,
        imageWidth: Int,
        imageHeight: Int,
        isMirrored: Boolean = false,
        scaleMode: ScaleMode = ScaleMode.FILL_CENTER
    ) {
        this.result = result
        this.imageWidth = imageWidth.coerceAtLeast(1)
        this.imageHeight = imageHeight.coerceAtLeast(1)
        this.isMirrored = isMirrored
        this.scaleMode = scaleMode
        invalidate()
    }

    fun clear() {
        result = null
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (width <= 0 || height <= 0) return

        val landmarks = result
            ?.landmarks()
            ?.firstOrNull()
            ?: return

        if (landmarks.isEmpty()) return

        drawPose(canvas, landmarks)
    }

    private fun drawPose(canvas: Canvas, landmarks: List<NormalizedLandmark>) {
        val transform = calculateTransform()

        POSE_CONNECTIONS.forEach { (start, end) ->
            if (start < landmarks.size && end < landmarks.size) {
                val startPoint = landmarks[start]
                val endPoint = landmarks[end]

                val startX = mapX(startPoint.x(), transform)
                val startY = mapY(startPoint.y(), transform)
                val endX = mapX(endPoint.x(), transform)
                val endY = mapY(endPoint.y(), transform)

                canvas.drawLine(
                    startX,
                    startY,
                    endX,
                    endY,
                    linePaint
                )
            }
        }

        landmarks.forEach { landmark ->
            val x = mapX(landmark.x(), transform)
            val y = mapY(landmark.y(), transform)

            canvas.drawCircle(
                x,
                y,
                jointRadius,
                pointPaint
            )
        }
    }

    private fun calculateTransform(): DrawTransform {
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        val sourceWidth = imageWidth.toFloat()
        val sourceHeight = imageHeight.toFloat()

        val scale = when (scaleMode) {
            ScaleMode.FILL_CENTER -> max(
                viewWidth / sourceWidth,
                viewHeight / sourceHeight
            )
            ScaleMode.FIT_CENTER -> min(
                viewWidth / sourceWidth,
                viewHeight / sourceHeight
            )
        }

        val scaledWidth = sourceWidth * scale
        val scaledHeight = sourceHeight * scale

        val offsetX = (viewWidth - scaledWidth) / 2f
        val offsetY = (viewHeight - scaledHeight) / 2f

        return DrawTransform(
            scale = scale,
            offsetX = offsetX,
            offsetY = offsetY
        )
    }

    private fun mapX(
        normalizedX: Float,
        transform: DrawTransform
    ): Float {
        val mappedX = normalizedX * imageWidth * transform.scale + transform.offsetX
        return if (isMirrored) {
            width - mappedX
        } else {
            mappedX
        }
    }

    private fun mapY(
        normalizedY: Float,
        transform: DrawTransform
    ): Float {
        return normalizedY * imageHeight * transform.scale + transform.offsetY
    }

    private data class DrawTransform(
        val scale: Float,
        val offsetX: Float,
        val offsetY: Float
    )

    enum class ScaleMode {
        FILL_CENTER,
        FIT_CENTER
    }

    companion object {
        private val POSE_CONNECTIONS = listOf(
            11 to 12,
            11 to 13,
            13 to 15,
            12 to 14,
            14 to 16,
            11 to 23,
            12 to 24,
            23 to 24,
            23 to 25,
            25 to 27,
            24 to 26,
            26 to 28,
            27 to 31,
            28 to 32
        )
    }
}

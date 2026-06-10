package com.example.physioapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

class PoseOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var results: PoseLandmarkerResult? = null
    private var imageWidth  = 1
    private var imageHeight = 1

    // Joint dot paint — teal color matching app theme
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color  = Color.parseColor("#2BAE7E")
        style  = Paint.Style.FILL
    }

    // Bone line paint
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color       = Color.parseColor("#80FFFFFF")
        style       = Paint.Style.STROKE
        strokeWidth = 4f
    }

    // Key joint connections — pairs of landmark indices
    // Based on MediaPipe 33 pose landmarks
    private val connections = listOf(
        // Torso
        11 to 12, 11 to 23, 12 to 24, 23 to 24,
        // Left arm
        11 to 13, 13 to 15,
        // Right arm
        12 to 14, 14 to 16,
        // Left leg
        23 to 25, 25 to 27, 27 to 31,
        // Right leg
        24 to 26, 26 to 28, 28 to 32,
        // Face to shoulders
        0 to 11, 0 to 12
    )

    fun setResults(
        poseLandmarkerResult: PoseLandmarkerResult,
        imgWidth: Int,
        imgHeight: Int
    ) {
        results     = poseLandmarkerResult
        imageWidth  = imgWidth
        imageHeight = imgHeight
        invalidate()
    }

    fun clear() {
        results = null
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val result = results ?: return
        if (result.landmarks().isEmpty()) return

        val landmarks = result.landmarks()[0]
        val scaleX = width.toFloat()  / imageWidth
        val scaleY = height.toFloat() / imageHeight

        // Draw connections (bones)
        for ((start, end) in connections) {
            if (start < landmarks.size && end < landmarks.size) {
                val s = landmarks[start]
                val e = landmarks[end]
                canvas.drawLine(
                    s.x() * scaleX,
                    s.y() * scaleY,
                    e.x() * scaleX,
                    e.y() * scaleY,
                    linePaint
                )
            }
        }

        // Draw key joint dots
        val keyJoints = setOf(
            0,            // nose
            11, 12,       // shoulders
            13, 14,       // elbows
            15, 16,       // wrists
            23, 24,       // hips
            25, 26,       // knees
            27, 28        // ankles
        )

        for (i in keyJoints) {
            if (i < landmarks.size) {
                val lm = landmarks[i]
                val cx = lm.x() * scaleX
                val cy = lm.y() * scaleY

                // Outer white ring
                dotPaint.color = Color.WHITE
                canvas.drawCircle(cx, cy, 10f, dotPaint)

                // Inner teal dot
                dotPaint.color = Color.parseColor("#2BAE7E")
                canvas.drawCircle(cx, cy, 6f, dotPaint)
            }
        }
    }
}
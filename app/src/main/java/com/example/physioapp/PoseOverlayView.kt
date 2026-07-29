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
    private var isFrontCamera = true

    // Outer white ring
    private val outerDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color  = Color.WHITE
        style  = Paint.Style.FILL
    }

    // Inner teal dot
    private val innerDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color  = Color.parseColor("#2BAE7E")
        style  = Paint.Style.FILL
    }

    // Bone line
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color       = Color.parseColor("#AAFFFFFF")
        style       = Paint.Style.STROKE
        strokeWidth = 4f
        strokeCap   = Paint.Cap.ROUND
    }

    // Key joint connections
    private val connections = listOf(
        11 to 12,  // shoulders
        11 to 13, 13 to 15,  // left arm
        12 to 14, 14 to 16,  // right arm
        11 to 23, 12 to 24,  // torso sides
        23 to 24,            // hips
        23 to 25, 25 to 27,  // left leg
        24 to 26, 26 to 28   // right leg
    )

    // Only key joints — not all 33
    private val keyJoints = setOf(
        0,          // nose
        11, 12,     // shoulders
        13, 14,     // elbows
        15, 16,     // wrists
        23, 24,     // hips
        25, 26,     // knees
        27, 28      // ankles
    )

    fun setResults(
        result: PoseLandmarkerResult,
        imgWidth: Int,
        imgHeight: Int
    ) {
        results     = result
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

        // Scale landmarks to view size
        // Mirror X for front camera
        fun lx(x: Float): Float {
            val scaled = x * width.toFloat()
            return if (isFrontCamera) width - scaled else scaled
        }
        fun ly(y: Float) = y * height.toFloat()

        // Draw bone connections
        for ((start, end) in connections) {
            if (start < landmarks.size && end < landmarks.size) {
                val s = landmarks[start]
                val e = landmarks[end]
                canvas.drawLine(
                    lx(s.x()), ly(s.y()),
                    lx(e.x()), ly(e.y()),
                    linePaint
                )
            }
        }

        // Draw joint dots
        for (i in keyJoints) {
            if (i < landmarks.size) {
                val lm = landmarks[i]
                val cx = lx(lm.x())
                val cy = ly(lm.y())
                canvas.drawCircle(cx, cy, 10f, outerDotPaint)
                canvas.drawCircle(cx, cy, 6f,  innerDotPaint)
            }
        }
    }
}
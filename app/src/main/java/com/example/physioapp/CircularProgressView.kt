package com.example.physioapp

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator

class CircularProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var progress = 0f
    private var trackColor = Color.parseColor("#E8EEF4")
    private var progressColor = Color.parseColor("#1A2E4A")
    private val strokeWidth = 40f

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        this.strokeWidth = this@CircularProgressView.strokeWidth
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        this.strokeWidth = this@CircularProgressView.strokeWidth
    }

    private val oval = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val padding = strokeWidth / 2f
        oval.set(padding, padding, width - padding, height - padding)

        // Draw background track
        trackPaint.color = trackColor
        canvas.drawArc(oval, 0f, 360f, false, trackPaint)

        // Draw progress clockwise starting from top
        progressPaint.color = progressColor
        val sweepAngle = 360f * (progress / 100f)
        canvas.drawArc(oval, -90f, sweepAngle, false, progressPaint)
    }

    fun setProgress(targetProgress: Float, colorHex: String) {
        progressColor = Color.parseColor(colorHex)
        val animator = ValueAnimator.ofFloat(0f, targetProgress)
        animator.duration = 1200
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener {
            progress = it.animatedValue as Float
            invalidate()
        }
        animator.start()
    }
}
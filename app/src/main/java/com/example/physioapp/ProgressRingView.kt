package com.example.physioapp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class ProgressRingView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 10f
        color = Color.parseColor("#E3E9F4")
    }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 10f
        color = Color.parseColor("#2BAE7E")
        strokeCap = Paint.Cap.ROUND
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0F1E35")
        textSize = 36f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val r = (minOf(width, height) / 2f) - 10f
        val oval = RectF(cx - r, cy - r, cx + r, cy + r)
        canvas.drawCircle(cx, cy, r, trackPaint)
        canvas.drawArc(oval, -90f, 360f * 0.78f, false, progressPaint)
        canvas.drawText("78%", cx, cy + 12f, textPaint)
    }
}
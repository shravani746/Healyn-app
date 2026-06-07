package com.example.physioapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class ExerciseSessionActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var tvFeedback: TextView
    private lateinit var tvFeedbackStrip: TextView
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var tvSetCount: TextView
    private lateinit var tvSetsDone: TextView
    private lateinit var dot1: View
    private lateinit var dot2: View
    private lateinit var dot3: View

    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private var sets = 0
    private val maxSets = 3

    private val feedbackMessages = listOf(
        "Good Posture!",
        "Raise your arm higher",
        "Keep your back straight",
        "Excellent form!",
        "Slow down a little"
    )
    private val stripMessages = listOf(
        "Keep your back straight",
        "Raise arm to shoulder height",
        "Smooth, controlled movement",
        "Excellent form!",
        "Almost there, keep going"
    )
    private var msgIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_session)

        previewView       = findViewById(R.id.previewView)
        tvFeedback        = findViewById(R.id.tvFeedback)
        tvFeedbackStrip   = findViewById(R.id.tvFeedbackStrip)
        btnStart          = findViewById(R.id.btnStart)
        btnStop           = findViewById(R.id.btnStop)
        tvSetCount        = findViewById(R.id.tvSetCount)
        tvSetsDone        = findViewById(R.id.tvSetsDone)
        dot1              = findViewById(R.id.dot1)
        dot2              = findViewById(R.id.dot2)
        dot3              = findViewById(R.id.dot3)

        if (allPermissionsGranted()) startCamera()
        else ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE)

        btnStart.setOnClickListener {
            if (!isRunning) {
                isRunning = true
                startFeedbackLoop()
            }
            sets++
            tvSetCount.text = "$sets / $maxSets"
            tvSetsDone.text = sets.toString()
            updateDots()
            if (sets >= maxSets) {
                btnStart.text = "Go to Feedback →"
                Toast.makeText(this, "All sets done! Great work 💪", Toast.LENGTH_SHORT).show()
                btnStart.setOnClickListener {
                    isRunning = false
                    handler.removeCallbacksAndMessages(null)
                    startActivity(Intent(this, FeedbackActivity::class.java))
                    finish()
                }
            } else {
                Toast.makeText(this, "Set $sets done! Keep going 🔥", Toast.LENGTH_SHORT).show()
            }
        }

        btnStop.setOnClickListener {
            isRunning = false
            handler.removeCallbacksAndMessages(null)
            startActivity(Intent(this, FeedbackActivity::class.java))
            finish()
        }
    }

    private fun updateDots() {
        val dots = listOf(dot1, dot2, dot3)
        for (i in dots.indices) {
            dots[i].setBackgroundResource(when {
                i < sets -> R.drawable.dot_done
                i == sets -> R.drawable.dot_active
                else -> R.drawable.dot_todo
            })
        }
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            val provider = future.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            try {
                provider.unbindAll()
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, preview)
            } catch (e: Exception) {
                Toast.makeText(this, "Camera error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startFeedbackLoop() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (isRunning) {
                    tvFeedback.text = feedbackMessages[msgIndex % feedbackMessages.size]
                    tvFeedbackStrip.text = stripMessages[msgIndex % stripMessages.size]
                    msgIndex++
                    handler.postDelayed(this, 2000)
                }
            }
        }, 2000)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE && allPermissionsGranted()) startCamera()
        else Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    companion object {
        private const val REQUEST_CODE = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
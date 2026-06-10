package com.example.physioapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.atan2

class ExerciseSessionActivity : AppCompatActivity() {

    // ─── Views ───────────────────────────────────────
    private lateinit var previewView:    PreviewView
    private lateinit var poseOverlay:    PoseOverlayView
    private lateinit var tvExerciseLabel: TextView
    private lateinit var tvRepCount:     TextView
    private lateinit var tvSetCount:     TextView
    private lateinit var tvLiveFeedback: TextView
    private lateinit var feedbackDot:    View
    private lateinit var btnFinish:      Button
    private lateinit var btnStop:        Button

    // ─── MediaPipe + Camera ───────────────────────────
    private lateinit var poseLandmarker: PoseLandmarker
    private lateinit var cameraExecutor: ExecutorService

    // ─── Session state ────────────────────────────────
    private var exerciseName = "Arm Raise"
    private var totalSets    = 3
    private var totalReps    = 10
    private var currentSet   = 1
    private var repCount     = 0
    private var correctReps  = 0
    private var totalFrames  = 0
    private var goodFrames   = 0
    private var isDown       = false
    private var isStopped    = false

    // ─── Angle smoothing (last 5 readings) ───────────
    private val angleBuffer = ArrayDeque<Double>(5)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_session)

        // Get data from ExerciseDetailActivity
        exerciseName = intent.getStringExtra("EXERCISE_NAME") ?: "Arm Raise"
        totalSets    = intent.getIntExtra("EXERCISE_SETS", 3)
        totalReps    = intent.getIntExtra("EXERCISE_REPS", 10)

        // Bind views
        previewView     = findViewById(R.id.cameraPreview)
        poseOverlay     = findViewById(R.id.poseOverlay)
        tvExerciseLabel = findViewById(R.id.tvExerciseLabel)
        tvRepCount      = findViewById(R.id.tvRepCount)
        tvSetCount      = findViewById(R.id.tvSetCount)
        tvLiveFeedback  = findViewById(R.id.tvLiveFeedback)
        feedbackDot     = findViewById(R.id.feedbackDot)
        btnFinish       = findViewById(R.id.btnFinishSession)
        btnStop         = findViewById(R.id.btnStopSession)

        tvExerciseLabel.text = exerciseName
        tvSetCount.text      = "$currentSet/$totalSets"
        tvRepCount.text      = "0"

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Back button
        findViewById<TextView>(R.id.btnBackSession).setOnClickListener {
            finish()
        }

        // Check permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA), 100
            )
        } else {
            setupMediaPipe()
            startCamera()
        }

        // End session button
        btnStop.setOnClickListener {
            isStopped = true
            goToFeedback()
        }

        // Complete set button — manual override
        btnFinish.setOnClickListener {
            isStopped = true
            goToFeedback()
        }
    }

    // ─── MediaPipe Setup ─────────────────────────────
    private fun setupMediaPipe() {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("pose_landmarker_lite.task")
                .build()

            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener { result, input ->
                    processResult(result, input.width, input.height)
                }
                .setErrorListener { error ->
                    runOnUiThread {
                        tvLiveFeedback.text = "Detection error: ${error.message}"
                    }
                }
                .build()

            poseLandmarker = PoseLandmarker.createFromOptions(this, options)

        } catch (e: Exception) {
            runOnUiThread {
                tvLiveFeedback.text = "Failed to load model"
            }
        }
    }

    // ─── Camera Setup ────────────────────────────────
    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            val provider = future.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val analyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        if (!isStopped) processImageProxy(imageProxy)
                        else imageProxy.close()
                    }
                }

            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview,
                    analyzer
                )
            } catch (e: Exception) {
                runOnUiThread {
                    tvLiveFeedback.text = "Camera failed: ${e.message}"
                }
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // ─── Process Frame ────────────────────────────────
    private fun processImageProxy(imageProxy: ImageProxy) {
        val bitmap  = imageProxy.toBitmap()
        val rotated = rotateBitmap(bitmap, imageProxy.imageInfo.rotationDegrees.toFloat())
        val mpImage = BitmapImageBuilder(rotated).build()
        poseLandmarker.detectAsync(mpImage, System.currentTimeMillis())
        imageProxy.close()
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    // ─── Process MediaPipe Result ─────────────────────
    private fun processResult(result: PoseLandmarkerResult, imgW: Int, imgH: Int) {

        // Update skeleton overlay
        runOnUiThread {
            if (result.landmarks().isEmpty()) {
                poseOverlay.clear()
                tvLiveFeedback.text = "Stand back so full body is visible 🧍"
                tvLiveFeedback.setTextColor(Color.parseColor("#FF9500"))
                feedbackDot.setBackgroundResource(R.drawable.circle_red)
                return@runOnUiThread
            }
            poseOverlay.setResults(result, imgW, imgH)
        }

        if (result.landmarks().isEmpty()) return

        val landmarks = result.landmarks()[0]
        totalFrames++

        val feedback:   String
        val isGoodForm: Boolean

        when (exerciseName) {

            // ── ARM RAISE ──────────────────────────────
            "Arm Raise" -> {
                val hip      = landmarks[23]
                val shoulder = landmarks[11]
                val elbow    = landmarks[13]

                val rawAngle = calculateAngle(
                    hip.x(),      hip.y(),
                    shoulder.x(), shoulder.y(),
                    elbow.x(),    elbow.y()
                )
                val angle = smoothAngle(rawAngle)

                // Rep counting
                if (angle < 100 && !isDown) isDown = true
                if (angle > 160 && isDown) {
                    isDown = false
                    repCount++
                    runOnUiThread { tvRepCount.text = repCount.toString() }
                    checkSetComplete()
                }

                isGoodForm = angle in 70.0..110.0
                feedback = when {
                    angle > 160           -> "Raise your arm to shoulder level ➡️"
                    angle in 100.0..160.0 -> "Keep raising! Almost there ⬆️"
                    angle in 70.0..100.0  -> "Perfect! Hold it level 💪"
                    angle < 70            -> "Too high! Lower to shoulder level ⬇️"
                    else                  -> "Good form! Keep going 🎯"
                }
            }

            // ── SQUAT ──────────────────────────────────
            "Squat" -> {
                val hip   = landmarks[23]
                val knee  = landmarks[25]
                val ankle = landmarks[27]

                val rawAngle = calculateAngle(
                    hip.x(),   hip.y(),
                    knee.x(),  knee.y(),
                    ankle.x(), ankle.y()
                )
                val angle = smoothAngle(rawAngle)

                // Rep counting
                if (angle < 90 && !isDown) isDown = true
                if (angle > 160 && isDown) {
                    isDown = false
                    repCount++
                    runOnUiThread { tvRepCount.text = repCount.toString() }
                    checkSetComplete()
                }

                isGoodForm = angle in 80.0..170.0
                feedback = when {
                    angle > 170 -> "Squat lower — bend your knees more 🔽"
                    angle < 70  -> "Too deep! Come up slightly 🔼"
                    angle in 80.0..100.0 -> "Perfect squat depth! 🎯"
                    else        -> "Good form! Keep going 💪"
                }
            }

            // ── NECK STRETCH ───────────────────────────
            "Neck Stretch" -> {
                val leftShoulder  = landmarks[11]
                val rightShoulder = landmarks[12]
                val nose          = landmarks[0]

                // Check if head is tilting sideways
                val shoulderMidX = (leftShoulder.x() + rightShoulder.x()) / 2
                val headOffset   = abs(nose.x() - shoulderMidX)

                // Good stretch = head offset > 0.05 from center
                isGoodForm = headOffset > 0.05f
                feedback = when {
                    headOffset < 0.03f -> "Tilt your head to the side gently 🧘"
                    headOffset in 0.03f..0.07f -> "Keep stretching a little more ↔️"
                    headOffset > 0.07f -> "Perfect stretch! Hold it 🎯"
                    else -> "Hold the stretch gently 🧘"
                }

                // For neck stretch count reps differently
                // Each time head returns to center = 1 rep
                if (headOffset < 0.02f && isDown) {
                    isDown = false
                    repCount++
                    runOnUiThread { tvRepCount.text = repCount.toString() }
                    checkSetComplete()
                }
                if (headOffset > 0.07f) isDown = true
            }

            else -> {
                isGoodForm = true
                feedback   = "Keep going! 💪"
            }
        }

        // Track accuracy
        if (isGoodForm) goodFrames++

        runOnUiThread {
            tvLiveFeedback.text = feedback
            if (isGoodForm) {
                tvLiveFeedback.setTextColor(Color.parseColor("#2BAE7E"))
                feedbackDot.setBackgroundResource(R.drawable.circle_green)
            } else {
                tvLiveFeedback.setTextColor(Color.parseColor("#FF3B30"))
                feedbackDot.setBackgroundResource(R.drawable.circle_red)
            }
        }
    }

    // ─── Check if Set is Complete ─────────────────────
    private fun checkSetComplete() {
        if (repCount >= totalReps * currentSet) {
            if (currentSet >= totalSets) {
                // All sets done!
                runOnUiThread {
                    isStopped = true
                    Toast.makeText(
                        this,
                        "All sets complete! Amazing work! 🎉",
                        Toast.LENGTH_SHORT
                    ).show()
                    goToFeedback()
                }
            } else {
                // One set done — ask for break
                runOnUiThread {
                    showBreakDialog()
                }
            }
        }
    }

    // ─── Break Dialog ─────────────────────────────────
    private fun showBreakDialog() {
        val setJustDone = currentSet
        AlertDialog.Builder(this)
            .setTitle("Set $setJustDone Complete! 🔥")
            .setMessage(
                "Great job! You completed set $setJustDone of $totalSets.\n\n" +
                        "Would you like a short break before the next set?"
            )
            .setPositiveButton("Take a Break 😴") { _, _ ->
                // Show countdown then continue
                showBreakCountdown()
            }
            .setNegativeButton("Continue Now 💪") { _, _ ->
                currentSet++
                runOnUiThread {
                    tvSetCount.text = "$currentSet/$totalSets"
                }
            }
            .setCancelable(false)
            .show()
    }

    // ─── Break Countdown ─────────────────────────────
    private fun showBreakCountdown() {
        var seconds = 30
        val dialog = AlertDialog.Builder(this)
            .setTitle("Rest Time 😴")
            .setMessage("Starting next set in $seconds seconds...")
            .setNegativeButton("Skip Break") { d, _ ->
                d.dismiss()
                currentSet++
                runOnUiThread { tvSetCount.text = "$currentSet/$totalSets" }
            }
            .setCancelable(false)
            .create()

        dialog.show()

        // Countdown timer
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                seconds--
                if (seconds <= 0) {
                    dialog.dismiss()
                    currentSet++
                    tvSetCount.text = "$currentSet/$totalSets"
                    Toast.makeText(
                        this@ExerciseSessionActivity,
                        "Set $currentSet starting! Go! 💪",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    dialog.setMessage("Starting set ${ currentSet + 1 } in $seconds seconds...")
                    handler.postDelayed(this, 1000)
                }
            }
        }
        handler.postDelayed(runnable, 1000)
    }

    // ─── Go to Feedback ───────────────────────────────
    private fun goToFeedback() {
        // Calculate final accuracy
        val accuracy = if (totalFrames == 0) 0
        else ((goodFrames.toFloat() / totalFrames) * 100).toInt()
            .coerceIn(0, 100)

        val intent = Intent(this, FeedbackActivity::class.java)
        intent.putExtra("EXERCISE_NAME", exerciseName)
        intent.putExtra("ACCURACY",      accuracy)
        intent.putExtra("REPS_DONE",     repCount)
        startActivity(intent)
        finish()
    }

    // ─── Angle Smoothing ──────────────────────────────
    private fun smoothAngle(newAngle: Double): Double {
        if (angleBuffer.size >= 5) angleBuffer.removeFirst()
        angleBuffer.addLast(newAngle)
        return angleBuffer.average()
    }

    // ─── Calculate Angle Between 3 Points ────────────
    private fun calculateAngle(
        ax: Float, ay: Float,
        bx: Float, by: Float,
        cx: Float, cy: Float
    ): Double {
        val radians = atan2(
            (cy - by).toDouble(), (cx - bx).toDouble()
        ) - atan2(
            (ay - by).toDouble(), (ax - bx).toDouble()
        )
        var angle = abs(radians * 180.0 / Math.PI)
        if (angle > 180.0) angle = 360.0 - angle
        return angle
    }

    // ─── Permission Result ────────────────────────────
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupMediaPipe()
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
        }
    }

    // ─── Cleanup ─────────────────────────────────────
    override fun onDestroy() {
        super.onDestroy()
        isStopped = true
        cameraExecutor.shutdown()
        if (::poseLandmarker.isInitialized) poseLandmarker.close()
    }
}
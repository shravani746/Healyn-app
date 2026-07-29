package com.example.physioapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
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
    private lateinit var previewView:     PreviewView
    private lateinit var poseOverlay:     PoseOverlayView
    private lateinit var tvExerciseLabel: TextView
    private lateinit var tvRepCount:      TextView
    private lateinit var tvSetCount:      TextView
    private lateinit var tvLiveFeedback:  TextView
    private lateinit var feedbackDot:     View
    private lateinit var btnFinish:       Button
    private lateinit var btnStop:         Button

    // ─── MediaPipe + Camera ───────────────────────────
    private lateinit var poseLandmarker: PoseLandmarker
    private lateinit var cameraExecutor: ExecutorService

    // ─── Session state ────────────────────────────────
    private var exerciseName = "Arm Raise"
    private var totalSets    = 3
    private var totalReps    = 10
    private var currentSet   = 1
    private var repCount     = 0
    private var goodFrames   = 0
    private var totalFrames  = 0
    private var isDown       = false
    private var isStopped    = false
    private var isBreakShowing = false

    // ─── Angle smoothing (last 5 readings) ───────────
    private val angleBuffer = ArrayDeque<Double>(5)

    // ─── Debug logging ────────────────────────────────
    private var frameCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_session)

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

        findViewById<TextView>(R.id.btnBackSession).setOnClickListener {
            finish()
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA), 100
            )
        } else {
            setupMediaPipe()
            startCamera()
        }

        btnStop.setOnClickListener {
            isStopped = true
            goToFeedback()
        }

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
                .setMinPoseDetectionConfidence(0.5f)
                .setMinPosePresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setResultListener { result, input ->
                    processResult(result, input.width, input.height)
                }
                .setErrorListener { error ->
                    android.util.Log.e("MEDIAPIPE", "Error: ${error.message}")
                    runOnUiThread {
                        tvLiveFeedback.text = "Detection error: ${error.message}"
                    }
                }
                .build()

            poseLandmarker = PoseLandmarker.createFromOptions(this, options)
            android.util.Log.d("MEDIAPIPE", "MediaPipe setup successful")

        } catch (e: Exception) {
            android.util.Log.e("MEDIAPIPE", "Setup failed: ${e.message}")
            runOnUiThread {
                tvLiveFeedback.text = "Failed to load model: ${e.message}"
                tvLiveFeedback.setTextColor(Color.parseColor("#FF3B30"))
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
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
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
                android.util.Log.d("CAMERA", "Camera started successfully")
            } catch (e: Exception) {
                android.util.Log.e("CAMERA", "Camera failed: ${e.message}")
                runOnUiThread {
                    tvLiveFeedback.text = "Camera failed: ${e.message}"
                    tvLiveFeedback.setTextColor(Color.parseColor("#FF3B30"))
                }
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // ─── Process Each Camera Frame ────────────────────
    private fun processImageProxy(imageProxy: ImageProxy) {
        try {
            val bitmap  = imageProxy.toBitmap()
            val rotated = rotateBitmap(
                bitmap,
                imageProxy.imageInfo.rotationDegrees.toFloat()
            )
            val mpImage = BitmapImageBuilder(rotated).build()
            poseLandmarker.detectAsync(mpImage, System.currentTimeMillis())

            frameCount++
            if (frameCount % 30 == 0) {
                android.util.Log.d("CAMERA", "Frames processed: $frameCount")
            }
        } catch (e: Exception) {
            android.util.Log.e("CAMERA", "Frame processing error: ${e.message}")
        } finally {
            imageProxy.close()
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(
            bitmap, 0, 0,
            bitmap.width, bitmap.height,
            matrix, true
        )
    }

    // ─── Process MediaPipe Result ─────────────────────
    private fun processResult(result: PoseLandmarkerResult, imgW: Int, imgH: Int) {

        runOnUiThread {
            if (result.landmarks().isEmpty()) {
                // No person detected — clear overlay
                poseOverlay.clear()
                tvLiveFeedback.text = getNoDetectionMessage()
                tvLiveFeedback.setTextColor(Color.parseColor("#FF9500"))
                feedbackDot.setBackgroundResource(R.drawable.circle_red)
                android.util.Log.d("POSE", "No landmarks detected")
                return@runOnUiThread
            }

            // Person detected — update overlay
            poseOverlay.setResults(result, imgW, imgH)
            android.util.Log.d("POSE",
                "Landmarks detected: ${result.landmarks()[0].size}"
            )
        }

        if (result.landmarks().isEmpty()) return

        val landmarks = result.landmarks()[0]
        totalFrames++

        val feedback:   String
        val isGoodForm: Boolean

        when (exerciseName) {

            // ── ARM RAISE (Lateral raise to shoulder level) ──
            "Arm Raise" -> {
                val hip      = landmarks[23]
                val shoulder = landmarks[11]
                val elbow    = landmarks[13]

                // Check visibility of key landmarks
                if ((hip.visibility().orElse(0f)) < 0.4f ||
                    (shoulder.visibility().orElse(0f)) < 0.4f ||
                    (elbow.visibility().orElse(0f)) < 0.4f) {
                    runOnUiThread {
                        tvLiveFeedback.text =
                            "Show your arm and hip clearly 🧍"
                        tvLiveFeedback.setTextColor(
                            Color.parseColor("#FF9500")
                        )
                    }
                    return
                }

                val rawAngle = calculateAngle(
                    hip.x(),      hip.y(),
                    shoulder.x(), shoulder.y(),
                    elbow.x(),    elbow.y()
                )
                val angle = smoothAngle(rawAngle)

                android.util.Log.d("ANGLE", "Arm angle: $angle")

                // Arm at side      = ~180°
                // Arm at shoulder  = ~80° to 100° (T-shape)
                // Rep counted when:
                // 1. Arm UP — angle drops below 100° → isDown = true
                // 2. Arm DOWN — angle rises above 155° → count rep

                if (angle < 100 && !isDown) {
                    isDown = true
                    android.util.Log.d("REP", "Arm UP detected")
                }
                if (angle > 155 && isDown) {
                    isDown = false
                    repCount++
                    android.util.Log.d("REP", "Rep counted: $repCount")
                    runOnUiThread { tvRepCount.text = repCount.toString() }
                    checkSetComplete()
                }

                isGoodForm = angle in 75.0..105.0

                feedback = when {
                    angle > 155           -> "Raise your arm to shoulder level ➡️"
                    angle in 105.0..155.0 -> "Keep raising! Almost there ⬆️"
                    angle in 75.0..105.0  -> "Perfect T-shape! Hold it 💪"
                    angle < 75            -> "Too high! Lower to shoulder level ⬇️"
                    else                  -> "Good form! 🎯"
                }
            }

            // ── SQUAT ────────────────────────────────────────
            "Squat" -> {
                val hip   = landmarks[23]
                val knee  = landmarks[25]
                val ankle = landmarks[27]

                if ((hip.visibility().orElse(0f)) < 0.4f ||
                    (knee.visibility().orElse(0f)) < 0.4f ||
                    (ankle.visibility().orElse(0f)) < 0.4f) {
                    runOnUiThread {
                        tvLiveFeedback.text =
                            "Show your full legs clearly 🧍"
                        tvLiveFeedback.setTextColor(
                            Color.parseColor("#FF9500")
                        )
                    }
                    return
                }

                val rawAngle = calculateAngle(
                    hip.x(),   hip.y(),
                    knee.x(),  knee.y(),
                    ankle.x(), ankle.y()
                )
                val angle = smoothAngle(rawAngle)

                android.util.Log.d("ANGLE", "Squat angle: $angle")

                // Standing  = ~170°
                // Squat     = ~80° to 90°
                if (angle < 90 && !isDown) {
                    isDown = true
                    android.util.Log.d("REP", "Squat DOWN detected")
                }
                if (angle > 160 && isDown) {
                    isDown = false
                    repCount++
                    android.util.Log.d("REP", "Squat rep counted: $repCount")
                    runOnUiThread { tvRepCount.text = repCount.toString() }
                    checkSetComplete()
                }

                isGoodForm = angle in 80.0..170.0

                feedback = when {
                    angle > 170          -> "Squat lower — bend knees more 🔽"
                    angle < 70           -> "Too deep! Come up slightly 🔼"
                    angle in 80.0..100.0 -> "Perfect squat depth! 🎯"
                    else                 -> "Good form! Keep going 💪"
                }
            }

            // ── NECK STRETCH ──────────────────────────────────
            // Only upper body needed — no full body required
            "Neck Stretch" -> {
                val nose          = landmarks[0]
                val leftShoulder  = landmarks[11]
                val rightShoulder = landmarks[12]

                // Check only upper body landmarks
                if ((nose.visibility().orElse(0f)) < 0.4f ||
                    (leftShoulder.visibility().orElse(0f)) < 0.4f ||
                    (rightShoulder.visibility().orElse(0f)) < 0.4f) {
                    runOnUiThread {
                        tvLiveFeedback.text =
                            "Show head and shoulders clearly 🧍"
                        tvLiveFeedback.setTextColor(
                            Color.parseColor("#FF9500")
                        )
                    }
                    return
                }

                // Midpoint between shoulders
                val shoulderMidX =
                    (leftShoulder.x() + rightShoulder.x()) / 2f
                val shoulderMidY =
                    (leftShoulder.y() + rightShoulder.y()) / 2f

                // Head must be above shoulders
                val isHeadAboveShoulders = nose.y() < shoulderMidY

                if (!isHeadAboveShoulders) {
                    isGoodForm = false
                    feedback = "Sit up straight — keep head above shoulders 🧍"
                } else {
                    val lateralOffset = abs(nose.x() - shoulderMidX)
                    android.util.Log.d("NECK",
                        "Lateral offset: $lateralOffset"
                    )

                    // Head returns to center → count rep
                    if (lateralOffset < 0.02f && isDown) {
                        isDown = false
                        repCount++
                        android.util.Log.d("REP",
                            "Neck stretch rep counted: $repCount"
                        )
                        runOnUiThread {
                            tvRepCount.text = repCount.toString()
                        }
                        checkSetComplete()
                    }

                    // Head tilted enough → mark as down
                    if (lateralOffset > 0.08f) isDown = true

                    isGoodForm = lateralOffset > 0.06f

                    feedback = when {
                        lateralOffset < 0.02f ->
                            "Tilt your head to the side gently ↔️"
                        lateralOffset in 0.02f..0.06f ->
                            "Keep stretching more ➡️"
                        lateralOffset in 0.06f..0.10f ->
                            "Good stretch! Hold it 🧘"
                        lateralOffset > 0.10f ->
                            "Perfect range of motion! 🎯"
                        else ->
                            "Hold the stretch gently 🧘"
                    }
                }
            }

            else -> {
                isGoodForm = true
                feedback   = "Keep going! 💪"
            }
        }

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

    // ─── Message when no person detected ─────────────
    private fun getNoDetectionMessage(): String {
        return when (exerciseName) {
            "Neck Stretch" ->
                "Show head and shoulders in frame 🧍"
            "Arm Raise" ->
                "Show upper body and arm in frame 🧍"
            else ->
                "Stand back so full body is visible 🧍"
        }
    }

    // ─── Check if Set is Complete ─────────────────────
    private fun checkSetComplete() {
        if (repCount >= totalReps * currentSet && !isBreakShowing) {
            if (currentSet >= totalSets) {
                runOnUiThread {
                    isStopped = true
                    Toast.makeText(
                        this,
                        "All sets complete! Amazing work! 🎉",
                        Toast.LENGTH_LONG
                    ).show()
                    Handler(Looper.getMainLooper()).postDelayed({
                        goToFeedback()
                    }, 1500)
                }
            } else {
                runOnUiThread {
                    isBreakShowing = true
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
                        "Would you like a 30 second break before the next set?"
            )
            .setPositiveButton("Take a Break 😴") { _, _ ->
                showBreakCountdown()
            }
            .setNegativeButton("Continue Now 💪") { _, _ ->
                currentSet++
                isBreakShowing = false
                runOnUiThread {
                    tvSetCount.text = "$currentSet/$totalSets"
                    Toast.makeText(
                        this,
                        "Set $currentSet — Go! 💪",
                        Toast.LENGTH_SHORT
                    ).show()
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
            .setMessage("Starting set ${currentSet + 1} in $seconds seconds...")
            .setNegativeButton("Skip Break 💪") { d, _ ->
                d.dismiss()
                currentSet++
                isBreakShowing = false
                runOnUiThread {
                    tvSetCount.text = "$currentSet/$totalSets"
                    Toast.makeText(
                        this,
                        "Set $currentSet — Go! 💪",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setCancelable(false)
            .create()

        dialog.show()

        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                seconds--
                if (seconds <= 0) {
                    dialog.dismiss()
                    currentSet++
                    isBreakShowing = false
                    runOnUiThread {
                        tvSetCount.text = "$currentSet/$totalSets"
                        Toast.makeText(
                            this@ExerciseSessionActivity,
                            "Set $currentSet — Go! 💪",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    dialog.setMessage(
                        "Starting set ${currentSet + 1} in $seconds seconds..."
                    )
                    handler.postDelayed(this, 1000)
                }
            }
        }
        handler.postDelayed(runnable, 1000)
    }

    // ─── Go to Feedback ───────────────────────────────
    private fun goToFeedback() {
        val accuracy = if (totalFrames == 0) 0
        else ((goodFrames.toFloat() / totalFrames) * 100)
            .toInt()
            .coerceIn(0, 100)

        android.util.Log.d("ACCURACY",
            "Good: $goodFrames / Total: $totalFrames = $accuracy%"
        )

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

    // ─── Calculate Angle at Point B ──────────────────
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
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            setupMediaPipe()
            startCamera()
        } else {
            Toast.makeText(
                this,
                "Camera permission required for pose detection",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ─── Cleanup ─────────────────────────────────────
    override fun onDestroy() {
        super.onDestroy()
        isStopped = true
        cameraExecutor.shutdown()
        if (::poseLandmarker.isInitialized) {
            poseLandmarker.close()
        }
    }
}
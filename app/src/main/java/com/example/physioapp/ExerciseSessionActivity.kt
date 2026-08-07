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
import android.speech.tts.TextToSpeech
import java.util.Locale
import com.example.physioapp.model.SaveProgressRequest
import com.example.physioapp.model.ProgressResponse
import com.example.physioapp.model.ResumeProgressResponse
import com.example.physioapp.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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
    // Nullable (not lateinit) so a model-load failure degrades gracefully instead of crashing.
    private var poseLandmarker: PoseLandmarker? = null
    private lateinit var cameraExecutor: ExecutorService

    // ─── Voice Assistant ───────────────────────────────
    private lateinit var textToSpeech: TextToSpeech
    private var lastSpokenFeedback = ""
    private var lastSpeechTime = 0L

    // ─── Session state ────────────────────────────────
    private var exerciseName = "Right Arm Lateral Raise"
    private var totalSets    = 3
    private var totalReps    = 10
    private var currentSet   = 1
    private var repCount     = 0
    private lateinit var userId: String
    private var isStopped    = false
    private var isBreakShowing = false

    // ─── Right Arm Lateral Raise rep-quality state ────
    private enum class ArmPhase { WAITING_DOWN, WAITING_T_UP, WAITING_OVERHEAD, WAITING_T_DOWN, WAITING_RETURN_DOWN }
    private var armPhase             = ArmPhase.WAITING_DOWN
    private var peakShoulderAngle    = 0.0
    private var minElbowAngleThisRep = 180.0
    private val armRepScores         = mutableListOf<Double>()
    private val armShoulderAngleBuffer = ArrayDeque<Double>(5)

    // ─── Squat rep-quality state ──────────────────────
    private enum class SquatPhase { WAITING_STAND, WAITING_SQUAT, WAITING_RETURN_STAND }
    private var squatPhase           = SquatPhase.WAITING_STAND
    private var minKneeAngleThisRep  = 180.0
    private var minBackAngleThisRep  = 180.0
    private val squatRepScores       = mutableListOf<Double>()
    private val kneeAngleBuffer      = ArrayDeque<Double>(5)
    private val backAngleBuffer      = ArrayDeque<Double>(5)

    // ─── Neck Stretch rep-quality state ───────────────
    private enum class NeckPhase { WAITING_CENTER, HOLDING_TILT, WAITING_RETURN_CENTER }
    private var neckPhase        = NeckPhase.WAITING_CENTER
    private var neckHoldStartTime = 0L
    private var neckHeldMillis    = 0L
    private var neckMaxTiltRatio  = 0.0
    private val neckRepScores     = mutableListOf<Double>()

    // ─── Debug logging ────────────────────────────────
    private var frameCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_session)
        val sharedPreferences = getSharedPreferences("HealynApp", MODE_PRIVATE)
        userId = sharedPreferences.getString("USER_ID", "") ?: ""

        exerciseName = intent.getStringExtra("EXERCISE_NAME") ?: "Right Arm Lateral Raise"
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

        loadProgress()

        cameraExecutor = Executors.newSingleThreadExecutor()

        findViewById<TextView>(R.id.btnBackSession).setOnClickListener {
            finish()
        }

        // Initialize Voice Assistant
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale.US
                textToSpeech.setSpeechRate(0.9f)
                textToSpeech.setPitch(1.0f)

                Handler(Looper.getMainLooper()).postDelayed({

                    if (exerciseName == "Right Arm Lateral Raise") {

                        textToSpeech.speak(
                            "Welcome to Healyn. Let's begin the Right Arm Lateral Raise exercise.",
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            "welcome"
                        )

                    } else if (exerciseName == "Squat") {

                        textToSpeech.speak(
                            "Welcome to Healyn. Let's begin the Squat exercise.",
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            "welcome"
                        )

                    }

                }, 1000)
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                100
            )
        } else {
            setupMediaPipe()
            startCamera()
        }

        btnStop.setOnClickListener {
            isStopped = true
            saveProgress("In Progress")
            goToFeedback()
        }

        btnFinish.setOnClickListener {
            isStopped = true
            saveProgress("Completed")
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

            poseLandmarker = try {

                PoseLandmarker.createFromOptions(
                    this,
                    options
                )

            } catch (e: Exception) {

                android.util.Log.e(
                    "MEDIAPIPE",
                    "Loading failed: ${e.message}"
                )

                runOnUiThread {
                    tvLiveFeedback.text =
                        "AI detection unavailable"
                }

                null
            }
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
                        if (!isStopped && !isBreakShowing) processImageProxy(imageProxy)
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
            poseLandmarker?.detectAsync(
                mpImage,
                System.currentTimeMillis()
            )

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
                poseOverlay.clear()
                tvLiveFeedback.text = getNoDetectionMessage()
                tvLiveFeedback.setTextColor(Color.parseColor("#FF9500"))
                feedbackDot.setBackgroundResource(R.drawable.circle_red)
                android.util.Log.d("POSE", "No landmarks detected")
                return@runOnUiThread
            }
            poseOverlay.setResults(result, imgW, imgH)
        }

        if (result.landmarks().isEmpty()) return

        val landmarks = result.landmarks()[0]

        val feedback:   String
        val isGoodForm: Boolean

        when (exerciseName) {

            // ── RIGHT ARM LATERAL RAISE (side → T → overhead → T → side, elbow straight) ──
            "Right Arm Lateral Raise" -> {
                val hip      = landmarks[24]
                val shoulder = landmarks[12]
                val elbow    = landmarks[14]
                val wrist    = landmarks[16]

                if (listOf(hip, shoulder, elbow, wrist).any { (it.visibility().orElse(0f)) < 0.4f }) {
                    runOnUiThread {
                        tvLiveFeedback.text = "Show your right arm and torso clearly"
                        tvLiveFeedback.setTextColor(Color.parseColor("#FF9500"))
                    }
                    return
                }

                val rawShoulderAngle = calculateAngle(
                    hip.x(), hip.y(), shoulder.x(), shoulder.y(), elbow.x(), elbow.y()
                )
                val shoulderAngle = smoothAngle(rawShoulderAngle, armShoulderAngleBuffer)
                val elbowAngle = calculateAngle(
                    shoulder.x(), shoulder.y(), elbow.x(), elbow.y(), wrist.x(), wrist.y()
                )

                android.util.Log.d("ANGLE_CHECK",
                    "shoulderAngle=$shoulderAngle elbowAngle=$elbowAngle phase=$armPhase")

                if (armPhase != ArmPhase.WAITING_DOWN) {
                    peakShoulderAngle    = maxOf(peakShoulderAngle, shoulderAngle)
                    minElbowAngleThisRep = minOf(minElbowAngleThisRep, elbowAngle)
                }

                when (armPhase) {
                    ArmPhase.WAITING_DOWN -> {
                        if (shoulderAngle < 30.0) {
                            armPhase = ArmPhase.WAITING_T_UP
                            peakShoulderAngle = shoulderAngle
                            minElbowAngleThisRep = elbowAngle
                        }
                    }
                    ArmPhase.WAITING_T_UP -> {
                        if (shoulderAngle in 70.0..110.0) armPhase = ArmPhase.WAITING_OVERHEAD
                    }
                    ArmPhase.WAITING_OVERHEAD -> {
                        if (shoulderAngle > 150.0) armPhase = ArmPhase.WAITING_T_DOWN
                    }
                    ArmPhase.WAITING_T_DOWN -> {
                        if (shoulderAngle in 70.0..110.0) armPhase = ArmPhase.WAITING_RETURN_DOWN
                    }
                    ArmPhase.WAITING_RETURN_DOWN -> {
                        if (shoulderAngle < 30.0) {
                            val romScore   = ((peakShoulderAngle - 150.0) / 30.0).coerceIn(0.0, 1.0)
                            val elbowScore = ((minElbowAngleThisRep - 120.0) / 40.0).coerceIn(0.0, 1.0)
                            val repScore   = (romScore + elbowScore) / 2.0
                            armRepScores.add(repScore)

                            repCount++
                            if (exerciseName == "Right Arm Lateral Raise" ||
                                exerciseName == "Squat") {

                                textToSpeech.speak(
                                    "Great job! Repetition completed.",
                                    TextToSpeech.QUEUE_ADD,
                                    null,
                                    "rep"
                                )
                            }
                            android.util.Log.d("REP",
                                "Arm rep $repCount: rom=$romScore elbow=$elbowScore score=$repScore")
                            runOnUiThread { tvRepCount.text = repCount.toString() }
                            checkSetComplete()

                            armPhase = ArmPhase.WAITING_DOWN
                            peakShoulderAngle = 0.0
                            minElbowAngleThisRep = 180.0
                        }
                    }
                }

                isGoodForm = elbowAngle > 150.0
                feedback = when {
                    elbowAngle <= 150.0 -> "Straighten your elbow"
                    armPhase == ArmPhase.WAITING_DOWN -> "Start with your arm relaxed at your side"
                    armPhase == ArmPhase.WAITING_T_UP -> "Raise your arm out to shoulder height"
                    armPhase == ArmPhase.WAITING_OVERHEAD -> "Keep raising your arm overhead"
                    armPhase == ArmPhase.WAITING_T_DOWN -> "Lower your arm back to shoulder height"
                    else -> "Lower your arm back down to your side"
                }
            }

            // ── SQUAT (stand → squat → stand, watching knee depth + back lean) ──
            "Squat" -> {
                val shoulder = landmarks[11]
                val hip      = landmarks[23]
                val knee     = landmarks[25]
                val ankle    = landmarks[27]

                if (listOf(shoulder, hip, knee, ankle).any { (it.visibility().orElse(0f)) < 0.4f }) {
                    runOnUiThread {
                        tvLiveFeedback.text = "Show your full body clearly"
                        tvLiveFeedback.setTextColor(Color.parseColor("#FF9500"))
                    }
                    return
                }

                val rawKneeAngle = calculateAngle(
                    hip.x(), hip.y(), knee.x(), knee.y(), ankle.x(), ankle.y()
                )
                val kneeAngle = smoothAngle(rawKneeAngle, kneeAngleBuffer)

                val rawBackAngle = calculateAngle(
                    shoulder.x(), shoulder.y(), hip.x(), hip.y(), knee.x(), knee.y()
                )
                val backAngle = smoothAngle(rawBackAngle, backAngleBuffer)

                android.util.Log.d("ANGLE_CHECK",
                    "kneeAngle=$kneeAngle backAngle=$backAngle phase=$squatPhase")

                if (squatPhase != SquatPhase.WAITING_STAND) {
                    minKneeAngleThisRep = minOf(minKneeAngleThisRep, kneeAngle)
                    minBackAngleThisRep = minOf(minBackAngleThisRep, backAngle)
                }

                when (squatPhase) {
                    SquatPhase.WAITING_STAND -> {
                        if (kneeAngle > 160.0) squatPhase = SquatPhase.WAITING_SQUAT
                    }
                    SquatPhase.WAITING_SQUAT -> {
                        if (kneeAngle < 130.0) {
                            squatPhase = SquatPhase.WAITING_RETURN_STAND
                            minKneeAngleThisRep = kneeAngle
                            minBackAngleThisRep = backAngle
                        }
                    }
                    SquatPhase.WAITING_RETURN_STAND -> {
                        if (kneeAngle > 160.0) {
                            // Lenient depth target — full credit around a 140° knee bend,
                            // not a strict 90° squat (per patient/elderly use case).
                            val depthScore = ((170.0 - minKneeAngleThisRep) / 30.0).coerceIn(0.0, 1.0)
                            val backScore  = ((minBackAngleThisRep - 90.0) / 60.0).coerceIn(0.0, 1.0)
                            val repScore   = (depthScore + backScore) / 2.0
                            squatRepScores.add(repScore)

                            repCount++
                            if (exerciseName == "Right Arm Lateral Raise" ||
                                exerciseName == "Squat") {

                                textToSpeech.speak(
                                    "Great job! Repetition completed.",
                                    TextToSpeech.QUEUE_ADD,
                                    null,
                                    "rep"
                                )
                            }
                            android.util.Log.d("REP",
                                "Squat rep $repCount: depth=$depthScore back=$backScore score=$repScore")
                            runOnUiThread { tvRepCount.text = repCount.toString() }
                            checkSetComplete()

                            squatPhase = SquatPhase.WAITING_STAND
                            minKneeAngleThisRep = 180.0
                            minBackAngleThisRep = 180.0
                        }
                    }
                }

                isGoodForm = backAngle > 110.0
                feedback = when {
                    backAngle <= 110.0 -> "Keep your chest up, don't lean too far forward"
                    squatPhase == SquatPhase.WAITING_STAND -> "Stand tall to begin"
                    squatPhase == SquatPhase.WAITING_SQUAT -> "Lower down, bend your knees"
                    else -> "Push back up to standing"
                }
            }

            // ── NECK STRETCH (tilt toward right shoulder, hold 3 seconds, return to center) ──
            "Neck Stretch" -> {
                val nose          = landmarks[0]
                val leftShoulder  = landmarks[11]
                val rightShoulder = landmarks[12]

                if (listOf(nose, leftShoulder, rightShoulder).any { (it.visibility().orElse(0f)) < 0.4f }) {
                    runOnUiThread {
                        tvLiveFeedback.text = "Show your head and shoulders clearly"
                        tvLiveFeedback.setTextColor(Color.parseColor("#FF9500"))
                    }
                    return
                }

                val shoulderMidX  = (leftShoulder.x() + rightShoulder.x()) / 2f
                val shoulderMidY  = (leftShoulder.y() + rightShoulder.y()) / 2f
                val shoulderHalfWidth = rightShoulder.x() - shoulderMidX
                val isHeadAboveShoulders = nose.y() < shoulderMidY

                if (!isHeadAboveShoulders) {
                    isGoodForm = false
                    feedback = "Sit up straight, keep your head above your shoulders"
                } else if (abs(shoulderHalfWidth) < 0.001f) {
                    isGoodForm = false
                    feedback = "Face the camera directly"
                } else {
                    val tiltRatio = ((nose.x() - shoulderMidX) / shoulderHalfWidth).toDouble()
                    val now = System.currentTimeMillis()

                    android.util.Log.d("ANGLE_CHECK", "tiltRatio=$tiltRatio phase=$neckPhase")

                    when (neckPhase) {
                        NeckPhase.WAITING_CENTER -> {
                            if (tiltRatio > 0.5) {
                                neckPhase = NeckPhase.HOLDING_TILT
                                neckHoldStartTime = now
                                neckHeldMillis = 0L
                                neckMaxTiltRatio = tiltRatio
                            }
                        }
                        NeckPhase.HOLDING_TILT -> {
                            neckMaxTiltRatio = maxOf(neckMaxTiltRatio, tiltRatio)
                            if (tiltRatio > 0.5) {
                                neckHeldMillis = now - neckHoldStartTime
                            } else {
                                neckPhase = NeckPhase.WAITING_RETURN_CENTER
                            }
                        }
                        NeckPhase.WAITING_RETURN_CENTER -> { /* waiting for center below */ }
                    }

                    if (neckPhase != NeckPhase.WAITING_CENTER && tiltRatio < 0.15) {
                        val holdScore  = (neckHeldMillis / 3000.0).coerceIn(0.0, 1.0)
                        val depthScore = ((neckMaxTiltRatio - 0.5) / 0.5).coerceIn(0.0, 1.0)
                        val repScore   = (holdScore + depthScore) / 2.0
                        neckRepScores.add(repScore)

                        repCount++
                        android.util.Log.d("REP",
                            "Neck rep $repCount: hold=$holdScore depth=$depthScore score=$repScore")
                        runOnUiThread { tvRepCount.text = repCount.toString() }
                        checkSetComplete()

                        neckPhase = NeckPhase.WAITING_CENTER
                        neckHeldMillis = 0L
                        neckMaxTiltRatio = 0.0
                    }

                    isGoodForm = tiltRatio > 0.5
                    feedback = when {
                        neckPhase == NeckPhase.WAITING_CENTER ->
                            "Tilt your head gently toward your right shoulder"
                        neckPhase == NeckPhase.HOLDING_TILT && neckHeldMillis < 3000 ->
                            "Hold the stretch a little longer"
                        neckPhase == NeckPhase.HOLDING_TILT ->
                            "Great, hold a moment more"
                        else ->
                            "Return your head to center"
                    }
                }
            }

            else -> {
                isGoodForm = true
                feedback   = "Keep going"
            }
        }

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

        if (exerciseName == "Right Arm Lateral Raise" ||
            exerciseName == "Squat") {

            speakFeedback(
                feedback
                    .replace("💪", "")
                    .replace("🎯", "")
                    .replace("⬆️", "")
                    .replace("⬇️", "")
                    .replace("➡️", "")
                    .replace("🔽", "")
                    .replace("🔼", "")
                    .trim()
            )
        }
    }

    // ─── Message when no person detected ─────────────
    private fun getNoDetectionMessage(): String {
        return when (exerciseName) {
            "Neck Stretch" -> "Show head and shoulders in frame"
            "Right Arm Lateral Raise" -> "Show upper body and right arm in frame"
            else -> "Stand back so full body is visible"
        }
    }

    // ─── Check if Set is Complete ─────────────────────
    private fun checkSetComplete() {
        if (repCount >= totalReps * currentSet && !isBreakShowing) {
            if (currentSet >= totalSets) {
                runOnUiThread {
                    isStopped = true

                    // Save completed exercise
                    saveProgress("Completed")

                    Toast.makeText(this, "All sets complete! Amazing work!", Toast.LENGTH_LONG).show()
                    if (::textToSpeech.isInitialized) {
                        textToSpeech.speak(
                            "Congratulations! You have successfully completed your exercise session.",
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            "finish"
                        )
                    }
                    Handler(Looper.getMainLooper()).postDelayed({ goToFeedback() }, 1500)
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
            .setTitle("Set $setJustDone Complete!")
            .setMessage("Great job! You completed set $setJustDone of $totalSets.\n\nWould you like a 30 second break before the next set?")
            .setPositiveButton("Take a Break") { _, _ -> showBreakCountdown() }
            .setNegativeButton("Continue Now") { _, _ ->
                currentSet++
                isBreakShowing = false
                runOnUiThread {
                    tvSetCount.text = "$currentSet/$totalSets"
                    Toast.makeText(this, "Set $currentSet — Go!", Toast.LENGTH_SHORT).show()
                }
            }
            .setCancelable(false)
            .show()
    }

    // ─── Break Countdown ─────────────────────────────
    private fun showBreakCountdown() {
        var seconds = 30
        val dialog = AlertDialog.Builder(this)
            .setTitle("Rest Time")
            .setMessage("Starting set ${currentSet + 1} in $seconds seconds...")
            .setNegativeButton("Skip Break") { d, _ ->
                d.dismiss()
                currentSet++
                isBreakShowing = false
                runOnUiThread {
                    tvSetCount.text = "$currentSet/$totalSets"
                    Toast.makeText(this, "Set $currentSet — Go!", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(this@ExerciseSessionActivity, "Set $currentSet — Go!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    dialog.setMessage("Starting set ${currentSet + 1} in $seconds seconds...")
                    handler.postDelayed(this, 1000)
                }
            }
        }
        handler.postDelayed(runnable, 1000)
    }

    // ─── Load Saved Progress from Backend ─────────────
    private fun loadProgress() {

        ApiClient.apiService.resumeProgress(
            userId,
            exerciseName
        ).enqueue(object : Callback<ResumeProgressResponse> {

            override fun onResponse(
                call: Call<ResumeProgressResponse>,
                response: Response<ResumeProgressResponse>
            ) {

                if (response.isSuccessful &&
                    response.body()?.success == true &&
                    response.body()?.session != null
                ) {

                    val session = response.body()!!.session!!

                    currentSet = session.setsCompleted
                    repCount = session.repsCompleted

                    tvSetCount.text = "$currentSet/$totalSets"
                    tvRepCount.text = repCount.toString()

                    Toast.makeText(
                        this@ExerciseSessionActivity,
                        "Previous progress restored",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            }

            override fun onFailure(
                call: Call<ResumeProgressResponse>,
                t: Throwable
            ) {

                android.util.Log.e(
                    "RESUME",
                    t.message ?: "Resume failed"
                )

            }

        })

    }

    // ─── Save Progress to Backend ─────────────────────
    private fun saveProgress(status: String) {

        val accuracy = when (exerciseName) {
            "Right Arm Lateral Raise" ->
                if (armRepScores.isEmpty()) 0
                else (armRepScores.average() * 100).toInt().coerceIn(0, 100)

            "Squat" ->
                if (squatRepScores.isEmpty()) 0
                else (squatRepScores.average() * 100).toInt().coerceIn(0, 100)

            "Neck Stretch" ->
                if (neckRepScores.isEmpty()) 0
                else (neckRepScores.average() * 100).toInt().coerceIn(0, 100)

            else -> 0
        }

        val request = SaveProgressRequest(
            userId = userId,
            exerciseName = exerciseName,
            setsCompleted = currentSet,
            repsCompleted = repCount,
            accuracy = accuracy,
            feedback = "",
            status = status
        )

        ApiClient.apiService.saveProgress(request)
            .enqueue(object : Callback<ProgressResponse> {

                override fun onResponse(
                    call: Call<ProgressResponse>,
                    response: Response<ProgressResponse>
                ) {
                    android.util.Log.d("PROGRESS", "Progress saved")
                }

                override fun onFailure(
                    call: Call<ProgressResponse>,
                    t: Throwable
                ) {
                    android.util.Log.e("PROGRESS", t.message ?: "Save failed")
                }

            })
    }

    // ─── Go to Feedback ───────────────────────────────
    private fun goToFeedback() {
        val accuracy = when (exerciseName) {
            "Right Arm Lateral Raise" ->
                if (armRepScores.isEmpty()) 0 else (armRepScores.average() * 100).toInt().coerceIn(0, 100)
            "Squat" ->
                if (squatRepScores.isEmpty()) 0 else (squatRepScores.average() * 100).toInt().coerceIn(0, 100)
            "Neck Stretch" ->
                if (neckRepScores.isEmpty()) 0 else (neckRepScores.average() * 100).toInt().coerceIn(0, 100)
            else -> 0
        }

        android.util.Log.d("ACCURACY",
            "exercise=$exerciseName reps=$repCount accuracy=$accuracy% " +
                    "armScores=$armRepScores squatScores=$squatRepScores neckScores=$neckRepScores")

        val intent = Intent(this, FeedbackActivity::class.java)
        intent.putExtra("EXERCISE_NAME", exerciseName)
        intent.putExtra("ACCURACY",      accuracy)
        intent.putExtra("REPS_DONE",     repCount)
        startActivity(intent)
        finish()
    }

    // ─── Angle Smoothing (per-angle buffer, not shared) ───
    private fun smoothAngle(newAngle: Double, buffer: ArrayDeque<Double>): Double {
        if (buffer.size >= 5) buffer.removeFirst()
        buffer.addLast(newAngle)
        return buffer.average()
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
            Toast.makeText(this, "Camera permission required for pose detection", Toast.LENGTH_LONG).show()
        }
    }

    private fun speakFeedback(message: String) {

        val currentTime = System.currentTimeMillis()

        if (message != lastSpokenFeedback &&
            currentTime - lastSpeechTime > 1800) {

            lastSpokenFeedback = message
            lastSpeechTime = currentTime

            if (::textToSpeech.isInitialized) {
                textToSpeech.speak(
                    message,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "feedback"
                )
            }
        }
    }

    // ─── Cleanup ─────────────────────────────────────
    override fun onDestroy() {
        super.onDestroy()
        isStopped = true
        cameraExecutor.shutdown()

        poseLandmarker?.close()

        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }
}
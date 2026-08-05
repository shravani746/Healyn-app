package com.example.physioapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class ExerciseDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_detail)

        val exerciseName = intent.getStringExtra("EXERCISE_NAME") ?: "Exercise"
        val exerciseSets = intent.getIntExtra("EXERCISE_SETS", 3)
        val exerciseReps = intent.getIntExtra("EXERCISE_REPS", 10)

        // Bind views
        val tvExerciseName  = findViewById<TextView>(R.id.tvExerciseName)
        val tvTotalSets     = findViewById<TextView>(R.id.tvTotalSets)
        val tvTotalReps     = findViewById<TextView>(R.id.tvTotalReps)
        val tvDifficulty    = findViewById<TextView>(R.id.tvDifficulty)
        val tvDescription   = findViewById<TextView>(R.id.tvDescription)
        val btnBack         = findViewById<TextView>(R.id.btnBack)
        val btnStartSession = findViewById<Button>(R.id.btnStartSession)
        val videoView       = findViewById<VideoView>(R.id.videoView)
        val playOverlay     = findViewById<View>(R.id.playOverlay)

        // Set values
        tvExerciseName.text = exerciseName
        tvTotalSets.text    = exerciseSets.toString()
        tvTotalReps.text    = exerciseReps.toString()
        tvDifficulty.text   = getDifficulty(exerciseName)
        tvDescription.text  = getDescription(exerciseName)

        // Back button
        btnBack.setOnClickListener { finish() }

        // Video setup
        val videoRes = getVideoResource(exerciseName)
        if (videoRes != 0) {
            val uri = Uri.parse(
                "android.resource://$packageName/$videoRes"
            )
            videoView.setVideoURI(uri)

            playOverlay.setOnClickListener {
                playOverlay.visibility = View.GONE
                videoView.start()
            }

            videoView.setOnCompletionListener {
                playOverlay.visibility = View.VISIBLE
            }
        }

        // Start session — goes to camera screen
        btnStartSession.setOnClickListener {
            val intent = Intent(this, ExerciseSessionActivity::class.java)
            intent.putExtra("EXERCISE_NAME", exerciseName)
            intent.putExtra("EXERCISE_SETS", exerciseSets)
            intent.putExtra("EXERCISE_REPS", exerciseReps)
            startActivity(intent)
        }
    }

    private fun getDifficulty(exercise: String): String {
        return when (exercise) {
            "Squat"        -> "Medium"
            "Neck Stretch" -> "Easy"
            else           -> "Easy"   // Right Arm Lateral Raise still falls here — fine, "Easy" was already the default
        }
    }

    private fun getDescription(exercise: String): String {
        return when (exercise) {
            "Right Arm Lateral Raise" -> "The lateral arm raise strengthens the shoulder muscles and improves range of motion. Commonly prescribed for shoulder rehabilitation and rotator cuff recovery."
            "Squat"        -> "The squat strengthens the quadriceps, hamstrings, and glutes. It is used in knee rehabilitation and lower body strengthening programs."
            "Neck Stretch" -> "The neck stretch relieves tension in the cervical muscles and improves flexibility. It is recommended for patients with neck pain or stiffness."
            else           -> "Follow your physiotherapist's instructions for this exercise."
        }
    }

    private fun getVideoResource(exercise: String): Int {
        return when (exercise) {
            "Right Arm Lateral Raise" -> R.raw.arm_raise   // keep the actual video file name as-is, only the key changes
            "Squat"                   -> R.raw.squat
            "Neck Stretch"            -> R.raw.neck_stretch
            else                      -> 0
        }
    }
}
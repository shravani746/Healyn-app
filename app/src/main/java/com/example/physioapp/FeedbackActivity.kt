package com.example.physioapp

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class FeedbackActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback)

        val exerciseName    = intent.getStringExtra("EXERCISE_NAME") ?: "Exercise"
        val accuracy        = intent.getIntExtra("ACCURACY", 0)
        val repsDone        = intent.getIntExtra("REPS_DONE", 0)

        val tvExerciseName  = findViewById<TextView>(R.id.tvExerciseName)
        val tvAccuracy      = findViewById<TextView>(R.id.tvAccuracy)
        val tvAccuracyLabel = findViewById<TextView>(R.id.tvAccuracyLabel)
        val tvMistakes      = findViewById<TextView>(R.id.tvMistakes)
        val tvReps          = findViewById<TextView>(R.id.tvReps)
        val circularProgress = findViewById<CircularProgressView>(R.id.circularProgressBar)
        val btnTryAgain     = findViewById<Button>(R.id.btnTryAgain)
        val btnDone         = findViewById<Button>(R.id.btnDone)

        // Set text values
        tvExerciseName.text = exerciseName
        tvAccuracy.text     = "$accuracy%"
        tvReps.text         = repsDone.toString()
        tvMistakes.text     = getFeedback(exerciseName)

        // Pick color and label based on score
        val hexColor = when {
            accuracy >= 80 -> "#085041"   // dark green
            accuracy >= 50 -> "#FF9500"   // orange
            else           -> "#FF3B30"   // red
        }

        val label = when {
            accuracy >= 80 -> "Great Job!"
            accuracy >= 50 -> "Good Effort!"
            else           -> "Keep Practicing"
        }

        val color = Color.parseColor(hexColor)
        tvAccuracy.setTextColor(color)
        tvAccuracyLabel.text = label
        tvAccuracyLabel.setTextColor(color)

        // Animate the ring clockwise
        circularProgress.setProgress(accuracy.toFloat(), hexColor)

        btnTryAgain.setOnClickListener { finish() }

        btnDone.setOnClickListener {
            val intent = Intent(this, PatientDashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }
    }

    private fun getFeedback(exercise: String): String {
        return when (exercise) {
            "Arm Raise"    -> "• Arm too low at peak position\n• Keep elbow straight\n• Maintain a steady pace"
            "Squat"        -> "• Knees going past toes\n• Keep your back straight\n• Lower hips more"
            "Neck Stretch" -> "• Hold the stretch a bit longer\n• Move slowly and gently\n• Breathe steadily"
            else           -> "• Great effort!\n• Keep practicing daily\n• Consistency is key"
        }
    }
}
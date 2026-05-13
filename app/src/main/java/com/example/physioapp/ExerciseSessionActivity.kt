package com.example.physioapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ExerciseSessionActivity : AppCompatActivity() {

    private var repCount = 0

    // Dummy feedback — Arya will replace with real MediaPipe data
    private val feedbackMessages = listOf(
        "Good Posture! Keep it up 💪",
        "Raise your arm higher ⬆️",
        "Keep your back straight 🧍",
        "Slow down the movement 🔄",
        "Great form! Keep going 🎯",
        "Almost there! Push through 💥"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_session)

        val exerciseName = intent.getStringExtra("EXERCISE_NAME") ?: "Exercise"

        val tvLabel      = findViewById<TextView>(R.id.tvExerciseLabel)
        val tvFeedback   = findViewById<TextView>(R.id.tvLiveFeedback)
        val tvRepCount   = findViewById<TextView>(R.id.tvRepCount)
        val btnStop      = findViewById<Button>(R.id.btnStopSession)
        val btnFinish    = findViewById<Button>(R.id.btnFinishSession)

        tvLabel.text = exerciseName

        // Rotate dummy feedback every time user taps anywhere
        // Arya will remove this and use real-time pose feedback
        tvFeedback.text = feedbackMessages.random()

        btnStop.setOnClickListener {
            btnStop.text = "Stopped ✋"
            btnStop.isEnabled = false
            tvFeedback.text = "Session paused. Tap Finish when ready."
            tvFeedback.setTextColor(android.graphics.Color.parseColor("#FF9500"))
        }

        btnFinish.setOnClickListener {
            val intent = Intent(this, FeedbackActivity::class.java)
            intent.putExtra("EXERCISE_NAME", exerciseName)
            intent.putExtra("ACCURACY", 85)        // Dummy — Arya replaces with real score
            intent.putExtra("REPS_DONE", repCount)
            startActivity(intent)
            finish()
        }
    }
}
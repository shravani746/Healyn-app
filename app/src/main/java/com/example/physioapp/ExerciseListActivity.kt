package com.example.physioapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageView

class ExerciseListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_list)

        val btnArmRaise = findViewById<Button>(R.id.btnStartArmRaise)
        val btnSquat = findViewById<Button>(R.id.btnStartSquat)
        val btnNeckStretch = findViewById<Button>(R.id.btnStartNeckStretch)

        btnArmRaise.setOnClickListener {
            launchSession("Arm Raise", 10, 3)
        }
        btnSquat.setOnClickListener {
            launchSession("Squat", 12, 3)
        }
        btnNeckStretch.setOnClickListener {
            launchSession("Neck Stretch", 0, 2)
        }
    }

    private fun launchSession(name: String, reps: Int, sets: Int) {
        val intent = Intent(this, ExerciseDetailActivity::class.java)
        intent.putExtra("EXERCISE_NAME", name)
        intent.putExtra("EXERCISE_REPS", reps)
        intent.putExtra("EXERCISE_SETS", sets)
        startActivity(intent)
    }
}
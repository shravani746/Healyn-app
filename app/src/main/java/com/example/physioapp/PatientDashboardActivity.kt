// PatientDashboardActivity.kt
// Package: com.example.physioapp
// Author: Zehrah (Dashboard module)
// FIX: cardStartExercise and cardViewProgress changed from LinearLayout to CardView

package com.example.physioapp

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.button.MaterialButton
import java.util.Calendar

class PatientDashboardActivity : AppCompatActivity() {

    // ─── Views ───────────────────────────────────────────────────────────────
    private lateinit var tvGreeting: TextView
    private lateinit var tvPatientName: TextView
    private lateinit var cardStartExercise: CardView   // ← was LinearLayout, now CardView
    private lateinit var cardViewProgress: CardView    // ← was LinearLayout, now CardView
    private lateinit var btnLogout: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_dashboard)

        bindViews()
        setGreeting()
        setPatientName()
        setClickListeners()
    }

    // ─── Bind all views ───────────────────────────────────────────────────────
    private fun bindViews() {
        tvGreeting        = findViewById(R.id.tvGreeting)
        tvPatientName     = findViewById(R.id.tvPatientName)
        cardStartExercise = findViewById(R.id.cardStartExercise)   // ← CardView
        cardViewProgress  = findViewById(R.id.cardViewProgress)    // ← CardView
        btnLogout         = findViewById(R.id.btnLogout)
    }

    // ─── Dynamic greeting based on time of day ────────────────────────────────
    private fun setGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        tvGreeting.text = when {
            hour < 12 -> "Good morning,"
            hour < 17 -> "Good afternoon,"
            else      -> "Good evening,"
        }
    }

    // ─── Patient name from Intent extras (passed from LoginActivity) ──────────
    private fun setPatientName() {
        val name = intent.getStringExtra("EXTRA_USER_NAME") ?: "Alex Johnson"
        tvPatientName.text = name
    }

    // ─── Click listeners / Navigation ────────────────────────────────────────
    private fun setClickListeners() {

        // Start Exercise → ExerciseListActivity
        cardStartExercise.setOnClickListener {
            val intent = Intent(this, ExerciseListActivity::class.java)
            startActivity(intent)
        }

        // View Progress → ProgressActivity
        cardViewProgress.setOnClickListener {
            val intent = Intent(this, ProgressActivity::class.java)
            startActivity(intent)
        }

        // Logout
        btnLogout.setOnClickListener {
            showLogoutDialog()
        }
    }

    // ─── Logout confirmation dialog ───────────────────────────────────────────
    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Log Out")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Log Out") { _, _ ->
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ─── Prevent going back to login with hardware back button ───────────────
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        super.onBackPressed()
        showLogoutDialog()
    }
}
// AssignExerciseActivity.kt
// Package: com.example.physioapp
// Author: Zehrah (Dashboard module)

package com.example.physioapp

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class AssignExerciseActivity : AppCompatActivity() {

    // ─── Views ───────────────────────────────────────────────────────────────
    private lateinit var tilExerciseName: TextInputLayout
    private lateinit var etExerciseName: TextInputEditText
    private lateinit var tilPatient: TextInputLayout
    private lateinit var actvPatient: AutoCompleteTextView
    private lateinit var tilReps: TextInputLayout
    private lateinit var etReps: TextInputEditText
    private lateinit var tilSets: TextInputLayout
    private lateinit var etSets: TextInputEditText
    private lateinit var etNotes: TextInputEditText
    private lateinit var btnAssign: MaterialButton
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnBack: android.widget.ImageButton

    // ─── Dummy patient list (same as PhysioDashboard) ────────────────────────
    private val patientNames = listOf(
        "Alex Johnson",
        "Maria Garcia",
        "Ravi Patel",
        "Nour Al-Hassan"
    )

    // ─── Suggested exercises for quick pick (optional UX enhancement) ─────────


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assign_exercise)

        bindViews()
        setupPatientDropdown()
        setClickListeners()
    }

    // ─── Bind views ───────────────────────────────────────────────────────────
    private fun bindViews() {
        tilExerciseName = findViewById(R.id.tilExerciseName)
        etExerciseName  = findViewById(R.id.etExerciseName)
        tilPatient      = findViewById(R.id.tilPatient)
        actvPatient     = findViewById(R.id.actvPatient)
        tilReps         = findViewById(R.id.tilReps)
        etReps          = findViewById(R.id.etReps)
        tilSets         = findViewById(R.id.tilSets)
        etSets          = findViewById(R.id.etSets)
        etNotes         = findViewById(R.id.etNotes)
        btnAssign       = findViewById(R.id.btnAssign)
        btnCancel       = findViewById(R.id.btnCancel)
        btnBack         = findViewById(R.id.btnBack)
    }

    // ─── Set up the patient dropdown with dummy names ─────────────────────────
    private fun setupPatientDropdown() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, patientNames)
        actvPatient.setAdapter(adapter)
        // Show all options on click
        actvPatient.setOnClickListener { actvPatient.showDropDown() }
    }

    // ─── All click listeners ──────────────────────────────────────────────────
    private fun setClickListeners() {

        // Back arrow → closes this screen
        btnBack.setOnClickListener { finish() }

        // Cancel → same as back
        btnCancel.setOnClickListener { finish() }

        // Assign → validate then show success Toast
        btnAssign.setOnClickListener { handleAssign() }
    }

    // ─── Assignment logic ─────────────────────────────────────────────────────
    private fun handleAssign() {
        // Clear previous errors
        tilExerciseName.error = null
        tilPatient.error      = null
        tilReps.error         = null
        tilSets.error         = null

        val exerciseName = etExerciseName.text.toString().trim()
        val patient      = actvPatient.text.toString().trim()
        val reps         = etReps.text.toString().trim()
        val sets         = etSets.text.toString().trim()

        // ── Validation ──
        var isValid = true

        if (exerciseName.isEmpty()) {
            tilExerciseName.error = "Please enter an exercise name"
            isValid = false
        }
        if (patient.isEmpty()) {
            tilPatient.error = "Please select a patient"
            isValid = false
        }
        if (reps.isEmpty()) {
            tilReps.error = "Enter repetitions"
            isValid = false
        }
        if (sets.isEmpty()) {
            tilSets.error = "Enter sets"
            isValid = false
        }

        if (!isValid) return

        // ── Success path (no backend — just Toast) ──
        val message = "✓ \"$exerciseName\" assigned to $patient\n($sets sets × $reps reps)"
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

        // Optional: log to Logcat for debugging during merging
        android.util.Log.d(
            "AssignExercise",
            "Assigned: $exerciseName | Patient: $patient | Sets: $sets | Reps: $reps"
        )

        // Go back to PhysioDashboard
        finish()
    }

    /// ─── Handle hardware back button ─────────────────────────────────────────
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

} // ← this closes the class — make sure this exists
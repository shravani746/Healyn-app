// PhysioDashboardActivity.kt
// Package: com.example.physioapp
// Author: Zehrah (Dashboard module)

package com.example.physioapp

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class PhysioDashboardActivity : AppCompatActivity() {

    // ─── Views ───────────────────────────────────────────────────────────────
    private lateinit var tvPhysioName: TextView
    private lateinit var tvPatientCount: TextView
    private lateinit var rvPatients: RecyclerView
    private lateinit var btnAssignExercise: MaterialButton
    private lateinit var btnPhysioLogout: MaterialButton

    // ─── Dummy data: replace with real data later if backend is added ─────────
    private val dummyPatients = listOf(
        Patient(name = "Alex Johnson",    condition = "Lower back pain",      status = "Active"),
        Patient(name = "Maria Garcia",    condition = "Knee rehabilitation",  status = "Active"),
        Patient(name = "Ravi Patel",      condition = "Shoulder strain",      status = "Pending"),
        Patient(name = "Nour Al-Hassan",  condition = "Post-surgery recovery",status = "Active")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_physio_dashboard)

        bindViews()
        setPhysioName()
        setupPatientList()
        setClickListeners()
    }

    // ─── Bind views ───────────────────────────────────────────────────────────
    private fun bindViews() {
        tvPhysioName    = findViewById(R.id.tvPhysioName)
        tvPatientCount  = findViewById(R.id.tvPatientCount)
        rvPatients      = findViewById(R.id.rvPatients)
        btnAssignExercise = findViewById(R.id.btnAssignExercise)
        btnPhysioLogout = findViewById(R.id.btnPhysioLogout)
    }

    // ─── Physio name from Intent extras (passed from LoginActivity) ───────────
    private fun setPhysioName() {
        val name = intent.getStringExtra("EXTRA_USER_NAME") ?: "Dr. Sarah Ahmed"
        tvPhysioName.text = name
        tvPatientCount.text = "${dummyPatients.size} patients"
    }

    // ─── Set up RecyclerView with dummy patients ──────────────────────────────
    private fun setupPatientList() {
        rvPatients.layoutManager = LinearLayoutManager(this)
        rvPatients.adapter = PatientAdapter(dummyPatients)
    }

    // ─── Click listeners / Navigation ────────────────────────────────────────
    private fun setClickListeners() {

        // Assign Exercise → AssignExerciseActivity
        btnAssignExercise.setOnClickListener {
            val intent = Intent(this, AssignExerciseActivity::class.java)
            startActivity(intent)
        }

        // Logout
        btnPhysioLogout.setOnClickListener {
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

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        super.onBackPressed()
        showLogoutDialog()
    }
}
package com.example.physioapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class ProgressActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_progress)

        findViewById<Button>(R.id.btnBackToDashboard).setOnClickListener {
            startActivity(Intent(this, PatientDashboardActivity::class.java))
            finish()
        }
    }
}
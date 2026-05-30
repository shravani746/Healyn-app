package com.example.physioapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TEMPORARY TEST — remove before merging with team
        val intent = Intent(this, PhysioDashboardActivity::class.java)
        intent.putExtra("EXTRA_USER_NAME", "Dr. Sarah Ahmed")
        startActivity(intent)
        finish()
    }
}
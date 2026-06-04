package com.example.physioapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val btnLogin  = findViewById<Button>(R.id.btnLogin)
        val rgRole    = findViewById<RadioGroup>(R.id.rgRole)
        val rbPatient = findViewById<RadioButton>(R.id.rbPatient)
        val rbPhysio  = findViewById<RadioButton>(R.id.rbPhysio)
        val tvSignup  = findViewById<TextView>(R.id.tvSignup)

        btnLogin.setOnClickListener {

            // Check which role is selected
            when (rgRole.checkedRadioButtonId) {

                R.id.rbPatient -> {
                    Toast.makeText(this,
                        "Welcome, Patient!",
                        Toast.LENGTH_SHORT).show()
                    startActivity(
                        Intent(this, PatientDashboardActivity::class.java)
                    )
                    finish()
                }

                R.id.rbPhysio -> {
                    Toast.makeText(this,
                        "Welcome, Physiotherapist!",
                        Toast.LENGTH_SHORT).show()
                    startActivity(
                        Intent(this, PhysioDashboardActivity::class.java)
                    )
                    finish()
                }

                else -> {
                    // No role selected
                    Toast.makeText(this,
                        "Please select a role!",
                        Toast.LENGTH_SHORT).show()
                }
            }
        }

        tvSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }
}
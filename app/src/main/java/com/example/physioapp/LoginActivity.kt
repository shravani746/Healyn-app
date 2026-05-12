package com.example.physioapp

import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val rbPatient = findViewById<RadioButton>(R.id.rbPatient)
        val tvSignup = findViewById<TextView>(R.id.tvSignup)

        btnLogin.setOnClickListener {

            // Temporary login action
            Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
        }

        // Signup text click
        tvSignup.setOnClickListener {
            Toast.makeText(this, "Signup coming soon!", Toast.LENGTH_SHORT).show()
        }
    }
}
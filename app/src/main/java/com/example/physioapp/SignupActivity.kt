package com.example.physioapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SignupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val etName     = findViewById<EditText>(R.id.etName)
        val etEmail    = findViewById<EditText>(R.id.etEmail)
        val etPhone    = findViewById<EditText>(R.id.etPhone)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val rgRole     = findViewById<RadioGroup>(R.id.rgRole)
        val btnSignup  = findViewById<Button>(R.id.btnSignup)
        val tvLogin    = findViewById<TextView>(R.id.tvLogin)

        btnSignup.setOnClickListener {

            val name     = etName.text.toString().trim()
            val email    = etEmail.text.toString().trim()
            val phone    = etPhone.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Basic validation
            when {
                name.isEmpty() -> {
                    Toast.makeText(this,
                        "Please enter your name",
                        Toast.LENGTH_SHORT).show()
                }
                email.isEmpty() -> {
                    Toast.makeText(this,
                        "Please enter your email",
                        Toast.LENGTH_SHORT).show()
                }
                phone.isEmpty() -> {
                    Toast.makeText(this,
                        "Please enter your phone number",
                        Toast.LENGTH_SHORT).show()
                }
                password.isEmpty() -> {
                    Toast.makeText(this,
                        "Please enter a password",
                        Toast.LENGTH_SHORT).show()
                }
                password.length < 6 -> {
                    Toast.makeText(this,
                        "Password must be at least 6 characters",
                        Toast.LENGTH_SHORT).show()
                }
                rgRole.checkedRadioButtonId == -1 -> {
                    Toast.makeText(this,
                        "Please select a role",
                        Toast.LENGTH_SHORT).show()
                }
                else -> {
                    // All fields valid — show success and go back to login
                    Toast.makeText(this,
                        "Account created successfully! Please login.",
                        Toast.LENGTH_LONG).show()

                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                    finish()
                }
            }
        }

        // Already have account — go back to login
        tvLogin.setOnClickListener {
            finish() // just goes back to LoginActivity
        }
    }
}
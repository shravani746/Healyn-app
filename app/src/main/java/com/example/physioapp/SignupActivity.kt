package com.example.physioapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.physioapp.model.RegisterRequest
import com.example.physioapp.model.RegisterResponse
import com.example.physioapp.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SignupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPhone = findViewById<EditText>(R.id.etPhone)
        val etPassword = findViewById<EditText>(R.id.etPassword)

        val rgRole = findViewById<RadioGroup>(R.id.rgRole)
        val btnSignup = findViewById<Button>(R.id.btnSignup)
        val tvLogin = findViewById<TextView>(R.id.tvLogin)

        val rbPatient = findViewById<RadioButton>(R.id.rbPatient)

        btnSignup.setOnClickListener {

            val fullName = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (fullName.isEmpty()) {
                etName.error = "Enter Full Name"
                return@setOnClickListener
            }

            if (email.isEmpty()) {
                etEmail.error = "Enter Email"
                return@setOnClickListener
            }

            if (phone.isEmpty()) {
                etPhone.error = "Enter Phone Number"
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                etPassword.error = "Enter Password"
                return@setOnClickListener
            }

            if (rgRole.checkedRadioButtonId == -1) {
                Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val role = if (rbPatient.isChecked) {
                "patient"
            } else {
                "physiotherapist"
            }

            val request = RegisterRequest(
                fullName = fullName,
                email = email,
                phoneNumber = phone,
                password = password,
                role = role
            )

            ApiClient.apiService.registerUser(request)
                .enqueue(object : Callback<RegisterResponse> {

                    override fun onResponse(
                        call: Call<RegisterResponse>,
                        response: Response<RegisterResponse>
                    ) {

                        if (response.isSuccessful && response.body() != null) {

                            Toast.makeText(
                                this@SignupActivity,
                                response.body()!!.message,
                                Toast.LENGTH_LONG
                            ).show()

                            startActivity(
                                Intent(
                                    this@SignupActivity,
                                    LoginActivity::class.java
                                )
                            )
                            finish()

                        } else {

                            val errorMessage = try {
                                response.errorBody()?.string()
                            } catch (e: Exception) {
                                "Registration Failed"
                            }

                            Toast.makeText(
                                this@SignupActivity,
                                errorMessage ?: "Registration Failed",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    override fun onFailure(
                        call: Call<RegisterResponse>,
                        t: Throwable
                    ) {

                        Toast.makeText(
                            this@SignupActivity,
                            "Network Error: ${t.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                })
        }

        tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
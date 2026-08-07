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
import com.example.physioapp.model.LoginRequest
import com.example.physioapp.model.LoginResponse
import com.example.physioapp.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)

        val rgRole = findViewById<RadioGroup>(R.id.rgRole)
        val rbPatient = findViewById<RadioButton>(R.id.rbPatient)

        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvSignup = findViewById<TextView>(R.id.tvSignup)

        btnLogin.setOnClickListener {

            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty()) {
                etEmail.error = "Enter Email"
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                etPassword.error = "Enter Password"
                return@setOnClickListener
            }

            if (rgRole.checkedRadioButtonId == -1) {
                Toast.makeText(this, "Select Role", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = LoginRequest(
                email = email,
                password = password
            )

            ApiClient.apiService.loginUser(request)
                .enqueue(object : Callback<LoginResponse> {

                    override fun onResponse(
                        call: Call<LoginResponse>,
                        response: Response<LoginResponse>
                    ) {

                        if (response.isSuccessful && response.body() != null) {

                            val user = response.body()!!.user

                            // Save login details
                            val sharedPreferences =
                                getSharedPreferences("HealynApp", MODE_PRIVATE)

                            sharedPreferences.edit()
                                .putString("USER_ID", user?._id)
                                .putString("TOKEN", response.body()!!.accessToken)
                                .putString("ROLE", user?.role)
                                .apply()

                            Toast.makeText(
                                this@LoginActivity,
                                response.body()!!.message,
                                Toast.LENGTH_SHORT
                            ).show()

                            if (user?.role == "patient") {

                                startActivity(
                                    Intent(
                                        this@LoginActivity,
                                        PatientDashboardActivity::class.java
                                    )
                                )

                            } else {

                                startActivity(
                                    Intent(
                                        this@LoginActivity,
                                        PhysioDashboardActivity::class.java
                                    )
                                )
                            }

                            finish()

                        } else {

                            val error = try {
                                response.errorBody()?.string()
                            } catch (e: Exception) {
                                "Login Failed"
                            }

                            Toast.makeText(
                                this@LoginActivity,
                                error,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    override fun onFailure(
                        call: Call<LoginResponse>,
                        t: Throwable
                    ) {

                        Toast.makeText(
                            this@LoginActivity,
                            "Network Error : ${t.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                })
        }

        tvSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }
}
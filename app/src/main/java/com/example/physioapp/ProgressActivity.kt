package com.example.physioapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.physioapp.model.ProgressResponse
import com.example.physioapp.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProgressActivity : AppCompatActivity() {

    private lateinit var tvRecoveryScore: TextView
    private lateinit var tvAverageAccuracy: TextView
    private lateinit var tvCompletedExercises: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_progress)

        tvRecoveryScore = findViewById(R.id.tvRecoveryScore)
        tvAverageAccuracy = findViewById(R.id.tvAverageAccuracy)
        tvCompletedExercises = findViewById(R.id.tvCompletedExercises)

        val sharedPreferences = getSharedPreferences("HealynApp", MODE_PRIVATE)
        val userId = sharedPreferences.getString("USER_ID", "") ?: ""

        // DEBUG: Display the User ID
        Toast.makeText(
            this,
            "USER ID = $userId",
            Toast.LENGTH_LONG
        ).show()

        if (userId.isEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        loadProgress(userId)

        findViewById<Button>(R.id.btnBackToDashboard).setOnClickListener {
            startActivity(Intent(this, PatientDashboardActivity::class.java))
            finish()
        }
    }

    private fun loadProgress(userId: String) {

        ApiClient.apiService.getProgress(userId)
            .enqueue(object : Callback<ProgressResponse> {

                override fun onResponse(
                    call: Call<ProgressResponse>,
                    response: Response<ProgressResponse>
                ) {

                    if (response.isSuccessful &&
                        response.body() != null &&
                        response.body()!!.success &&
                        response.body()!!.progress != null
                    ) {

                        val progress = response.body()!!.progress!!

                        tvCompletedExercises.text =
                            progress.completedExercises.toString()

                        tvAverageAccuracy.text =
                            "${progress.averageAccuracy}%"

                        val recovery =
                            if (progress.totalExercises == 0)
                                0
                            else
                                (progress.completedExercises * 100) / progress.totalExercises

                        tvRecoveryScore.text = "$recovery%"

                    } else {

                        Toast.makeText(
                            this@ProgressActivity,
                            "No progress found",
                            Toast.LENGTH_SHORT
                        ).show()

                    }
                }

                override fun onFailure(
                    call: Call<ProgressResponse>,
                    t: Throwable
                ) {

                    Toast.makeText(
                        this@ProgressActivity,
                        "Error: ${t.message}",
                        Toast.LENGTH_LONG
                    ).show()

                }
            })
    }
}
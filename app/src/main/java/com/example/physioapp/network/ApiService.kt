package com.example.physioapp.network

import com.example.physioapp.model.ExerciseResponse
import com.example.physioapp.model.LoginRequest
import com.example.physioapp.model.LoginResponse
import com.example.physioapp.model.ProgressResponse
import com.example.physioapp.model.RegisterRequest
import com.example.physioapp.model.RegisterResponse
import com.example.physioapp.model.ResumeProgressResponse
import com.example.physioapp.model.SaveProgressRequest
import com.example.physioapp.model.StartExerciseRequest
import com.example.physioapp.model.StartExerciseResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {

    // ---------------- Register ----------------
    @POST("users/register")
    fun registerUser(
        @Body request: RegisterRequest
    ): Call<RegisterResponse>

    // ---------------- Login ----------------
    @POST("users/login")
    fun loginUser(
        @Body request: LoginRequest
    ): Call<LoginResponse>

    // ---------------- Get All Exercises ----------------
    @GET("exercises")
    fun getAllExercises(): Call<ExerciseResponse>

    // ---------------- Start Exercise ----------------
    @POST("exercises/start")
    fun startExercise(
        @Body request: StartExerciseRequest
    ): Call<StartExerciseResponse>

    // ---------------- Save Progress ----------------
    @POST("progress/save")
    fun saveProgress(
        @Body request: SaveProgressRequest
    ): Call<ProgressResponse>

    // ---------------- Resume Progress ----------------
    @GET("progress/resume")
    fun resumeProgress(
        @Query("userId") userId: String,
        @Query("exerciseName") exerciseName: String
    ): Call<ResumeProgressResponse>

    // ---------------- Dashboard Progress ----------------
    @GET("progress")
    fun getProgress(
        @Query("userId") userId: String
    ): Call<ProgressResponse>
}
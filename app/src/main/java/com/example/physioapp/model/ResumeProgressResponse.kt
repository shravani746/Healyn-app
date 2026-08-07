package com.example.physioapp.model

data class ResumeProgressResponse(
    val success: Boolean,
    val session: ProgressSession?
)

data class ProgressSession(
    val userId: String,
    val exerciseName: String,
    val setsCompleted: Int,
    val repsCompleted: Int,
    val accuracy: Int,
    val feedback: String,
    val status: String
)
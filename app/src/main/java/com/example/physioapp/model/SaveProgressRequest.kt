package com.example.physioapp.model

data class SaveProgressRequest(
    val userId: String,
    val exerciseName: String,
    val setsCompleted: Int,
    val repsCompleted: Int,
    val accuracy: Int = 0,
    val feedback: String = "",
    val status: String = "In Progress"
)
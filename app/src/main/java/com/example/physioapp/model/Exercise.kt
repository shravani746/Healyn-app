package com.example.physioapp.model

data class Exercise(
    val _id: String,
    val name: String,
    val category: String,
    val difficulty: String,
    val sets: Int,
    val reps: Int,
    val description: String,
    val image: String?,
    val instructions: List<String>
)
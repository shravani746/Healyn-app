package com.example.physioapp.model

data class ExerciseResponse(
    val success: Boolean,
    val count: Int,
    val exercises: List<Exercise>
)
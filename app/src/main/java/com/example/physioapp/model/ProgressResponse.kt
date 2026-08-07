package com.example.physioapp.model

data class ProgressResponse(
    val success: Boolean,
    val message: String? = null,
    val progress: ProgressData? = null
)

data class ProgressData(
    val totalExercises: Int,
    val completedExercises: Int,
    val pendingExercises: Int,
    val averageAccuracy: Double
)
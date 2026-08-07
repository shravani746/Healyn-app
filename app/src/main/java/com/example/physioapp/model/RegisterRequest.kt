package com.example.physioapp.model

data class RegisterRequest(
    val fullName: String,
    val email: String,
    val phoneNumber: String,
    val password: String,
    val role: String
)
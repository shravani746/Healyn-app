package com.example.physioapp.model

data class RegisterResponse(
    val message: String,
    val accessToken: String?,
    val user: UserData?
)

data class UserData(
    val _id: String,
    val fullName: String,
    val email: String,
    val phoneNumber: String,
    val role: String
)
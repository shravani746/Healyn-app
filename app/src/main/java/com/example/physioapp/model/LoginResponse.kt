package com.example.physioapp.model

data class LoginResponse(
    val message: String,
    val accessToken: String?,
    val user: UserData?
)
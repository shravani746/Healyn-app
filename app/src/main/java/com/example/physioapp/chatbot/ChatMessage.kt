package com.example.physioapp.chatbot

/**
 * Represents a single bubble in the chat thread.
 *
 * @param text the message content
 * @param isUser true if sent by the patient/user, false if sent by the bot
 */
data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

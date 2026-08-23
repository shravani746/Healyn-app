package com.example.physioapp.chatbot

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ChatbotActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var editTextMessage: EditText
    private lateinit var buttonSend: ImageButton
    private lateinit var adapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()

    // Small delay so the bot reply doesn't feel instant/robotic.
    private val botReplyDelayMs = 500L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatbot)

        recyclerView = findViewById(R.id.recyclerViewChat)
        editTextMessage = findViewById(R.id.editTextMessage)
        buttonSend = findViewById(R.id.buttonSend)

        adapter = ChatAdapter(messages)
        recyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        recyclerView.adapter = adapter

        // Greet the user as soon as the screen opens.
        addBotMessage(
            "Hi! I'm the Healyn support assistant. Ask me about exercises, " +
                "your progress, login issues, or how exercise assignment works."
        )

        buttonSend.setOnClickListener { sendMessage() }
        editTextMessage.setOnEditorActionListener { _, _, _ ->
            sendMessage()
            true
        }
    }

    private fun sendMessage() {
        val userText = editTextMessage.text.toString().trim()
        if (userText.isEmpty()) return

        adapter.addMessage(ChatMessage(userText, isUser = true))
        recyclerView.scrollToPosition(messages.size - 1)
        editTextMessage.text.clear()

        // Simulate a short "thinking" delay before the bot replies.
        Handler(Looper.getMainLooper()).postDelayed({
            val response = ChatbotEngine.getResponse(userText)
            addBotMessage(response)
        }, botReplyDelayMs)
    }

    private fun addBotMessage(text: String) {
        adapter.addMessage(ChatMessage(text, isUser = false))
        recyclerView.scrollToPosition(messages.size - 1)
    }
}

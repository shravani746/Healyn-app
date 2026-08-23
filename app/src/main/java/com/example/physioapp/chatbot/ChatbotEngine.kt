package com.example.physioapp.chatbot

/**
 * Simple rule-based (keyword matching) support chatbot for Healyn.
 *
 * No network calls or API keys needed — every response is looked up locally
 * from [rules]. To add a new topic, just add another [Rule] with the keywords
 * that should trigger it.
 *
 * Matching strategy:
 *  1. Lowercase + strip punctuation from the user's message.
 *  2. Score every rule by how many of its keywords appear in the message.
 *  3. Return the highest-scoring rule's response (ties broken by order added).
 *  4. If nothing scores above 0, return [fallbackResponse].
 */
object ChatbotEngine {

    private data class Rule(val keywords: List<String>, val response: String)

    // Order matters only for tie-breaking, so put more specific rules first.
    private val rules = listOf(
        Rule(
            listOf("hi", "hello", "hey", "good morning", "good evening"),
            "Hi! I'm the Healyn support assistant. You can ask me about " +
                "exercises, your progress, login issues, or how to reach your physiotherapist."
        ),
        Rule(
            listOf("thank", "thanks", "thank you"),
            "You're welcome! Let me know if there's anything else I can help with."
        ),
        Rule(
            listOf("bye", "goodbye", "see you"),
            "Take care, and keep up the great work on your recovery! 👋"
        ),
        Rule(
            listOf("password", "forgot password", "reset password", "can't log in", "cant log in", "login issue"),
            "To reset your password, go to the Login screen and tap \"Forgot Password?\" " +
                "below the Log In button. If you don't see that option yet, use your registered " +
                "email to sign in, or contact your physiotherapist to have your account checked."
        ),
        Rule(
            listOf("sign up", "signup", "create account", "register"),
            "To create an account, tap \"Sign Up\" on the Login screen, fill in your name, " +
                "email, phone number, and password, then choose whether you're a Patient or " +
                "Physiotherapist before tapping Create Account."
        ),
        Rule(
            listOf("start exercise", "begin exercise", "how to start"),
            "From your Home dashboard, tap \"Start Exercise\", or go to the Exercise tab and " +
                "tap \"Start Exercise\" on any assigned exercise card. You'll see instructions " +
                "first, then tap \"Start Exercise Session\" to begin the live camera-tracked session."
        ),
        Rule(
            listOf("camera", "posture", "live session", "not detecting", "not tracking", "pose"),
            "For accurate posture tracking during a live session, make sure your full upper " +
                "body (or the body part being tracked) is visible in frame, you're in good " +
                "lighting, and your phone is stable. If tracking still isn't working, try ending " +
                "the session and restarting it from the Exercise tab."
        ),
        Rule(
            listOf("accuracy", "score", "low accuracy", "improve accuracy"),
            "Your accuracy score reflects how closely your movements match the reference " +
                "form. Check the \"Areas to Improve\" list on your Session Summary screen after " +
                "each session — it gives specific corrections like posture or pacing tips."
        ),
        Rule(
            listOf("progress", "recovery score", "streak", "sessions done"),
            "You can track your recovery on the Progress tab — it shows your Recovery Score, " +
                "average accuracy, weekly activity, and your recent sessions."
        ),
        Rule(
            listOf("exercise list", "my exercises", "assigned exercise", "what exercises"),
            "Your assigned exercises are listed on the Exercise tab, each showing sets, reps " +
                "(or hold time), and a difficulty level. Tap any exercise for full instructions."
        ),
        Rule(
            listOf("physiotherapist", "therapist", "doctor", "contact my"),
            "Your physiotherapist reviews your session accuracy and progress and can update " +
                "your assigned exercises. If you need to reach them directly, check with your " +
                "clinic for their contact details, as in-app messaging isn't available yet."
        ),
        Rule(
            listOf("assign exercise", "prescribe", "add patient"),
            "Physiotherapists can assign exercises from the Home or Assign tab: tap \"+ Assign\", " +
                "fill in the exercise name, select the patient, set reps and sets, and add any " +
                "optional notes before tapping Assign Exercise."
        ),
        Rule(
            listOf("equipment", "mat", "what do i need"),
            "Most Healyn exercises are bodyweight only. Where a mat or other equipment is " +
                "listed, it's marked Optional or Required on the exercise's details screen."
        ),
        Rule(
            listOf("logout", "log out", "sign out"),
            "You can log out anytime using the \"Log Out\" button near the bottom of your " +
                "dashboard."
        ),
        Rule(
            listOf("bug", "not working", "crash", "error", "issue", "problem"),
            "Sorry you're running into trouble! Try closing and reopening the app first. " +
                "If the issue continues, please describe what happened and which screen you " +
                "were on so it can be looked into."
        ),
        Rule(
            listOf("human", "real person", "support team", "talk to someone"),
            "I can help with common questions here. For anything I can't resolve, please " +
                "reach out to your clinic or physiotherapist directly."
        )
    )

    private const val fallbackResponse =
        "I'm not sure about that yet — I can help with login, exercises, live sessions, " +
            "accuracy scores, progress tracking, and exercise assignments. Could you rephrase " +
            "your question, or try one of those topics?"

    /**
     * Returns the best-matching canned response for [userMessage].
     */
    fun getResponse(userMessage: String): String {
        val normalized = userMessage
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")

        var bestRule: Rule? = null
        var bestScore = 0

        for (rule in rules) {
            val score = rule.keywords.count { keyword -> normalized.contains(keyword) }
            if (score > bestScore) {
                bestScore = score
                bestRule = rule
            }
        }

        return bestRule?.response ?: fallbackResponse
    }
}

package com.example.physioapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val imgLogo   = findViewById<ImageView>(R.id.imgLogo)
        val tvAppName = findViewById<TextView>(R.id.tvAppName)
        val tvTagline = findViewById<TextView>(R.id.tvTagline)

        // Start all off-screen (below) and invisible
        imgLogo.translationY   = 60f
        tvAppName.translationY = 80f
        tvTagline.translationY = 60f

        imgLogo.alpha   = 0f
        tvAppName.alpha = 0f
        tvTagline.alpha = 0f

        imgLogo.visibility   = View.VISIBLE
        tvAppName.visibility = View.VISIBLE
        tvTagline.visibility = View.VISIBLE

        // ── Logo: soft fade-up spring ──
        Handler(Looper.getMainLooper()).postDelayed({
            imgLogo.animate()
                .alpha(1f)
                .setDuration(500)
                .start()

            springUp(imgLogo, stiffness = SpringForce.STIFFNESS_LOW, damping = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY)
        }, 100)

        // ── App name: slightly springier ──
        Handler(Looper.getMainLooper()).postDelayed({
            tvAppName.animate()
                .alpha(1f)
                .setDuration(500)
                .start()

            springUp(tvAppName, stiffness = SpringForce.STIFFNESS_LOW, damping = SpringForce.DAMPING_RATIO_LOW_BOUNCY)
        }, 350)

        // ── Tagline: lightest spring, floats in ──
        Handler(Looper.getMainLooper()).postDelayed({
            tvTagline.animate()
                .alpha(1f)
                .setDuration(500)
                .start()

            springUp(tvTagline, stiffness = SpringForce.STIFFNESS_VERY_LOW, damping = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY)
        }, 600)

        // ── Navigate to Login ──
        Handler(Looper.getMainLooper()).postDelayed({
            findViewById<View>(android.R.id.content)
                .animate()
                .alpha(0f)
                .setDuration(700)
                .withEndAction {
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
        }, 2800)
    }

    private fun springUp(
        view: View,
        stiffness: Float,
        damping: Float
    ) {
        SpringAnimation(view, DynamicAnimation.TRANSLATION_Y, 0f).apply {
            spring.stiffness = stiffness
            spring.dampingRatio = damping
            start()
        }
    }
}
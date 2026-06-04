package com.example.physioapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var tvAppName: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splashactivity)

        tvAppName = findViewById(R.id.tvAppName)

        animateText()
    }

    private fun animateText() {

        val text = "Healyn"
        var index = 0

        val handler = Handler(Looper.getMainLooper())

        val runnable = object : Runnable {
            override fun run() {

                if (index <= text.length) {

                    tvAppName.text = text.substring(0, index)
                    index++

                    handler.postDelayed(this, 180)

                } else {

                    // Wait 2 seconds after full text appears
                    handler.postDelayed({

                        findViewById<View>(android.R.id.content)
                            .animate()
                            .alpha(0f)
                            .setDuration(800)
                            .withEndAction {

                                startActivity(
                                    Intent(
                                        this@SplashActivity,
                                        LoginActivity::class.java
                                    )
                                )

                                finish()
                            }

                    }, 2000)
                }
            }
        }

        handler.post(runnable)
    }
}
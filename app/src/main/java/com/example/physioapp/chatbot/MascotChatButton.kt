package com.example.physioapp.chatbot

import com.example.physioapp.R

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout

/**
 * A small animated mascot (Healyn's leaf-sprout character, holding the
 * checklist pose) that sits on the dashboard and opens [ChatbotActivity]
 * when tapped. Drop it into any layout:
 *
 * ```xml
 * <com.example.physioapp.chatbot.MascotChatButton
 *     android:layout_width="wrap_content"
 *     android:layout_height="wrap_content" />
 * ```
 *
 * Uses a single official illustration (`mascot_bust_idle`), so the "liveliness"
 * comes from motion rather than pose-swapping:
 *  - gentle up/down float
 *  - a soft breathing pulse on the circle background
 *  - a periodic friendly wiggle (small rotation side to side) to catch the eye
 *
 * On tap: a quick bounce, then it opens the chatbot screen.
 */
class MascotChatButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val ivCircle: ImageView
    private val ivMascot: ImageView
    private val ivBadge: ImageView

    private val handler = Handler(Looper.getMainLooper())
    private val activeAnimators = mutableListOf<ObjectAnimator>()
    private var wiggleRunnable: Runnable? = null

    /** How often (ms) the mascot wiggles on its own. */
    var wiggleIntervalMs: Long = 5000

    init {
        LayoutInflater.from(context).inflate(R.layout.view_mascot_button, this, true)
        ivCircle = findViewById(R.id.ivMascotCircle)
        ivMascot = findViewById(R.id.ivMascotImage)
        ivBadge = findViewById(R.id.ivChatBadge)

        isClickable = true
        isFocusable = true

        setOnClickListener {
            playGreetAnimation {
                context.startActivity(Intent(context, ChatbotActivity::class.java))
            }
        }

        startIdleAnimations()
    }

    private fun startIdleAnimations() {
        loopFloat(ivMascot, "translationY", 0f, -8f, 1800)
        loopFloat(ivCircle, "scaleX", 1f, 1.06f, 1800)
        loopFloat(ivCircle, "scaleY", 1f, 1.06f, 1800)
        loopFloat(ivBadge, "alpha", 0.4f, 1f, 1400)
        scheduleNextWiggle()
    }

    private fun loopFloat(target: View, property: String, from: Float, to: Float, durationMs: Long) {
        val animator = ObjectAnimator.ofFloat(target, property, from, to, from).apply {
            duration = durationMs
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }
        activeAnimators.add(animator)
        animator.start()
    }

    private fun scheduleNextWiggle() {
        val runnable = Runnable {
            playWiggleAnimation()
            scheduleNextWiggle()
        }
        wiggleRunnable = runnable
        handler.postDelayed(runnable, wiggleIntervalMs)
    }

    /** A small friendly side-to-side rotation, like a little "hey, look here!" nudge. */
    private fun playWiggleAnimation() {
        ObjectAnimator.ofFloat(ivMascot, "rotation", 0f, -6f, 6f, -4f, 0f).apply {
            duration = 700
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    /** Quick squash-and-bounce feedback on tap, then invokes [onEnd]. */
    private fun playGreetAnimation(onEnd: () -> Unit) {
        val scaleDown = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(this@MascotChatButton, "scaleX", 1f, 0.85f),
                ObjectAnimator.ofFloat(this@MascotChatButton, "scaleY", 1f, 0.85f)
            )
            duration = 100
        }
        val scaleUp = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(this@MascotChatButton, "scaleX", 0.85f, 1f),
                ObjectAnimator.ofFloat(this@MascotChatButton, "scaleY", 0.85f, 1f)
            )
            duration = 180
            interpolator = OvershootInterpolator()
        }

        AnimatorSet().apply {
            playSequentially(scaleDown, scaleUp)
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationEnd(animation: Animator) = onEnd()
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
            start()
        }
    }

    /** Stops all loops. Called automatically on detach, but safe to call manually too. */
    fun stopAnimations() {
        activeAnimators.forEach { it.cancel() }
        activeAnimators.clear()
        wiggleRunnable?.let { handler.removeCallbacks(it) }
        wiggleRunnable = null
        ivMascot.animate().cancel()
    }

    override fun onDetachedFromWindow() {
        stopAnimations()
        super.onDetachedFromWindow()
    }
}

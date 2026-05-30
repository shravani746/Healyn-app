// PatientAdapter.kt — UPDATED v2
// Package: com.example.physioapp
// Change from v1: each patient now gets a unique avatar color (green / blue / coral)
// The status badge also changes color for "Pending" patients

package com.example.physioapp

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

data class Patient(
    val name: String,
    val condition: String,
    val status: String   // "Active" or "Pending"
)

class PatientAdapter(
    private val patients: List<Patient>
) : RecyclerView.Adapter<PatientAdapter.PatientViewHolder>() {

    // Avatar color sets: background color res + text color res
    // Cycles through green → blue → coral → green…
    private val avatarColors = listOf(
        Pair(R.color.avatar_green_bg,  R.color.avatar_green_text),
        Pair(R.color.avatar_blue_bg,   R.color.avatar_blue_text),
        Pair(R.color.avatar_coral_bg,  R.color.avatar_coral_text)
    )

    inner class PatientViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvInitials:  TextView = itemView.findViewById(R.id.tvPatientInitials)
        val tvName:      TextView = itemView.findViewById(R.id.tvPatientName)
        val tvCondition: TextView = itemView.findViewById(R.id.tvPatientCondition)
        val tvStatus:    TextView = itemView.findViewById(R.id.tvPatientStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_patient, parent, false)
        return PatientViewHolder(view)
    }

    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        val patient = patients[position]
        val context = holder.itemView.context

        // ── Initials ──────────────────────────────────────────────────────
        val initials = patient.name
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
        holder.tvInitials.text = initials

        // ── Avatar color (cycles through palette) ────────────────────────
        val colorPair = avatarColors[position % avatarColors.size]
        val bgDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(ContextCompat.getColor(context, colorPair.first))
        }
        holder.tvInitials.background = bgDrawable
        holder.tvInitials.setTextColor(ContextCompat.getColor(context, colorPair.second))

        // ── Name & condition ─────────────────────────────────────────────
        holder.tvName.text      = patient.name
        holder.tvCondition.text = patient.condition

        // ── Status badge ─────────────────────────────────────────────────
        holder.tvStatus.text = patient.status

        if (patient.status == "Pending") {
            val pendingBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 16f
                setColor(ContextCompat.getColor(context, R.color.badge_pending_bg))
            }
            holder.tvStatus.background = pendingBg
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.badge_pending_text))
        } else {
            val activeBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 16f
                setColor(ContextCompat.getColor(context, R.color.badge_active_bg))
            }
            holder.tvStatus.background = activeBg
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.badge_active_text))
        }
    }

    override fun getItemCount(): Int = patients.size
}
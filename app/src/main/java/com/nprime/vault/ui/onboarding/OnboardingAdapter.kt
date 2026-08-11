package com.nprime.vault.ui.onboarding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nprime.vault.R

class OnboardingAdapter : RecyclerView.Adapter<OnboardingAdapter.SlideVH>() {

    private val slides = listOf(
        Slide(R.drawable.ic_onboard_select, R.string.onboard_title_1, R.string.onboard_body_1),
        Slide(R.drawable.ic_onboard_lock,   R.string.onboard_title_2, R.string.onboard_body_2),
        Slide(R.drawable.ic_onboard_shield, R.string.onboard_title_3, R.string.onboard_body_3),
    )

    data class Slide(val icon: Int, val title: Int, val body: Int)

    inner class SlideVH(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.slide_icon)
        val title: TextView = view.findViewById(R.id.slide_title)
        val body: TextView  = view.findViewById(R.id.slide_body)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = SlideVH(
        LayoutInflater.from(parent.context).inflate(R.layout.item_onboarding_slide, parent, false)
    )

    override fun getItemCount() = slides.size

    override fun onBindViewHolder(holder: SlideVH, position: Int) {
        val s = slides[position]
        holder.icon.setImageResource(s.icon)
        holder.title.setText(s.title)
        holder.body.setText(s.body)
    }
}

package com.aktech.meetmindai

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.aktech.meetmindai.fragment.MeetingItem

class MeetingAdapter(
    private val meetings: List<MeetingItem>,
    private val onClick: (MeetingItem) -> Unit
) : RecyclerView.Adapter<MeetingAdapter.MeetingViewHolder>() {

    inner class MeetingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: CardView = view.findViewById(R.id.cardMeeting)
        val date: TextView = view.findViewById(R.id.textDate)
        val summary: TextView = view.findViewById(R.id.textSummary)
        val evaluation: TextView = view.findViewById(R.id.textEvaluation)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MeetingViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_meeting_card, parent, false)
        return MeetingViewHolder(v)
    }

    override fun onBindViewHolder(holder: MeetingViewHolder, position: Int) {
        val item = meetings[position]
        holder.date.text = item.date
        holder.summary.text = item.summary.ifBlank { "No summary" }
        holder.evaluation.text = item.evaluation.ifBlank { "No evaluation" }

        holder.card.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = meetings.size
}

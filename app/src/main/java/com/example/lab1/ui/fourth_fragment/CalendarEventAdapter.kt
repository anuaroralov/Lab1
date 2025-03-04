package com.example.lab1.ui.fourth_fragment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lab1.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CalendarEventsAdapter :
    ListAdapter<CalendarEvent, CalendarEventsAdapter.EventViewHolder>(EventDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.tv_event_title)
        private val dateTextView: TextView = itemView.findViewById(R.id.tv_event_date)
        private val timeTextView: TextView = itemView.findViewById(R.id.tv_event_time)
        private val locationTextView: TextView = itemView.findViewById(R.id.tv_event_location)

        fun bind(event: CalendarEvent) {
            titleTextView.text = event.title

            val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
            val formattedDate = dateFormat.format(Date(event.startTime))
            dateTextView.text = formattedDate

            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            val startTime = timeFormat.format(Date(event.startTime))
            val endTime = timeFormat.format(Date(event.endTime))
            timeTextView.text = "$startTime - $endTime"

            locationTextView.text = event.location ?: "No location"
        }
    }

    class EventDiffCallback : DiffUtil.ItemCallback<CalendarEvent>() {
        override fun areItemsTheSame(oldItem: CalendarEvent, newItem: CalendarEvent): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CalendarEvent, newItem: CalendarEvent): Boolean {
            return oldItem == newItem
        }
    }
}
package com.example.lab1.ui.fourth_fragment

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.provider.CalendarContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lab1.R
import com.example.lab1.databinding.FragmentFourthBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FourthFragment : Fragment() {

    private var _binding: FragmentFourthBinding? = null
    private val binding get() = _binding!!

    private lateinit var calendarAdapter: CalendarEventAdapter
    private val calendarEvents = mutableListOf<CalendarEvent>()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            fetchCalendarEvents()
        } else {
            Toast.makeText(
                requireContext(),
                "Calendar permission is required to show events",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFourthBinding.inflate(inflater, container, false)

        // Set up RecyclerView
        setupRecyclerView()

        // Check and request permissions
        checkCalendarPermission()

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerView() {
        calendarAdapter = CalendarEventAdapter(calendarEvents)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = calendarAdapter
        }
    }

    private fun checkCalendarPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_CALENDAR
            ) == PackageManager.PERMISSION_GRANTED -> {
                fetchCalendarEvents()
            }

            shouldShowRequestPermissionRationale(Manifest.permission.READ_CALENDAR) -> {
                Toast.makeText(
                    requireContext(),
                    "Calendar permission is needed to show your upcoming events",
                    Toast.LENGTH_LONG
                ).show()
                requestPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
            }

            else -> {
                requestPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
            }
        }
    }

    private fun fetchCalendarEvents() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvNoEvents.visibility = View.GONE

        val currentTimeMillis = System.currentTimeMillis()

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.CALENDAR_ID
        )

        val selection = "${CalendarContract.Events.DTSTART} >= ?"
        val selectionArgs = arrayOf(currentTimeMillis.toString())

        val sortOrder = "${CalendarContract.Events.DTSTART} ASC"

        var cursor: Cursor? = null

        try {
            cursor = requireContext().contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            cursor?.let {
                calendarEvents.clear()

                if (it.count > 0) {
                    val idColumnIndex = it.getColumnIndex(CalendarContract.Events._ID)
                    val titleColumnIndex = it.getColumnIndex(CalendarContract.Events.TITLE)
                    val descriptionColumnIndex =
                        it.getColumnIndex(CalendarContract.Events.DESCRIPTION)
                    val startColumnIndex = it.getColumnIndex(CalendarContract.Events.DTSTART)
                    val endColumnIndex = it.getColumnIndex(CalendarContract.Events.DTEND)
                    val locationColumnIndex =
                        it.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)

                    while (it.moveToNext()) {
                        val id = if (idColumnIndex != -1) it.getLong(idColumnIndex) else 0
                        val title = if (titleColumnIndex != -1) it.getString(titleColumnIndex)
                            ?: "No Title" else "No Title"
                        val description =
                            if (descriptionColumnIndex != -1) it.getString(descriptionColumnIndex)
                                ?: "" else ""
                        val start = if (startColumnIndex != -1) it.getLong(startColumnIndex) else 0
                        val end = if (endColumnIndex != -1) it.getLong(endColumnIndex) else 0
                        val location =
                            if (locationColumnIndex != -1) it.getString(locationColumnIndex)
                                ?: "" else ""

                        calendarEvents.add(
                            CalendarEvent(
                                id = id,
                                title = title,
                                description = description,
                                start = start,
                                end = end,
                                location = location
                            )
                        )
                    }
                }

                binding.progressBar.visibility = View.GONE

                if (calendarEvents.isEmpty()) {
                    binding.tvNoEvents.visibility = View.VISIBLE
                } else {
                    binding.tvNoEvents.visibility = View.GONE
                }

                calendarAdapter.notifyDataSetChanged()
            }
        } catch (e: Exception) {
            binding.progressBar.visibility = View.GONE
            binding.tvNoEvents.text = "Error loading events: ${e.message}"
            binding.tvNoEvents.visibility = View.VISIBLE
        } finally {
            cursor?.close()
        }
    }

    data class CalendarEvent(
        val id: Long,
        val title: String,
        val description: String,
        val start: Long,
        val end: Long,
        val location: String
    )

    inner class CalendarEventAdapter(
        private val events: List<CalendarEvent>
    ) : RecyclerView.Adapter<CalendarEventAdapter.EventViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_calendar_event, parent, false)
            return EventViewHolder(view)
        }

        override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
            val event = events[position]
            holder.bind(event)
        }

        override fun getItemCount() = events.size

        inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind(event: CalendarEvent) {
                val tvTitle = itemView.findViewById<android.widget.TextView>(R.id.tv_event_title)
                val tvDateTime =
                    itemView.findViewById<android.widget.TextView>(R.id.tv_event_datetime)
                val tvLocation =
                    itemView.findViewById<android.widget.TextView>(R.id.tv_event_location)
                val tvDescription =
                    itemView.findViewById<android.widget.TextView>(R.id.tv_event_description)

                val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

                val startDate = Date(event.start)
                val endDate = Date(event.end)

                val dateTimeText = "${dateFormat.format(startDate)}\n" +
                        "Time: ${timeFormat.format(startDate)} - ${timeFormat.format(endDate)}"

                tvTitle.text = event.title
                tvDateTime.text = dateTimeText
                tvLocation.text =
                    if (event.location.isNotEmpty()) "Location: ${event.location}" else "No location"

                if (event.description.isNotEmpty()) {
                    tvDescription.visibility = View.VISIBLE
                    tvDescription.text = event.description
                } else {
                    tvDescription.visibility = View.GONE
                }

                itemView.setOnClickListener {
                    val eventUri =
                        ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.id)
                    Toast.makeText(requireContext(), "Event ID: ${event.id}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }
}
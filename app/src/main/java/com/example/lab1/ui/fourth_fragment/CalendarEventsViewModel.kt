package com.example.lab1.ui.fourth_fragment

import android.app.Application
import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CalendarEventsViewModel(application: Application) : AndroidViewModel(application) {
    private val _events = MutableLiveData<List<CalendarEvent>>()
    val events: LiveData<List<CalendarEvent>> = _events

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    suspend fun fetchUpcomingEvents() {
        _isLoading.postValue(true)

        try {
            val eventsList = withContext(Dispatchers.IO) {
                getUpcomingEvents(getApplication<Application>().contentResolver)
            }

            Log.d("CalendarEvents", "Fetched events: ${eventsList.size}")
            _events.postValue(eventsList)
        } catch (e: Exception) {
            Log.e("CalendarEvents", "Error fetching events", e)
        } finally {
            _isLoading.postValue(false)
        }
    }

    private fun getUpcomingEvents(contentResolver: ContentResolver): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.EVENT_LOCATION
        )

        val selection = "${CalendarContract.Events.DTSTART} >= ?"
        val selectionArgs = arrayOf(System.currentTimeMillis().toString())

        val sortOrder = "${CalendarContract.Events.DTSTART} ASC"

        val uri: Uri = CalendarContract.Events.CONTENT_URI
        val cursor: Cursor? = contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        cursor?.use {
            val idIndex = it.getColumnIndex(CalendarContract.Events._ID)
            val titleIndex = it.getColumnIndex(CalendarContract.Events.TITLE)
            val descriptionIndex = it.getColumnIndex(CalendarContract.Events.DESCRIPTION)
            val startTimeIndex = it.getColumnIndex(CalendarContract.Events.DTSTART)
            val endTimeIndex = it.getColumnIndex(CalendarContract.Events.DTEND)
            val locationIndex = it.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)

            while (it.moveToNext()) {
                val event = CalendarEvent(
                    id = it.getLong(idIndex),
                    title = it.getString(titleIndex) ?: "Untitled Event",
                    description = it.getString(descriptionIndex),
                    startTime = it.getLong(startTimeIndex),
                    endTime = it.getLong(endTimeIndex),
                    location = it.getString(locationIndex)
                )
                events.add(event)
            }
        }

        return events
    }
}
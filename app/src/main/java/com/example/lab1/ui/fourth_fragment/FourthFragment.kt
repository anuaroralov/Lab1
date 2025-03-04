package com.example.lab1.ui.fourth_fragment

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lab1.databinding.FragmentFourthBinding
import kotlinx.coroutines.launch

class FourthFragment : Fragment() {
    private var _binding: FragmentFourthBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CalendarEventsViewModel by viewModels()
    private lateinit var eventsAdapter: CalendarEventsAdapter

    companion object {
        private const val CALENDAR_PERMISSION_REQUEST_CODE = 100
        private val CALENDAR_PERMISSIONS = arrayOf(
            Manifest.permission.READ_CALENDAR
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFourthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()
        checkCalendarPermissions()
    }

    private fun setupRecyclerView() {
        eventsAdapter = CalendarEventsAdapter()
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = eventsAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.events.observe(viewLifecycleOwner) { events ->
            binding.tvNoEvents.visibility = if (events.isEmpty()) View.VISIBLE else View.GONE
            eventsAdapter.submitList(events)
        }
    }

    private fun checkCalendarPermissions() {
        if (hasCalendarPermissions()) {
            fetchEvents()
        } else {
            requestPermissions(CALENDAR_PERMISSIONS, CALENDAR_PERMISSION_REQUEST_CODE)
        }
    }

    private fun hasCalendarPermissions(): Boolean {
        return CALENDAR_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun fetchEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.fetchUpcomingEvents()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CALENDAR_PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                fetchEvents()
            } else {
                binding.tvNoEvents.text = "Calendar access denied"
                binding.tvNoEvents.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
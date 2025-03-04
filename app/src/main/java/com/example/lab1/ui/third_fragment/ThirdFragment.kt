package com.example.lab1.ui.third_fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.lab1.databinding.FragmentThirdBinding


class ThirdFragment : Fragment() {

    private var _binding: FragmentThirdBinding? = null
    private val binding get() = _binding!!

    private var airplaneModeReceiver: BroadcastReceiver? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentThirdBinding.inflate(inflater, container, false)

        updateAirplaneModeStatus()

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        registerAirplaneModeReceiver()
    }

    override fun onPause() {
        super.onPause()
        unregisterAirplaneModeReceiver()
    }

    private fun registerAirplaneModeReceiver() {
        if (airplaneModeReceiver == null) {
            airplaneModeReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == Intent.ACTION_AIRPLANE_MODE_CHANGED) {
                        updateAirplaneModeStatus()
                    }
                }
            }
        }

        context?.registerReceiver(
            airplaneModeReceiver,
            IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED)
        )
    }

    private fun unregisterAirplaneModeReceiver() {
        airplaneModeReceiver?.let {
            context?.unregisterReceiver(it)
        }
    }

    private fun updateAirplaneModeStatus() {
        val isAirplaneModeOn = Settings.Global.getInt(
            requireContext().contentResolver,
            Settings.Global.AIRPLANE_MODE_ON, 0
        ) != 0

        val statusMessage = if (isAirplaneModeOn) {
            "Airplane Mode: ON"
        } else {
            "Airplane Mode: OFF"
        }

        binding.statusText.text = statusMessage

        Toast.makeText(context, statusMessage, Toast.LENGTH_SHORT).show()
    }
}
package com.example.lab1.ui.second_fragment

import android.Manifest
import android.R
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.lab1.databinding.FragmentSecondBinding

class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null
    private val binding get() = _binding!!

    private var musicService: MusicPlayerService? = null
    private var bound = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startMusicService()
        } else {
            Toast.makeText(context, "Permission not granted", Toast.LENGTH_SHORT).show()
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicPlayerService.MusicBinder
            musicService = binder.getService()
            bound = true
            updateUI()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
            bound = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnPlayPause.setOnClickListener {
            if (musicService == null) {
                requestNotificationPermissionAndStartService()
            } else {
                togglePlayPause()
            }
        }

        binding.btnStop.setOnClickListener {
            musicService?.stop()

            // Reset UI elements
            binding.btnPlayPause.setImageResource(R.drawable.ic_media_play)
            binding.tvCurrentTrack.text = "Stopped"

            // Optional: Clear the track list if needed
            val emptyAdapter = ArrayAdapter<String>(requireContext(), R.layout.simple_list_item_1, emptyList())
            binding.lvTrackList.adapter = emptyAdapter
        }

        binding.btnNext.setOnClickListener {
            musicService?.next()
            updateUI()
        }

        binding.btnPrevious.setOnClickListener {
            musicService?.previous()
            updateUI()
        }

        binding.lvTrackList.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                musicService?.playTrack(position)
                updateUI()
            }
    }

    private fun requestNotificationPermissionAndStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    startMusicService()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            startMusicService()
        }
    }

    private fun togglePlayPause() {
        musicService?.let {
            if (it.isPlaying()) {
                it.pause()
            } else {
                it.play()
            }
            updateUI()
        }
    }

    private fun startMusicService() {
        val intent = Intent(requireContext(), MusicPlayerService::class.java)
        requireActivity().startService(intent)

        if (!bound) {
            requireActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun updateUI() {
        musicService?.let { service ->
            val isPlaying = service.isPlaying()

            binding.btnPlayPause.setImageResource(
                if (isPlaying) R.drawable.ic_media_play else R.drawable.ic_media_pause
            )

            service.getCurrentTrackName().let {
                binding.tvCurrentTrack.text = "Now Playing: $it"
            }

            service.getMusicList().let { tracks ->
                val adapter = ArrayAdapter(requireContext(), R.layout.simple_list_item_1, tracks)
                binding.lvTrackList.adapter = adapter
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(requireContext(), MusicPlayerService::class.java)
        requireActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onResume() {
        super.onResume()
        if (bound) {
            updateUI()
        }
    }

    override fun onStop() {
        super.onStop()
        if (bound) {
            requireActivity().unbindService(serviceConnection)
            bound = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
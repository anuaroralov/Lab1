package com.example.lab1.ui.second_fragment

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class MusicPlayerService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private val NOTIFICATION_CHANNEL_ID = "music_player_channel"
    private val NOTIFICATION_ID = 101
    private val PLAY_ACTION = "PLAY_ACTION"
    private val PAUSE_ACTION = "PAUSE_ACTION"
    private val STOP_ACTION = "STOP_ACTION"
    private val NEXT_ACTION = "NEXT_ACTION"
    private val PREV_ACTION = "PREV_ACTION"

    private var musicList = ArrayList<String>()
    private var currentTrackIndex = 0
    private var currentTrackName = ""
    private var isStopped = false

    private val binder = MusicBinder()

    inner class MusicBinder : Binder() {
        fun getService(): MusicPlayerService = this@MusicPlayerService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        loadMusicList()
        initializeMediaPlayer()
    }

    private fun loadMusicList() {
        try {
            val assetManager = applicationContext.assets
            val musicFiles = assetManager.list("music") ?: emptyArray()

            musicList.clear()
            for (file in musicFiles) {
                if (file.endsWith(".mp3")) {
                    musicList.add(file)
                }
            }

            if (musicList.isEmpty()) {
                musicList.add("no_music_found.mp3")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            musicList.clear()
            musicList.add("music.mp3")
        }
    }

    private fun initializeMediaPlayer() {
        releaseMediaPlayer()

        if (musicList.isEmpty()) return

        currentTrackName = musicList[currentTrackIndex]

        mediaPlayer = MediaPlayer().apply {
            try {
                val assetPath = if (currentTrackName == "no_music_found.mp3") {
                    "music.mp3"
                } else {
                    "music/${currentTrackName}"
                }

                val descriptor = try {
                    applicationContext.assets.openFd(assetPath)
                } catch (e: Exception) {
                    applicationContext.assets.openFd("music.mp3")
                }

                setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
                prepare()
                setOnCompletionListener {
                    playNextTrack()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Music Player Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Channel for Music Player"
                enableLights(true)
                lightColor = Color.RED
            }

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            PLAY_ACTION -> {
                isStopped = false
                playMusic()
            }

            PAUSE_ACTION -> pauseMusic()
            STOP_ACTION -> stopService()
            NEXT_ACTION -> playNextTrack()
            PREV_ACTION -> playPreviousTrack()
        }

        updateNotification()
        return START_NOT_STICKY
    }

    private fun playMusic() {
        if (mediaPlayer?.isPlaying == false && !isStopped) {
            mediaPlayer?.start()
        }
    }

    private fun pauseMusic() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
        }
    }

    private fun playNextTrack() {
        val wasPlaying = mediaPlayer?.isPlaying ?: false

        currentTrackIndex = (currentTrackIndex + 1) % musicList.size
        initializeMediaPlayer()

        if (wasPlaying) {
            playMusic()
        }

        updateNotification()
    }

    private fun playPreviousTrack() {
        val wasPlaying = mediaPlayer?.isPlaying ?: false

        currentTrackIndex = if (currentTrackIndex > 0) currentTrackIndex - 1 else musicList.size - 1
        initializeMediaPlayer()

        if (wasPlaying) {
            playMusic()
        }

        updateNotification()
    }

    private fun stopService() {
        isStopped = true
        mediaPlayer?.apply {
            if (isPlaying) {
                pause()
                seekTo(0)
            }
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)

        stopForeground(true)
        stopSelf()
    }

    private fun updateNotification() {
        val notification = createNotification()
        if (mediaPlayer?.isPlaying == true) {
            startForeground(NOTIFICATION_ID, notification)
        } else {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotification(): Notification {
        val playIntent = Intent(this, MusicPlayerService::class.java).apply {
            action = PLAY_ACTION
        }
        val pauseIntent = Intent(this, MusicPlayerService::class.java).apply {
            action = PAUSE_ACTION
        }
        val stopIntent = Intent(this, MusicPlayerService::class.java).apply {
            action = STOP_ACTION
        }
        val nextIntent = Intent(this, MusicPlayerService::class.java).apply {
            action = NEXT_ACTION
        }
        val prevIntent = Intent(this, MusicPlayerService::class.java).apply {
            action = PREV_ACTION
        }

        val playPendingIntent = PendingIntent.getService(this, 0, playIntent, PendingIntent.FLAG_IMMUTABLE)
        val pausePendingIntent = PendingIntent.getService(this, 0, pauseIntent, PendingIntent.FLAG_IMMUTABLE)
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val nextPendingIntent = PendingIntent.getService(this, 0, nextIntent, PendingIntent.FLAG_IMMUTABLE)
        val prevPendingIntent = PendingIntent.getService(this, 0, prevIntent, PendingIntent.FLAG_IMMUTABLE)

        val displayName = currentTrackName.replace(".mp3", "")

        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Now Playing")
            .setContentText(displayName)
            .setSubText(if (mediaPlayer?.isPlaying == true) "Playing" else "Paused")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setDeleteIntent(stopPendingIntent)

        notificationBuilder.addAction(
            NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_previous,
                "Previous",
                prevPendingIntent
            ).build()
        )

        if (mediaPlayer?.isPlaying == true) {
            notificationBuilder.addAction(
                NotificationCompat.Action.Builder(
                    android.R.drawable.ic_media_pause,
                    "Pause",
                    pausePendingIntent
                ).build()
            )
        } else {
            notificationBuilder.addAction(
                NotificationCompat.Action.Builder(
                    android.R.drawable.ic_media_play,
                    "Play",
                    playPendingIntent
                ).build()
            )
        }

        notificationBuilder.addAction(
            NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_next,
                "Next",
                nextPendingIntent
            ).build()
        )

        notificationBuilder.addAction(
            NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                stopPendingIntent
            ).build()
        )

        return notificationBuilder.build()
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onDestroy() {
        releaseMediaPlayer()
        super.onDestroy()
    }

    fun play() {
        isStopped = false
        playMusic()
        updateNotification()
    }

    fun pause() {
        pauseMusic()
        updateNotification()
    }

    fun stop() {
        stopService()
    }

    fun next() {
        playNextTrack()
    }

    fun previous() {
        playPreviousTrack()
    }

    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying == true && !isStopped
    }

    fun getCurrentTrackName(): String {
        return currentTrackName.replace(".mp3", "")
    }

    fun getMusicList(): List<String> {
        return musicList.map { it.replace(".mp3", "") }
    }

    fun playTrack(index: Int) {
        if (index in 0 until musicList.size) {
            currentTrackIndex = index
            val wasPlaying = mediaPlayer?.isPlaying ?: false
            initializeMediaPlayer()
            if (wasPlaying) {
                playMusic()
            }
            updateNotification()
        }
    }
}
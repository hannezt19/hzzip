package com.yohanes.filereader.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import android.support.v4.media.session.MediaSessionCompat
import com.yohanes.filereader.MainActivity

/**
 * Foreground service supaya kontrol TTS (play/pause/mundur/maju) muncul
 * di notifikasi & lock screen, ala Spotify. Tombol di notifikasi kirim
 * Intent balik ke Service ini (bukan lewat MediaButtonReceiver terpisah,
 * biar tidak perlu tambahan broadcast receiver di manifest), lalu Service
 * meneruskannya ke TtsPlaybackBridge yang didengarkan PdfViewerScreen.
 */
class TtsPlaybackService : Service() {

    private var mediaSession: MediaSessionCompat? = null
    private val channelId = "tts_playback_channel"
    private val notificationId = 1001

    companion object {
        const val ACTION_PLAY_PAUSE = "com.yohanes.filereader.ACTION_PLAY_PAUSE"
        const val ACTION_SKIP_NEXT = "com.yohanes.filereader.ACTION_SKIP_NEXT"
        const val ACTION_SKIP_PREV = "com.yohanes.filereader.ACTION_SKIP_PREV"
        const val ACTION_STOP = "com.yohanes.filereader.ACTION_STOP"
    }

    override fun onCreate() {
        super.onCreate()
        val session = MediaSessionCompat(this, "TtsPlaybackService")
        session.isActive = true
        mediaSession = session
        TtsPlaybackBridge.session = session
        TtsPlaybackBridge.serviceInstance = this
        createChannel()
        startForeground(notificationId, buildNotification(isPlaying = true, title = "Membaca dokumen"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> TtsPlaybackBridge.onPlayPause?.invoke()
            ACTION_SKIP_NEXT -> TtsPlaybackBridge.onSkipNext?.invoke()
            ACTION_SKIP_PREV -> TtsPlaybackBridge.onSkipPrev?.invoke()
            ACTION_STOP -> TtsPlaybackBridge.onStop?.invoke()
            else -> startForeground(notificationId, buildNotification(isPlaying = true, title = "Membaca dokumen"))
        }
        return START_NOT_STICKY
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(channelId, "Pembacaan Teks (TTS)", NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }
    }

    private fun actionPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, TtsPlaybackService::class.java).setAction(action)
        return PendingIntent.getService(
            this, action.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun buildNotification(isPlaying: Boolean, title: String): android.app.Notification {
        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val contentIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(title)
            .setContentText("File Reader - Pembacaan Teks")
            .addAction(android.R.drawable.ic_media_previous, "Mundur", actionPendingIntent(ACTION_SKIP_PREV))
            .addAction(playPauseIcon, "Play/Pause", actionPendingIntent(ACTION_PLAY_PAUSE))
            .addAction(android.R.drawable.ic_media_next, "Maju", actionPendingIntent(ACTION_SKIP_NEXT))
            .setContentIntent(contentIntent)
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setOngoing(isPlaying)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun refreshNotification(isPlaying: Boolean, title: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(notificationId, buildNotification(isPlaying, title))
    }

    override fun onDestroy() {
        mediaSession?.isActive = false
        mediaSession?.release()
        TtsPlaybackBridge.session = null
        TtsPlaybackBridge.serviceInstance = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

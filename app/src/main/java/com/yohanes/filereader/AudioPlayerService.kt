package com.yohanes.filereader

import android.content.Intent
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.io.File

private val CMD_STOP = SessionCommand("com.yohanes.filereader.STOP", android.os.Bundle.EMPTY)

// Provider notifikasi custom: dasarnya tetap DefaultMediaNotificationProvider (jadi tombol
// prev/play-pause/next bawaan tetap ada apa adanya), cuma di-override bagian addNotificationActions
// supaya notifikasi dipaksa "ongoing" (tidak bisa di-swipe/hilang) kapan pun - baik playing maupun
// paused. createNotification tidak bisa dioverride langsung (final di versi Media3 ini), jadi
// setOngoing dipasang lewat method addNotificationActions yang memang disediakan untuk dikustomisasi.
@UnstableApi
class CustomMediaNotificationProvider(context: android.content.Context) :
    DefaultMediaNotificationProvider(context) {
    override fun addNotificationActions(
        mediaSession: MediaSession,
        mediaButtons: ImmutableList<CommandButton>,
        builder: androidx.core.app.NotificationCompat.Builder,
        actionFactory: MediaNotification.ActionFactory
    ): IntArray {
        val indices = super.addNotificationActions(mediaSession, mediaButtons, builder, actionFactory)
        builder.setOngoing(true)
        return indices
    }
}

@UnstableApi
class AudioPlayerService : androidx.media3.session.MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        setMediaNotificationProvider(CustomMediaNotificationProvider(this))

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus= */ true)
            .build()
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val path = mediaItem?.mediaId ?: return
                val openIntent = Intent(this@AudioPlayerService, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    data = Uri.fromFile(File(path))
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                val pendingIntent = android.app.PendingIntent.getActivity(
                    this@AudioPlayerService, 0, openIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                mediaSession?.setSessionActivity(pendingIntent)
            }
        })

        val callback = object : MediaSession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS
                    .buildUpon()
                    .add(CMD_STOP)
                    .build()
                return MediaSession.ConnectionResult.accept(
                    sessionCommands,
                    MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS
                )
            }

            override fun onPostConnect(session: MediaSession, controller: MediaSession.ControllerInfo) {
                session.setCustomLayout(
                    ImmutableList.of(
                        CommandButton.Builder()
                            .setDisplayName("Tutup")
                            .setSessionCommand(CMD_STOP)
                            .setIconResId(R.drawable.ic_stop_notification)
                            .build()
                    )
                )
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: android.os.Bundle
            ): ListenableFuture<SessionResult> {
                if (customCommand.customAction == CMD_STOP.customAction) {
                    session.player.stop()
                    stopSelf()
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
        }

        mediaSession = MediaSession.Builder(this, player).setCallback(callback).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}

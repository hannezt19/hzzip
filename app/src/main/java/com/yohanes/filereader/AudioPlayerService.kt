package com.yohanes.filereader

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
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

@UnstableApi
class CustomMediaNotificationProvider(context: android.content.Context) :
    DefaultMediaNotificationProvider(context) {
    override fun createNotification(
        mediaSession: MediaSession,
        customLayout: ImmutableList<CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        onNotificationChangedCallback: MediaNotification.Provider.Callback
    ): MediaNotification {
        val result = super.createNotification(mediaSession, customLayout, actionFactory, onNotificationChangedCallback)
        // Notifikasi dibuat non-dismissable (tidak bisa di-swipe) kapan pun - baik playing maupun paused.
        // Satu-satunya cara menutup adalah lewat tombol X (custom command STOP di bawah).
        result.notification.flags = result.notification.flags or Notification.FLAG_ONGOING_EVENT
        return result
    }
}

@UnstableApi
class AudioPlayerService : androidx.media3.session.MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        setMediaNotificationProvider(CustomMediaNotificationProvider(this))

        val player = ExoPlayer.Builder(this).build()
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val path = mediaItem?.mediaId ?: return
                val openIntent = Intent(this@AudioPlayerService, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    data = Uri.fromFile(File(path))
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    this@AudioPlayerService, 0, openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
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

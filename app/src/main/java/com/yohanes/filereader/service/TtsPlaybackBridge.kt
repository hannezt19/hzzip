package com.yohanes.filereader.service

import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat

/**
 * Jembatan antara TtsPlaybackService (notifikasi/lock screen) dan
 * PdfViewerScreen (logic TTS sebenarnya ada di sana). Service tidak
 * langsung memutar TTS - dia cuma memanggil callback ini saat tombol
 * di notifikasi/lock screen ditekan.
 */
object TtsPlaybackBridge {
    var onPlayPause: (() -> Unit)? = null
    var onSkipNext: (() -> Unit)? = null
    var onSkipPrev: (() -> Unit)? = null
    var onStop: (() -> Unit)? = null

    var session: MediaSessionCompat? = null
    var serviceInstance: TtsPlaybackService? = null

    fun updateState(isPlaying: Boolean, title: String) {
        session?.let { s ->
            val state = PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_STOP
                )
                .setState(
                    if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                    PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                    1f
                )
                .build()
            s.setPlaybackState(state)
            val metadata = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "File Reader")
                .build()
            s.setMetadata(metadata)
        }
        serviceInstance?.refreshNotification(isPlaying, title)
    }
}

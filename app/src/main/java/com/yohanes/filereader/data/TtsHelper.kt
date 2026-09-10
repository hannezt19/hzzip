package com.yohanes.filereader.data

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID

object TtsHelper {
    private var tts: TextToSpeech? = null
    private var onUtteranceDone: ((String) -> Unit)? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null

    // Diisi dari luar (PdfViewerScreen) supaya UI (tombol play/pause) ikut update
    // begitu Audio Focus direbut app lain (mis. Musicolet mulai main).
    var onAudioFocusLost: (() -> Unit)? = null

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                tts?.stop()
                onAudioFocusLost?.invoke()
            }
        }
    }

    fun ensureInit(context: Context, onDone: (String) -> Unit) {
        onUtteranceDone = onDone
        if (audioManager == null) {
            audioManager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        }
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("id", "ID")
            }
        }
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                utteranceId?.let { id -> onUtteranceDone?.invoke(id) }
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {}
        })
    }

    private fun requestAudioFocus(): Boolean {
        val am = audioManager ?: return false
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attrs)
            .setOnAudioFocusChangeListener(focusChangeListener)
            .build()
        focusRequest = request
        return am.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonAudioFocus() {
        val am = audioManager ?: return
        focusRequest?.let { am.abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    fun speak(text: String, volume: Float, pitch: Float, speed: Float): String {
        requestAudioFocus()
        val utteranceId = UUID.randomUUID().toString()
        tts?.setPitch(pitch)
        tts?.setSpeechRate(speed)
        val params = Bundle()
        params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        return utteranceId
    }

    fun stop() {
        tts?.stop()
        abandonAudioFocus()
    }
}

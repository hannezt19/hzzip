package com.yohanes.filereader.data

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID

object TtsHelper {
    private var tts: TextToSpeech? = null
    private var onUtteranceDone: ((String) -> Unit)? = null

    fun ensureInit(context: Context, onDone: (String) -> Unit) {
        onUtteranceDone = onDone
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

    fun speak(text: String, volume: Float, pitch: Float, speed: Float): String {
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
    }
}

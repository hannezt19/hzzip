package com.yohanes.filereader.data

import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.common.model.DownloadConditions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed class ModelDownloadState {
    object Idle : ModelDownloadState()
    object Downloading : ModelDownloadState()
    object Done : ModelDownloadState()
    data class Error(val message: String) : ModelDownloadState()
}

object TranslateHelper {

    private val translatorCache = mutableMapOf<String, Translator>()

    // Deteksi bahasa sumber dari cuplikan teks
    suspend fun detectLanguage(text: String): String? = suspendCancellableCoroutine { cont ->
        val languageIdentifier = LanguageIdentification.getClient()
        languageIdentifier.identifyLanguage(text)
            .addOnSuccessListener { languageCode ->
                if (languageCode == "und") cont.resume(null) else cont.resume(languageCode)
            }
            .addOnFailureListener { cont.resume(null) }
    }

    // Ambil/siapkan translator untuk 1 bahasa sumber
    private fun getOrCreateTranslator(sourceLangCode: String): Translator? {
        val mlkitSourceLang = TranslateLanguage.fromLanguageTag(sourceLangCode) ?: return null
        translatorCache[mlkitSourceLang]?.let { return it }

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(mlkitSourceLang)
            .setTargetLanguage(TranslateLanguage.INDONESIAN)
            .build()
        val translator = Translation.getClient(options)
        translatorCache[mlkitSourceLang] = translator
        return translator
    }

    // Pastikan model bahasa sudah terunduh, laporkan progress lewat callback
    suspend fun ensureModelDownloaded(
        sourceLangCode: String,
        onStateChange: (ModelDownloadState) -> Unit
    ): Translator? {
        val translator = getOrCreateTranslator(sourceLangCode) ?: run {
            onStateChange(ModelDownloadState.Error("Bahasa tidak didukung"))
            return null
        }

        onStateChange(ModelDownloadState.Downloading)
        val conditions = DownloadConditions.Builder().build() // izinkan unduh lewat wifi/data

        return suspendCancellableCoroutine { cont ->
            translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener {
                    onStateChange(ModelDownloadState.Done)
                    cont.resume(translator)
                }
                .addOnFailureListener { e ->
                    onStateChange(ModelDownloadState.Error(e.message ?: "Gagal unduh model bahasa"))
                    cont.resume(null)
                }
        }
    }

    // Translate teks pakai translator yang sudah siap
    suspend fun translate(translator: Translator, text: String): String =
        suspendCancellableCoroutine { cont ->
            translator.translate(text)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
}

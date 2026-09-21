package pl.lab512.puls512.speech

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import pl.lab512.puls512.data.Article
import pl.lab512.puls512.data.NarratorProfile
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

object PolishSpeech {
    private val polish = Locale("pl", "PL")

    fun configure(engine: TextToSpeech, profile: NarratorProfile): Boolean {
        val languageResult = engine.setLanguage(polish)
        val voices = engine.voices.orEmpty()
            .filter { it.locale.language == polish.language }
            .sortedWith(
                compareBy<android.speech.tts.Voice> { it.isNetworkConnectionRequired }
                    .thenByDescending { it.quality }
                    .thenBy { it.name }
            )
        if (voices.isNotEmpty()) engine.voice = voices[profile.voiceSlot % voices.size]
        engine.setPitch(profile.pitch)
        engine.setSpeechRate(profile.rate)
        engine.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        return languageResult != TextToSpeech.LANG_MISSING_DATA && languageResult != TextToSpeech.LANG_NOT_SUPPORTED
    }

    fun speak(engine: TextToSpeech, text: String, profile: NarratorProfile, utterancePrefix: String): Boolean {
        if (!configure(engine, profile)) return false
        val chunks = split(text)
        chunks.forEachIndexed { index, chunk ->
            engine.speak(
                chunk,
                if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
                null,
                "${utterancePrefix}_$index"
            )
        }
        return chunks.isNotEmpty()
    }

    fun preview(context: Context, profile: NarratorProfile) {
        lateinit var engine: TextToSpeech
        engine = TextToSpeech(context.applicationContext) { status ->
            if (status != TextToSpeech.SUCCESS || !configure(engine, profile)) {
                engine.shutdown()
            } else {
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit
                    override fun onDone(utteranceId: String?) = engine.shutdown()
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) = engine.shutdown()
                })
                engine.speak(
                    "Dzień dobry. Tu ${profile.displayName}. Oto próbka polskiego głosu w aplikacji PULS 512.",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "puls512_preview"
                )
            }
        }
    }

    fun speakBlocking(context: Context, text: String, profile: NarratorProfile): Boolean {
        val ready = CountDownLatch(1)
        val status = AtomicInteger(TextToSpeech.ERROR)
        lateinit var engine: TextToSpeech
        engine = TextToSpeech(context.applicationContext) { result ->
            status.set(result)
            ready.countDown()
        }
        if (!ready.await(12, TimeUnit.SECONDS) || status.get() != TextToSpeech.SUCCESS || !configure(engine, profile)) {
            engine.shutdown()
            return false
        }

        val chunks = split(text)
        if (chunks.isEmpty()) {
            engine.shutdown()
            return false
        }
        val finished = CountDownLatch(chunks.size)
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) = finished.countDown()
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = finished.countDown()
        })
        chunks.forEachIndexed { index, chunk ->
            engine.speak(
                chunk,
                if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
                null,
                "puls512_auto_$index"
            )
        }
        val completed = finished.await(9, TimeUnit.MINUTES)
        engine.stop()
        engine.shutdown()
        return completed
    }

    fun briefingText(articles: List<Article>): String = buildString {
        append("Oto najważniejsze, sprawdzone wiadomości przygotowane przez PULS 512. ")
        articles.forEachIndexed { index, article ->
            append("Wiadomość ${index + 1}. Status: ${article.verificationStatus.label}. ")
            append("${article.title}. ${article.summary}. ${article.whyItMatters}. ")
            if (article.sources.isNotEmpty()) append("Źródło: ${article.sources.joinToString(", ") { it.name }}. ")
        }
        append("To wszystko w tym briefingu. Oryginalne linki do artykułów znajdziesz w aplikacji.")
    }

    private fun split(text: String): List<String> {
        val max = TextToSpeech.getMaxSpeechInputLength().coerceAtMost(3_500)
        val sentences = text.trim().split(Regex("(?<=[.!?])\\s+"))
        val result = mutableListOf<String>()
        var current = StringBuilder()
        sentences.forEach { sentence ->
            if (current.isNotEmpty() && current.length + sentence.length + 1 > max) {
                result += current.toString()
                current = StringBuilder()
            }
            if (sentence.length > max) {
                if (current.isNotEmpty()) {
                    result += current.toString()
                    current = StringBuilder()
                }
                sentence.chunked(max).forEach(result::add)
            } else {
                if (current.isNotEmpty()) current.append(' ')
                current.append(sentence)
            }
        }
        if (current.isNotEmpty()) result += current.toString()
        return result.filter { it.isNotBlank() }
    }
}

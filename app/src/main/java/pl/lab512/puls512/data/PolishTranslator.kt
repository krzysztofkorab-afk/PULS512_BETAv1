package pl.lab512.puls512.data

import com.google.android.gms.tasks.Tasks
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.concurrent.TimeUnit

/**
 * Tłumaczy treść na urządzeniu. Oryginalny tekst i adresy źródeł pozostają
 * nietknięte, dzięki czemu użytkownik zawsze może porównać tłumaczenie z portalem.
 */
class PolishTranslator {
    private val identifier = LanguageIdentification.getClient()

    fun translateAll(articles: List<Article>): List<Article> = articles.map { article ->
        runCatching { translate(article) }.getOrDefault(article.withOriginals())
    }.also { identifier.close() }

    private fun translate(article: Article): Article {
        val prepared = article.withOriginals()
        val sample = "${prepared.originalTitle}. ${prepared.originalSummary}".take(1200)
        val languageTag = Tasks.await(identifier.identifyLanguage(sample), 10, TimeUnit.SECONDS)
        if (languageTag == "pl" || languageTag == "und") return prepared

        val sourceLanguage = TranslateLanguage.fromLanguageTag(languageTag) ?: return prepared
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLanguage)
            .setTargetLanguage(TranslateLanguage.POLISH)
            .build()
        val translator = Translation.getClient(options)
        return try {
            val conditions = DownloadConditions.Builder().build()
            Tasks.await(translator.downloadModelIfNeeded(conditions), 75, TimeUnit.SECONDS)
            val translatedTitle = Tasks.await(translator.translate(prepared.originalTitle), 20, TimeUnit.SECONDS)
            val translatedSummary = Tasks.await(translator.translate(prepared.originalSummary), 30, TimeUnit.SECONDS)
            prepared.copy(
                title = translatedTitle,
                summary = translatedSummary,
                translatedToPolish = true
            )
        } finally {
            translator.close()
        }
    }

    private fun Article.withOriginals(): Article = copy(
        originalTitle = originalTitle.ifBlank { title },
        originalSummary = originalSummary.ifBlank { summary },
        sources = sources.map { source ->
            if (source.originalTitle.isBlank()) source.copy(originalTitle = originalTitle.ifBlank { title }) else source
        }
    )
}

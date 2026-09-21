package pl.lab512.puls512.data

import android.text.Html
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.net.HttpURLConnection
import java.net.URL
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.math.max

class NewsRepository {
    private data class Feed(
        val source: String,
        val url: String,
        val category: NewsCategory,
        val trustWeight: Int
    )

    // Kuratorska lista startowa. Każdy wpis prowadzi do kanału RSS/Atom wydawcy.
    private val feeds = listOf(
        Feed("PAP", "https://www.pap.pl/rss.xml", NewsCategory.POLSKA, 5),
        Feed("Polskie Radio 24", "https://polskieradio24.pl/rss/35", NewsCategory.POLSKA, 4),
        Feed("RMF24", "https://www.rmf24.pl/fakty/feed", NewsCategory.POLSKA, 3),
        Feed("Euronews", "https://www.euronews.com/rss?level=theme&name=news", NewsCategory.EUROPA, 4),
        Feed("POLITICO Europe", "https://www.politico.eu/feed/", NewsCategory.EUROPA, 4),
        Feed("BBC World", "https://feeds.bbci.co.uk/news/world/rss.xml", NewsCategory.SWIAT, 5),
        Feed("DW", "https://rss.dw.com/rdf/rss-en-all", NewsCategory.SWIAT, 4),
        Feed("France 24", "https://www.france24.com/en/rss", NewsCategory.SWIAT, 4),
        Feed("The Guardian World", "https://www.theguardian.com/world/rss", NewsCategory.SWIAT, 3),
        Feed("BBC Business", "https://feeds.bbci.co.uk/news/business/rss.xml", NewsCategory.BIZNES, 4),
        Feed("BBC Technology", "https://feeds.bbci.co.uk/news/technology/rss.xml", NewsCategory.TECHNOLOGIA, 4)
    )

    fun fetchBriefing(categories: Set<NewsCategory>, limit: Int): List<Article> {
        val selectedFeeds = feeds.filter { it.category in categories }
        val pool = Executors.newFixedThreadPool(5)
        val candidates = try {
            pool.invokeAll(selectedFeeds.map { feed ->
                Callable { runCatching { readFeed(feed) }.getOrDefault(emptyList()) }
            }).flatMap { it.get() }.sortedByDescending { score(it) }
        } finally {
            pool.shutdownNow()
        }

        val unique = mutableListOf<Article>()
        for (article in candidates) {
            if (unique.none { isSimilar(it.title, article.title) }) unique += article
            if (unique.size >= limit) break
        }
        return unique.ifEmpty { demoBriefing(categories, limit) }
    }

    fun demoBriefing(categories: Set<NewsCategory>, limit: Int): List<Article> {
        val now = System.currentTimeMillis()
        val samples = listOf(
            Article("PULS 512 jest gotowy na pierwszy briefing", "To wersja demonstracyjna. Po połączeniu z internetem przeciągnij ekran lub naciśnij Odśwież, aby pobrać najnowsze materiały ze źródeł.", "", "PULS 512", NewsCategory.POLSKA, now, 5),
            Article("Wiadomości są grupowane i oceniane", "Aplikacja premiuje aktualność, jakość źródła i usuwa bardzo podobne nagłówki. W wersji produkcyjnej dojdzie potwierdzanie wydarzeń między redakcjami.", "", "PULS 512", NewsCategory.EUROPA, now - 60_000, 5),
            Article("Brief można odsłuchać po polsku", "Przycisk odtwarzania uruchamia syntezator mowy dostępny w telefonie. Tempo i głos można później rozszerzyć o ustawienia użytkownika.", "", "PULS 512", NewsCategory.SWIAT, now - 120_000, 5),
            Article("Godziny poranne i wieczorne wybiera użytkownik", "Każdy raport może zostać włączony lub wyłączony. Po zapisaniu aplikacja planuje powiadomienie na wybraną godzinę.", "", "PULS 512", NewsCategory.BIZNES, now - 180_000, 5),
            Article("Uwagi tekstowe i nagrania trafiają do Lab512", "W panelu Uwagi można napisać wiadomość albo nagrać głos. Telefon otworzy aplikację pocztową z gotowym odbiorcą.", "", "PULS 512", NewsCategory.TECHNOLOGIA, now - 240_000, 5)
        )
        return samples.filter { it.category in categories }.take(limit)
    }

    private fun readFeed(feed: Feed): List<Article> {
        val connection = (URL(feed.url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 7_000
            readTimeout = 8_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "PULS512/0.1 (+https://lab512.pl)")
            setRequestProperty("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml")
        }
        return try {
            connection.inputStream.buffered().use { input ->
                val parser = Xml.newPullParser().apply {
                    setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                    setInput(input, null)
                }
                parse(parser, feed)
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun parse(parser: XmlPullParser, feed: Feed): List<Article> {
        val result = mutableListOf<Article>()
        var event = parser.eventType
        var insideEntry = false
        var title = ""
        var description = ""
        var link = ""
        var date = ""

        while (event != XmlPullParser.END_DOCUMENT && result.size < 12) {
            if (event == XmlPullParser.START_TAG) {
                val tag = parser.name.lowercase(Locale.ROOT)
                if (tag == "item" || tag == "entry") {
                    insideEntry = true
                    title = ""; description = ""; link = ""; date = ""
                } else if (insideEntry) {
                    when (tag.substringAfter(':')) {
                        "title" -> title = readText(parser)
                        "description", "summary", "encoded" -> if (description.isBlank()) description = readText(parser)
                        "link" -> {
                            val href = parser.getAttributeValue(null, "href")
                            link = href ?: readText(parser)
                        }
                        "pubdate", "published", "updated" -> if (date.isBlank()) date = readText(parser)
                    }
                }
            } else if (event == XmlPullParser.END_TAG) {
                val tag = parser.name.lowercase(Locale.ROOT)
                if ((tag == "item" || tag == "entry") && insideEntry) {
                    val cleanTitle = clean(title, 180)
                    if (cleanTitle.isNotBlank()) {
                        result += Article(
                            title = cleanTitle,
                            summary = clean(description, 340).ifBlank { "Otwórz źródło, aby przeczytać szczegóły." },
                            url = link.trim(),
                            source = feed.source,
                            category = feed.category,
                            publishedAt = parseDate(date),
                            trustWeight = feed.trustWeight
                        )
                    }
                    insideEntry = false
                }
            }
            event = parser.next()
        }
        return result
    }

    private fun readText(parser: XmlPullParser): String {
        return if (parser.next() == XmlPullParser.TEXT) parser.text.orEmpty().also { parser.nextTag() } else ""
    }

    private fun clean(value: String, maxLength: Int): String {
        val plain = Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY).toString()
            .replace(Regex("\\s+"), " ")
            .trim()
        return if (plain.length <= maxLength) plain else plain.take(maxLength).substringBeforeLast(' ') + "…"
    }

    private fun parseDate(raw: String): Long {
        val parsers = listOf(
            DateTimeFormatter.RFC_1123_DATE_TIME,
            DateTimeFormatter.ISO_ZONED_DATE_TIME,
            DateTimeFormatter.ISO_OFFSET_DATE_TIME
        )
        for (formatter in parsers) {
            runCatching { return ZonedDateTime.parse(raw.trim(), formatter).toInstant().toEpochMilli() }
        }
        return System.currentTimeMillis()
    }

    private fun score(article: Article): Long {
        val ageHours = max(0, (System.currentTimeMillis() - article.publishedAt) / 3_600_000)
        return article.trustWeight * 1_000L - ageHours.coerceAtMost(500)
    }

    private fun isSimilar(a: String, b: String): Boolean {
        fun tokens(text: String) = text.lowercase(Locale("pl"))
            .replace(Regex("[^a-ząćęłńóśźż0-9 ]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 3 }
            .toSet()
        val one = tokens(a); val two = tokens(b)
        if (one.isEmpty() || two.isEmpty()) return false
        val overlap = one.intersect(two).size.toDouble()
        return overlap / one.union(two).size >= 0.55
    }
}

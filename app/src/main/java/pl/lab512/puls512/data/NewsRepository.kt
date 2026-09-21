package pl.lab512.puls512.data

import android.text.Html
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import pl.lab512.puls512.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.math.max

class NewsRepository {
    private enum class FeedKind { OFFICIAL_DATA, OFFICIAL_STATEMENT, AGENCY, EDITORIAL }

    private data class Feed(
        val source: String,
        val url: String,
        val category: NewsCategory,
        val trustWeight: Int,
        val kind: FeedKind
    )

    private data class Candidate(val article: Article, val kind: FeedKind)

    // Źródła bezpłatne i publiczne. Pełne serwisy Reuters/AP/AFP/PAP mogą wymagać licencji.
    private val feeds = listOf(
        Feed("PAP", "https://www.pap.pl/rss.xml", NewsCategory.POLSKA, 5, FeedKind.AGENCY),
        Feed("Polskie Radio 24", "https://polskieradio24.pl/rss/35", NewsCategory.POLSKA, 4, FeedKind.EDITORIAL),
        Feed("RMF24", "https://www.rmf24.pl/fakty/feed", NewsCategory.POLSKA, 4, FeedKind.EDITORIAL),
        Feed("Eurostat", "https://ec.europa.eu/eurostat/news/euro-indicators?_estatsearchportlet_WAR_estatsearchportlet_INSTANCE_OaTpFrwlabNK_collection=CAT_PREREL&_estatsearchportlet_WAR_estatsearchportlet_INSTANCE_OaTpFrwlabNK_pageNumber=1&_estatsearchportlet_WAR_estatsearchportlet_INSTANCE_OaTpFrwlabNK_pageSize=20&_estatsearchportlet_WAR_estatsearchportlet_INSTANCE_OaTpFrwlabNK_sort=lastUpdateDate&p_p_cacheability=cacheLevelPage&p_p_id=estatsearchportlet_WAR_estatsearchportlet_INSTANCE_OaTpFrwlabNK&p_p_lifecycle=2&p_p_mode=view&p_p_resource_id=atom&p_p_state=normal", NewsCategory.BIZNES, 5, FeedKind.OFFICIAL_DATA),
        Feed("Europejski Bank Centralny", "https://www.ecb.europa.eu/rss/press.html", NewsCategory.BIZNES, 5, FeedKind.OFFICIAL_DATA),
        Feed("Rada UE", "https://www.consilium.europa.eu/en/press/press-releases/rss/", NewsCategory.EUROPA, 5, FeedKind.OFFICIAL_STATEMENT),
        Feed("Euronews", "https://www.euronews.com/rss?level=theme&name=news", NewsCategory.EUROPA, 4, FeedKind.EDITORIAL),
        Feed("POLITICO Europe", "https://www.politico.eu/feed/", NewsCategory.EUROPA, 4, FeedKind.EDITORIAL),
        Feed("ONZ", "https://news.un.org/feed/subscribe/en/news/all/rss.xml", NewsCategory.SWIAT, 5, FeedKind.OFFICIAL_STATEMENT),
        Feed("WHO", "https://www.who.int/rss-feeds/news-english.xml", NewsCategory.SWIAT, 5, FeedKind.OFFICIAL_DATA),
        Feed("BBC World", "https://feeds.bbci.co.uk/news/world/rss.xml", NewsCategory.SWIAT, 5, FeedKind.EDITORIAL),
        Feed("DW", "https://rss.dw.com/rdf/rss-en-all", NewsCategory.SWIAT, 4, FeedKind.EDITORIAL),
        Feed("France 24", "https://www.france24.com/en/rss", NewsCategory.SWIAT, 4, FeedKind.EDITORIAL),
        Feed("The Guardian World", "https://www.theguardian.com/world/rss", NewsCategory.SWIAT, 3, FeedKind.EDITORIAL),
        Feed("BBC Business", "https://feeds.bbci.co.uk/news/business/rss.xml", NewsCategory.BIZNES, 4, FeedKind.EDITORIAL),
        Feed("BBC Technology", "https://feeds.bbci.co.uk/news/technology/rss.xml", NewsCategory.TECHNOLOGIA, 4, FeedKind.EDITORIAL),
        Feed("NASA", "https://www.nasa.gov/rss/dyn/breaking_news.rss", NewsCategory.TECHNOLOGIA, 5, FeedKind.OFFICIAL_DATA)
    )

    fun fetchBriefing(categories: Set<NewsCategory>, limit: Int, verifiedOnly: Boolean = true): List<Article> {
        if (BuildConfig.API_BASE_URL.isNotBlank()) {
            val remote = runCatching { BackendClient(BuildConfig.API_BASE_URL).fetch(categories, limit, verifiedOnly) }.getOrNull()
            if (!remote.isNullOrEmpty()) return PolishTranslator().translateAll(remote)
        }
        val local = fetchLocally(categories, limit, verifiedOnly)
        return PolishTranslator().translateAll(local)
    }

    private fun fetchLocally(categories: Set<NewsCategory>, limit: Int, verifiedOnly: Boolean): List<Article> {
        val selectedFeeds = feeds.filter { it.category in categories }
        val pool = Executors.newFixedThreadPool(6)
        val candidates = try {
            pool.invokeAll(selectedFeeds.map { feed ->
                Callable { runCatching { readFeed(feed) }.getOrDefault(emptyList()) }
            }).flatMap { it.get() }.sortedByDescending { score(it.article) }
        } finally {
            pool.shutdownNow()
        }

        val unused = candidates.toMutableList()
        val clustered = mutableListOf<Article>()
        while (unused.isNotEmpty()) {
            val seed = unused.removeAt(0)
            val matches = unused.filter { isSimilar(seed.article.title, it.article.title) }
            unused.removeAll(matches.toSet())
            val cluster = listOf(seed) + matches
            val sources = cluster.map {
                SourceRef(
                    it.article.source,
                    it.article.url,
                    it.kind in setOf(FeedKind.OFFICIAL_DATA, FeedKind.OFFICIAL_STATEMENT),
                    it.article.originalTitle.ifBlank { it.article.title }
                )
            }
                .distinctBy { it.name }
            val kinds = cluster.map { it.kind }.toSet()
            val status = when {
                FeedKind.OFFICIAL_DATA in kinds -> VerificationStatus.CONFIRMED
                sources.size >= 2 -> VerificationStatus.CONFIRMED
                FeedKind.OFFICIAL_STATEMENT in kinds -> VerificationStatus.OFFICIAL_SOURCE
                else -> VerificationStatus.DEVELOPING
            }
            val reason = when (status) {
                VerificationStatus.CONFIRMED -> if (FeedKind.OFFICIAL_DATA in kinds) "Dane pochodzą bezpośrednio z właściwej instytucji." else "Zgodność ${sources.size} niezależnych źródeł."
                VerificationStatus.OFFICIAL_SOURCE -> "Komunikat instytucji; treść zewnętrzna nie została jeszcze niezależnie potwierdzona."
                VerificationStatus.DEVELOPING -> "Pojedyncze źródło — materiał oczekuje na dodatkowe potwierdzenie."
            }
            val representative = cluster.maxBy { score(it.article) }.article
            clustered += representative.copy(
                sources = sources,
                verificationStatus = status,
                verificationReason = reason,
                whyItMatters = if (status == VerificationStatus.CONFIRMED) "Informacja przeszła automatyczną kontrolę źródeł PULS 512." else "Traktuj jako komunikat, a nie ostatecznie ustalony fakt."
            )
        }

        val result = clustered
            .filter { !verifiedOnly || it.verificationStatus == VerificationStatus.CONFIRMED }
            .sortedByDescending { score(it) }
            .take(limit)
        return result.ifEmpty { demoBriefing(categories, limit) }
    }

    fun demoBriefing(categories: Set<NewsCategory>, limit: Int): List<Article> {
        val now = System.currentTimeMillis()
        val confirmed = VerificationStatus.CONFIRMED
        val samples = listOf(
            Article("PULS 512 sprawdza wiadomości przed publikacją", "Wersja 1 grupuje informacje o tym samym wydarzeniu, porównuje źródła i domyślnie ukrywa materiały bez potwierdzenia.", "", "PULS 512", NewsCategory.POLSKA, now, 5, verificationStatus = confirmed, verificationReason = "Zasada działania aplikacji.", whyItMatters = "W briefingu jest mniej wiadomości, ale każda ma jawny poziom wiarygodności."),
            Article("Każda informacja pokazuje podstawę oceny", "Zielony status oznacza dane pierwotne albo zgodność co najmniej dwóch niezależnych źródeł. Komunikaty jednostronne są wyraźnie oznaczane.", "", "PULS 512", NewsCategory.EUROPA, now - 60_000, 5, verificationStatus = confirmed, verificationReason = "Zasada działania aplikacji.", whyItMatters = "Użytkownik widzi nie tylko treść, lecz także podstawę jej publikacji."),
            Article("Podsumowanie AI korzysta wyłącznie z zebranego materiału", "Po podłączeniu backendu model nie wyszukuje faktów samodzielnie. Tworzy krótki tekst wyłącznie z przekazanych artykułów i zachowuje ich linki.", "", "PULS 512", NewsCategory.TECHNOLOGIA, now - 120_000, 5, verificationStatus = confirmed, verificationReason = "Zasada działania backendu.", whyItMatters = "Ogranicza to ryzyko dopisywania przez model informacji, których nie ma w źródłach.")
        )
        return samples.filter { it.category in categories }.take(limit)
    }

    private fun readFeed(feed: Feed): List<Candidate> {
        val connection = (URL(feed.url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 7_000
            readTimeout = 9_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "PULS512/1.0 (+https://lab512.pl)")
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

    private fun parse(parser: XmlPullParser, feed: Feed): List<Candidate> {
        val result = mutableListOf<Candidate>()
        var event = parser.eventType
        var insideEntry = false
        var title = ""
        var description = ""
        var link = ""
        var date = ""

        while (event != XmlPullParser.END_DOCUMENT && result.size < 15) {
            if (event == XmlPullParser.START_TAG) {
                val tag = parser.name.lowercase(Locale.ROOT)
                if (tag == "item" || tag == "entry") {
                    insideEntry = true
                    title = ""; description = ""; link = ""; date = ""
                } else if (insideEntry) {
                    when (tag.substringAfter(':')) {
                        "title" -> title = readText(parser)
                        "description", "summary", "encoded" -> if (description.isBlank()) description = readText(parser)
                        "link" -> link = parser.getAttributeValue(null, "href") ?: readText(parser)
                        "pubdate", "published", "updated" -> if (date.isBlank()) date = readText(parser)
                    }
                }
            } else if (event == XmlPullParser.END_TAG) {
                val tag = parser.name.lowercase(Locale.ROOT)
                if ((tag == "item" || tag == "entry") && insideEntry) {
                    val cleanTitle = clean(title, 180)
                    if (cleanTitle.isNotBlank()) {
                        val article = Article(
                            title = cleanTitle,
                            summary = clean(description, 360).ifBlank { "Otwórz źródło, aby przeczytać szczegóły." },
                            url = link.trim(),
                            source = feed.source,
                            category = feed.category,
                            publishedAt = parseDate(date),
                            trustWeight = feed.trustWeight,
                            sources = listOf(SourceRef(feed.source, link.trim(), feed.kind in setOf(FeedKind.OFFICIAL_DATA, FeedKind.OFFICIAL_STATEMENT), cleanTitle)),
                            originalTitle = cleanTitle,
                            originalSummary = clean(description, 360).ifBlank { "Otwórz źródło, aby przeczytać szczegóły." }
                        )
                        result += Candidate(article, feed.kind)
                    }
                    insideEntry = false
                }
            }
            event = parser.next()
        }
        return result
    }

    private fun readText(parser: XmlPullParser): String =
        if (parser.next() == XmlPullParser.TEXT) parser.text.orEmpty().also { parser.nextTag() } else ""

    private fun clean(value: String, maxLength: Int): String {
        val plain = Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY).toString().replace(Regex("\\s+"), " ").trim()
        return if (plain.length <= maxLength) plain else plain.take(maxLength).substringBeforeLast(' ') + "…"
    }

    private fun parseDate(raw: String): Long {
        val parsers = listOf(DateTimeFormatter.RFC_1123_DATE_TIME, DateTimeFormatter.ISO_ZONED_DATE_TIME, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        for (formatter in parsers) runCatching { return ZonedDateTime.parse(raw.trim(), formatter).toInstant().toEpochMilli() }
        return System.currentTimeMillis()
    }

    private fun score(article: Article): Long {
        val ageHours = max(0, (System.currentTimeMillis() - article.publishedAt) / 3_600_000)
        val verificationBonus = if (article.verificationStatus == VerificationStatus.CONFIRMED) 2_000 else 0
        return article.trustWeight * 1_000L + verificationBonus - ageHours.coerceAtMost(500)
    }

    private fun isSimilar(a: String, b: String): Boolean {
        fun tokens(text: String) = text.lowercase(Locale("pl"))
            .replace(Regex("[^a-ząćęłńóśźż0-9 ]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 3 }
            .toSet()
        val one = tokens(a); val two = tokens(b)
        if (one.isEmpty() || two.isEmpty()) return false
        val intersection = one.intersect(two).size.toDouble()
        val jaccard = intersection / one.union(two).size
        val containment = intersection / minOf(one.size, two.size)
        return jaccard >= 0.38 || containment >= 0.62
    }
}

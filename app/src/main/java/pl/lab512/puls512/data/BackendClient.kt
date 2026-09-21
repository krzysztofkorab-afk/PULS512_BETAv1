package pl.lab512.puls512.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

class BackendClient(private val baseUrl: String) {
    fun fetch(categories: Set<NewsCategory>, limit: Int, verifiedOnly: Boolean): List<Article> {
        val categoryParam = URLEncoder.encode(categories.joinToString(",") { it.name }, "UTF-8")
        val endpoint = "${baseUrl.trimEnd('/')}/briefing?categories=$categoryParam&limit=$limit&verifiedOnly=$verifiedOnly"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "PULS512-Android/0.2")
        }
        return try {
            if (connection.responseCode !in 200..299) error("Backend HTTP ${connection.responseCode}")
            val root = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val items = root.getJSONArray("items")
            buildList {
                for (index in 0 until items.length()) {
                    val item = items.getJSONObject(index)
                    val sourcesJson = item.optJSONArray("sources")
                    val sources = buildList {
                        if (sourcesJson != null) for (sourceIndex in 0 until sourcesJson.length()) {
                            val source = sourcesJson.getJSONObject(sourceIndex)
                            add(SourceRef(source.getString("name"), source.getString("url"), source.optBoolean("primary", false)))
                        }
                    }
                    val category = NewsCategory.entries.firstOrNull { it.name == item.optString("category") } ?: NewsCategory.SWIAT
                    val status = VerificationStatus.entries.firstOrNull { it.name == item.optString("verificationStatus") }
                        ?: VerificationStatus.DEVELOPING
                    val firstSource = sources.firstOrNull() ?: SourceRef("PULS 512", "")
                    add(
                        Article(
                            title = item.getString("title"),
                            summary = item.getString("summary"),
                            url = firstSource.url,
                            source = firstSource.name,
                            category = category,
                            publishedAt = item.optLong("publishedAt", System.currentTimeMillis()),
                            trustWeight = 5,
                            sources = sources,
                            verificationStatus = status,
                            verificationReason = item.optString("verificationReason", "Ocena wykonana przez silnik PULS 512."),
                            whyItMatters = item.optString("whyItMatters", "")
                        )
                    )
                }
            }
        } finally {
            connection.disconnect()
        }
    }
}

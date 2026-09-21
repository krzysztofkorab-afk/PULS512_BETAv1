package pl.lab512.puls512.data

enum class NewsCategory(val label: String) {
    POLSKA("Polska"),
    EUROPA("Europa"),
    SWIAT("Świat"),
    BIZNES("Biznes"),
    TECHNOLOGIA("Technologia")
}

data class Article(
    val title: String,
    val summary: String,
    val url: String,
    val source: String,
    val category: NewsCategory,
    val publishedAt: Long = System.currentTimeMillis(),
    val trustWeight: Int = 1
)

data class UserSettings(
    val morningEnabled: Boolean = true,
    val morningHour: Int = 7,
    val morningMinute: Int = 0,
    val eveningEnabled: Boolean = true,
    val eveningHour: Int = 19,
    val eveningMinute: Int = 0,
    val categories: Set<NewsCategory> = NewsCategory.entries.toSet(),
    val briefingLength: Int = 6
)

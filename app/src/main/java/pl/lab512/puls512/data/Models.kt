package pl.lab512.puls512.data

enum class NewsCategory(val label: String) {
    POLSKA("Polska"),
    EUROPA("Europa"),
    SWIAT("Świat"),
    BIZNES("Biznes"),
    TECHNOLOGIA("Technologia")
}

enum class VerificationStatus(val label: String) {
    CONFIRMED("Potwierdzona"),
    OFFICIAL_SOURCE("Źródło oficjalne"),
    DEVELOPING("Rozwijająca się")
}

data class SourceRef(
    val name: String,
    val url: String,
    val primary: Boolean = false,
    val originalTitle: String = ""
)

enum class NarratorProfile(
    val displayName: String,
    val description: String,
    val genderLabel: String,
    val voiceSlot: Int,
    val pitch: Float,
    val rate: Float
) {
    ANNA("Anna", "spokojna i wyraźna", "głos kobiecy", 0, 1.08f, 0.96f),
    ZOFIA("Zofia", "ciepła i dynamiczna", "głos kobiecy", 1, 1.18f, 1.04f),
    MAREK("Marek", "niski i informacyjny", "głos męski", 2, 0.86f, 0.94f),
    PIOTR("Piotr", "głęboki i zdecydowany", "głos męski", 3, 0.76f, 1.01f)
}

data class Article(
    val title: String,
    val summary: String,
    val url: String,
    val source: String,
    val category: NewsCategory,
    val publishedAt: Long = System.currentTimeMillis(),
    val trustWeight: Int = 1,
    val sources: List<SourceRef> = listOf(SourceRef(source, url)),
    val verificationStatus: VerificationStatus = VerificationStatus.DEVELOPING,
    val verificationReason: String = "Pojedyncze źródło — informacja wymaga dalszego potwierdzenia.",
    val whyItMatters: String = "",
    val originalTitle: String = title,
    val originalSummary: String = summary,
    val translatedToPolish: Boolean = false
)

data class UserSettings(
    val morningEnabled: Boolean = true,
    val morningHour: Int = 7,
    val morningMinute: Int = 0,
    val eveningEnabled: Boolean = true,
    val eveningHour: Int = 19,
    val eveningMinute: Int = 0,
    val categories: Set<NewsCategory> = NewsCategory.entries.toSet(),
    val briefingLength: Int = 6,
    val verifiedOnly: Boolean = true,
    val autoReadEnabled: Boolean = false,
    val narratorProfile: NarratorProfile = NarratorProfile.ANNA
)

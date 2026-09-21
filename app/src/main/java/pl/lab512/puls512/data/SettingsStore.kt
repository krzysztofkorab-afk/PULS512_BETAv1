package pl.lab512.puls512.data

import android.content.Context

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("puls512_settings", Context.MODE_PRIVATE)

    fun load(): UserSettings {
        val saved = prefs.getStringSet("categories", null)
        val categories = saved?.mapNotNull { value ->
            NewsCategory.entries.firstOrNull { it.name == value }
        }?.toSet()?.takeIf { it.isNotEmpty() } ?: NewsCategory.entries.toSet()

        return UserSettings(
            morningEnabled = prefs.getBoolean("morning_enabled", true),
            morningHour = prefs.getInt("morning_hour", 7),
            morningMinute = prefs.getInt("morning_minute", 0),
            eveningEnabled = prefs.getBoolean("evening_enabled", true),
            eveningHour = prefs.getInt("evening_hour", 19),
            eveningMinute = prefs.getInt("evening_minute", 0),
            categories = categories,
            briefingLength = prefs.getInt("briefing_length", 6)
        )
    }

    fun save(settings: UserSettings) {
        prefs.edit()
            .putBoolean("morning_enabled", settings.morningEnabled)
            .putInt("morning_hour", settings.morningHour)
            .putInt("morning_minute", settings.morningMinute)
            .putBoolean("evening_enabled", settings.eveningEnabled)
            .putInt("evening_hour", settings.eveningHour)
            .putInt("evening_minute", settings.eveningMinute)
            .putStringSet("categories", settings.categories.map { it.name }.toSet())
            .putInt("briefing_length", settings.briefingLength)
            .apply()
    }
}

package pl.lab512.puls512.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import pl.lab512.puls512.data.SettingsStore
import pl.lab512.puls512.data.UserSettings
import java.util.Calendar

object AlarmScheduler {
    const val EXTRA_KIND = "briefing_kind"
    const val MORNING = "morning"
    const val EVENING = "evening"

    fun scheduleAll(context: Context, settings: UserSettings = SettingsStore(context).load()) {
        schedule(context, MORNING, settings.morningEnabled, settings.morningHour, settings.morningMinute, 5121)
        schedule(context, EVENING, settings.eveningEnabled, settings.eveningHour, settings.eveningMinute, 5122)
    }

    fun scheduleNext(context: Context, kind: String) {
        val settings = SettingsStore(context).load()
        if (kind == MORNING) {
            schedule(context, kind, settings.morningEnabled, settings.morningHour, settings.morningMinute, 5121)
        } else {
            schedule(context, kind, settings.eveningEnabled, settings.eveningHour, settings.eveningMinute, 5122)
        }
    }

    private fun schedule(context: Context, kind: String, enabled: Boolean, hour: Int, minute: Int, requestCode: Int) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, BriefingReceiver::class.java).putExtra(EXTRA_KIND, kind),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        manager.cancel(pending)
        if (!enabled) return

        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis

        if (Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms()) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pending)
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pending)
        }
    }
}

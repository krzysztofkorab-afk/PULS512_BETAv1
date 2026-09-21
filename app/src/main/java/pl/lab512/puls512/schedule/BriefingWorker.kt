package pl.lab512.puls512.schedule

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import pl.lab512.puls512.MainActivity
import pl.lab512.puls512.R
import pl.lab512.puls512.data.NewsRepository
import pl.lab512.puls512.data.SettingsStore

class BriefingWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val kind = inputData.getString(AlarmScheduler.EXTRA_KIND) ?: AlarmScheduler.MORNING
        return runCatching {
            val settings = SettingsStore(applicationContext).load()
            val articles = NewsRepository().fetchBriefing(settings.categories, settings.briefingLength)
            showNotification(kind, articles.firstOrNull()?.title ?: "Twój briefing jest gotowy")
            Result.success()
        }.getOrElse { Result.retry() }
    }

    private fun showNotification(kind: String, lead: String) {
        val channelId = "puls512_briefings"
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Poranne i wieczorne briefingi", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Najważniejsze informacje wybrane przez PULS 512"
                }
            )
        }
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return

        val openApp = PendingIntent.getActivity(
            applicationContext,
            5100,
            Intent(applicationContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = if (kind == AlarmScheduler.MORNING) "Dzień dobry — poranny PULS" else "Dobry wieczór — wieczorny PULS"
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(lead)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$lead\n\nDotknij, aby przeczytać lub odsłuchać cały briefing."))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(if (kind == AlarmScheduler.MORNING) 5121 else 5122, notification)
    }
}

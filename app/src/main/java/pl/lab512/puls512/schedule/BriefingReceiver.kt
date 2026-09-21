package pl.lab512.puls512.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

class BriefingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra(AlarmScheduler.EXTRA_KIND) ?: AlarmScheduler.MORNING
        val request = OneTimeWorkRequestBuilder<BriefingWorker>()
            .setInputData(workDataOf(AlarmScheduler.EXTRA_KIND to kind))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("puls512_$kind", ExistingWorkPolicy.REPLACE, request)
        AlarmScheduler.scheduleNext(context, kind)
    }
}

package com.almamun252.nikhuthisab.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

// Debug-only reminder test, callable by the ADB shell permission holder.
class ReminderTestReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val request = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(10, TimeUnit.SECONDS)
            .setInputData(workDataOf("type" to "DailyReminder", "debug_test" to true))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "daily_reminder_debug_test", ExistingWorkPolicy.REPLACE, request
        )
        Log.i("DailyReminderWorker", "Reminder test queued with a 10-second minimum delay")
    }
}

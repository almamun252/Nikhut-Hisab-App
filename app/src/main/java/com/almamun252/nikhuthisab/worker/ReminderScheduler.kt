package com.almamun252.nikhuthisab.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    // দেনা-পাওনা নির্দিষ্ট তারিখের রিমাইন্ডার
    fun scheduleReminder(
        context: Context,
        transactionId: Int,
        title: String,
        amount: Float,
        type: String,
        dueDate: Long
    ) {
        val currentTime = System.currentTimeMillis()

        if (dueDate <= currentTime) {
            return
        }

        val reminderTime = dueDate - TimeUnit.HOURS.toMillis(24)
        val delay = reminderTime - currentTime

        val actualDelay = if (delay > 0) delay else TimeUnit.MINUTES.toMillis(1)

        val inputData = Data.Builder()
            .putInt("transactionId", transactionId)
            .putString("title", title)
            .putFloat("amount", amount)
            .putString("type", type)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(actualDelay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .build()

        val workName = "reminder_$transactionId"

        WorkManager.getInstance(context).enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancelReminder(context: Context, transactionId: Int) {
        val workName = "reminder_$transactionId"
        WorkManager.getInstance(context).cancelUniqueWork(workName)
    }

    // Reopening the app keeps the existing dated request instead of resetting its delay.
    @Synchronized
    fun scheduleDailyReminder(context: Context) {
        val app = context.applicationContext
        val prefs = app.getSharedPreferences("daily_reminder_settings", Context.MODE_PRIVATE)
        val manager = WorkManager.getInstance(app)
        if (!prefs.getBoolean("legacy_schedule_removed", false)) {
            manager.cancelUniqueWork("daily_8pm_reminder")
            prefs.edit().putBoolean("legacy_schedule_removed", true).apply()
        }
        val now = java.time.Instant.now()
        val zone = java.time.ZoneId.systemDefault()
        val target = DailyReminderTime.nextRun(now, zone)
        val date = target.atZone(zone).toLocalDate().toString()
        val request = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(target.toEpochMilli() - now.toEpochMilli(), TimeUnit.MILLISECONDS)
            .setInputData(Data.Builder().putString("type", "DailyReminder").build())
            .addTag("daily_8pm_reminder_v2")
            .build()
        manager.enqueueUniqueWork(
            "daily_8pm_reminder_$date", ExistingWorkPolicy.KEEP, request
        )
    }

    // Persist the switch and keep one nightly request per account/date.
    private const val BACKUP_PREFS = "nightly_backup_settings"
    private const val ENABLED = "enabled"
    private const val SEED = "installation_seed"
    private const val BACKUP_TAG = "nightly_backup_v2"

    fun isAutoBackupEnabled(context: Context): Boolean =
        context.getSharedPreferences(BACKUP_PREFS, Context.MODE_PRIVATE)
            .getBoolean(ENABLED, true)

    fun setAutoBackupEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(BACKUP_PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(ENABLED, enabled).apply()
        if (enabled) scheduleDailyAutoBackup(context) else cancelAutoBackup(context)
    }

    @Synchronized
    fun scheduleDailyAutoBackup(context: Context) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(BACKUP_PREFS, Context.MODE_PRIVATE)
        val workManager = WorkManager.getInstance(appContext)
        if (!prefs.getBoolean("legacy_schedule_removed", false)) {
            workManager.cancelUniqueWork("NikhutHisabAutoBackup")
            prefs.edit().putBoolean("legacy_schedule_removed", true).apply()
        }
        if (!isAutoBackupEnabled(appContext)) return
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser ?: return
        val seed = if (prefs.contains(SEED)) prefs.getLong(SEED, 0L) else {
            kotlin.random.Random.nextLong().also {
                prefs.edit().putLong(SEED, it).apply()
            }
        }
        val now = java.time.Instant.now()
        val zone = java.time.ZoneId.systemDefault()
        val target = NightlyBackupTime.nextRun(now, zone, seed)
        val input = Data.Builder()
            .putString("backup_uid", user.uid)
            .putLong("scheduled_at", target.toEpochMilli())
            .build()
        val request = OneTimeWorkRequestBuilder<AutoBackupWorker>()
            .setInputData(input)
            .setInitialDelay(target.toEpochMilli() - now.toEpochMilli(), TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
            .addTag(BACKUP_TAG)
            .build()
        val date = target.atZone(zone).toLocalDate()
        workManager.enqueueUniqueWork(
            "nightly_backup_${user.uid}_$date", ExistingWorkPolicy.KEEP, request
        )
    }

    // WorkManager retains queued work across app closure and ordinary reboots.
    fun cancelAutoBackup(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(BACKUP_TAG)
        WorkManager.getInstance(context).cancelUniqueWork("NikhutHisabAutoBackup")
    }
}

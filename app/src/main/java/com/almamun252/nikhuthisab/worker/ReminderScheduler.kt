package com.almamun252.nikhuthisab.worker

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
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

    // প্রতিদিন রাত ৮ টার রিমাইন্ডার (Daily 8:00 PM Reminder)
    fun scheduleDailyReminder(context: Context) {
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 20) // রাত ৮টা (24-hour format এ 20)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)

            // যদি আজকের রাত ৮টা ইতিমধ্যে পার হয়ে গিয়ে থাকে, তাহলে আগামীকালের জন্য সেট করবে
            if (before(currentDate)) {
                add(Calendar.HOUR_OF_DAY, 24)
            }
        }

        val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis

        val inputData = Data.Builder()
            .putString("type", "DailyReminder")
            .build()

        // প্রতিদিন একবার চালানোর জন্য PeriodicWorkRequest (২৪ ঘণ্টা অন্তর)
        val dailyWorkRequest = PeriodicWorkRequestBuilder<NotificationWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "daily_8pm_reminder",
            ExistingPeriodicWorkPolicy.UPDATE,
            dailyWorkRequest
        )
    }
}
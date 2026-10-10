package com.almamun252.nikhuthisab.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.almamun252.nikhuthisab.MainActivity
import com.almamun252.nikhuthisab.R
import com.almamun252.nikhuthisab.utils.NotificationAppearance
import kotlinx.coroutines.CancellationException
import java.time.LocalDate

class NotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        val type = inputData.getString("type") ?: "Lending"
        val daily = type == "DailyReminder"
        val test = inputData.getBoolean("debug_test", false)
        val prefs = context.getSharedPreferences("daily_reminder_settings", Context.MODE_PRIVATE)
        val today = LocalDate.now().toString()

        if (daily) {
            // Always keep tomorrow queued, including when today's permission is denied.
            ReminderScheduler.scheduleDailyReminder(context)
            if (!test && prefs.getString("last_delivered_date", null) == today) {
                return Result.success(workDataOf("status" to "already_delivered_today"))
            }
        }
        val title: String
        val message: String
        val id: Int
        if (daily) {
            title = context.getString(R.string.notif_daily_title)
            message = context.getString(R.string.notif_daily_desc)
            id = 8000
        } else {
            val name = inputData.getString("title") ?: context.getString(R.string.notif_unknown_person)
            val amount = inputData.getFloat("amount", 0f).toInt()
            val transactionId = inputData.getInt("transactionId", -1)
            title = context.getString(R.string.notif_payment_title)
            message = context.getString(
                if (type == "Lending") R.string.notif_payment_lending else R.string.notif_payment_borrowing,
                name, amount
            )
            id = transactionId.takeIf { it != -1 } ?: System.currentTimeMillis().toInt()
        }
        return try {
            val posted = showNotification(title, message, id, daily)
            if (posted && daily && !test) {
                prefs.edit().putString("last_delivered_date", today).apply()
            }
            Log.i("DailyReminderWorker", if (posted) "Notification posted" else "Notification blocked by permission/channel settings")
            Result.success(workDataOf("status" to if (posted) "posted" else "notifications_disabled"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("DailyReminderWorker", "Notification unavailable: ${e.javaClass.simpleName}")
            Result.failure(workDataOf("status" to "notification_error"))
        }
    }

    private fun showNotification(title: String, message: String, id: Int, daily: Boolean): Boolean {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) return false
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = if (daily) "daily_accounting_reminder" else "reminder_channel"
        val channelName = context.getString(
            if (daily) R.string.notif_daily_channel_name else R.string.notif_channel_name
        )
        manager.createNotificationChannel(NotificationChannel(
            channelId, channelName, NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notif_channel_desc)
        })
        if (manager.getNotificationChannel(channelId)?.importance == NotificationManager.IMPORTANCE_NONE) return false
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationAppearance.apply(context, NotificationCompat.Builder(context, channelId))
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        manager.notify(id, notification)
        return true
    }
}

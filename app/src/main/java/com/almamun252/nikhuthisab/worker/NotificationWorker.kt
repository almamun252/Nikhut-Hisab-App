package com.almamun252.nikhuthisab.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.almamun252.nikhuthisab.MainActivity
import com.almamun252.nikhuthisab.R

class NotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // শিডিউলার থেকে পাঠানো ডেটাগুলো গ্রহণ করা
        val type = inputData.getString("type") ?: "Lending"

        val title: String
        val message: String
        val notificationId: Int

        // টাইপ অনুযায়ী মেসেজ এবং টাইটেল সেট করা (ডায়নামিক স্ট্রিং ব্যবহার করে)
        if (type == "DailyReminder") {
            title = context.getString(R.string.notif_daily_title)
            message = context.getString(R.string.notif_daily_desc)
            notificationId = 8000 // প্রতিদিনের রিমাইন্ডারের জন্য একটি নির্দিষ্ট আইডি
        } else {
            val personName = inputData.getString("title") ?: context.getString(R.string.notif_unknown_person)
            val amount = inputData.getFloat("amount", 0f).toInt()
            val transactionId = inputData.getInt("transactionId", -1)

            title = context.getString(R.string.notif_payment_title)
            message = if (type == "Lending") {
                context.getString(R.string.notif_payment_lending, personName, amount)
            } else {
                context.getString(R.string.notif_payment_borrowing, personName, amount)
            }
            notificationId = transactionId.takeIf { it != -1 } ?: System.currentTimeMillis().toInt()
        }

        // নোটিফিকেশন ফায়ার করা
        showNotification(
            title = title,
            message = message,
            notificationId = notificationId
        )

        return Result.success()
    }

    private fun showNotification(title: String, message: String, notificationId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "reminder_channel"

        // Android 8.0 (API 26) বা তার ওপরের জন্য Notification Channel তৈরি করা বাধ্যতামূলক
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_channel_desc)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // নোটিফিকেশনে ক্লিক করলে অ্যাপ ওপেন হওয়ার জন্য Intent তৈরি করা
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // নোটিফিকেশনটি বিল্ড করা
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // এখানে পরে আপনার অ্যাপের আইকন দিতে পারেন
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // ক্লিক করার পর নোটিফিকেশনটি মুছে যাবে
            .build()

        // নোটিফিকেশনটি স্ক্রিনে দেখানো
        notificationManager.notify(notificationId, notification)
    }
}
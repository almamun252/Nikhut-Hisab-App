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

class NotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // শিডিউলার থেকে পাঠানো ডেটাগুলো গ্রহণ করা
        val personName = inputData.getString("title") ?: "অজ্ঞাত"
        val amount = inputData.getFloat("amount", 0f)
        val type = inputData.getString("type") ?: "Lending"
        val transactionId = inputData.getInt("transactionId", -1)

        // লেনদেনের ধরন অনুযায়ী মেসেজ তৈরি করা
        val message = if (type == "Lending") {
            "আগামীকাল $personName এর ৳${amount.toInt()} দেওয়ার কথা।"
        } else {
            "আগামীকাল $personName কে ৳${amount.toInt()} পরিশোধ করতে হবে।"
        }

        // নোটিফিকেশন ফায়ার করা
        showNotification(
            title = "পেমেন্ট রিমাইন্ডার!",
            message = message,
            notificationId = transactionId.takeIf { it != -1 } ?: System.currentTimeMillis().toInt()
        )

        return Result.success()
    }

    private fun showNotification(title: String, message: String, notificationId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "reminder_channel"

        // Android 8.0 (API 26) বা তার ওপরের জন্য Notification Channel তৈরি করা বাধ্যতামূলক
        // এখানে Build.VERSION_CODES.O ব্যবহার করা হয়েছে
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Transaction Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "দেনা-পাওনা এবং বিলের রিমাইন্ডার"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // নোটিফিকেশনে ক্লিক করলে অ্যাপ ওপেন হওয়ার জন্য Intent তৈরি করা
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
            .setSmallIcon(android.R.drawable.ic_popup_reminder) // এখানে পরে আপনার অ্যাপের আইকন দিতে পারেন
            .setContentTitle(title)
            .setContentText(message)
            // IMPORTANCE_HIGH এর বদলে PRIORITY_HIGH ব্যবহার করা হয়েছে
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // ক্লিক করার পর নোটিফিকেশনটি মুছে যাবে
            .build()

        // নোটিফিকেশনটি স্ক্রিনে দেখানো
        notificationManager.notify(notificationId, notification)
    }
}
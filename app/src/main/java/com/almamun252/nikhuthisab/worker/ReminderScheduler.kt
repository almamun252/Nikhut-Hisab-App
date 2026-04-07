package com.almamun252.nikhuthisab.worker

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    fun scheduleReminder(
        context: Context,
        transactionId: Int,
        title: String,
        amount: Float,
        type: String,
        dueDate: Long
    ) {
        val currentTime = System.currentTimeMillis()

        // যদি ডেডলাইন পার হয়ে গিয়ে থাকে, তবে আর নোটিফিকেশন শিডিউল করার দরকার নেই
        if (dueDate <= currentTime) {
            return
        }

        // ডেডলাইনের ২৪ ঘণ্টা (৮৬,৪০০,০০০ মিলিসেকেন্ড) আগের সময় হিসাব করা
        val reminderTime = dueDate - TimeUnit.HOURS.toMillis(24)
        val delay = reminderTime - currentTime

        // যদি ডেডলাইনের আর ২৪ ঘণ্টাও বাকি না থাকে, তবে আমরা খুব অল্প সময়ের (১ মিনিট) মধ্যে
        // নোটিফিকেশনটি ফায়ার করে দেব যাতে ইউজার মিস না করে।
        val actualDelay = if (delay > 0) delay else TimeUnit.MINUTES.toMillis(1)

        // ওয়ার্কারের কাছে ডেটা পাঠানো
        val inputData = Data.Builder()
            .putInt("transactionId", transactionId)
            .putString("title", title)
            .putFloat("amount", amount)
            .putString("type", type)
            .build()

        // একবার কাজ করার জন্য রিকোয়েস্ট তৈরি
        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(actualDelay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .build()

        // প্রতিটি লেনদেনের জন্য একটি ইউনিক নাম সেট করা, যাতে পরে চাইলে ক্যানসেল বা আপডেট করা যায়
        val workName = "reminder_$transactionId"

        // WorkManager এর কাছে শিডিউল জমা দেওয়া
        WorkManager.getInstance(context).enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.REPLACE, // যদি একই আইডির জন্য আগে কোনো কাজ থাকে, তবে তা নতুন ডেটা দিয়ে রিপ্লেস হবে
            workRequest
        )
    }

    // নোটিফিকেশন শিডিউল ক্যানসেল করার ফাংশন
    fun cancelReminder(context: Context, transactionId: Int) {
        val workName = "reminder_$transactionId"
        WorkManager.getInstance(context).cancelUniqueWork(workName)
    }
}
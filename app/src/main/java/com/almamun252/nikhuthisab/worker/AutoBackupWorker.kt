package com.almamun252.nikhuthisab.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.almamun252.nikhuthisab.R
import com.almamun252.nikhuthisab.utils.BackupRestoreManager
import com.almamun252.nikhuthisab.utils.GoogleDriveHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AutoBackupWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val channelId = "backup_channel"
    private val notificationId = 101

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            // ১. কাজ শুরু হওয়ার সাথে সাথে প্রোগ্রেস বারসহ নোটিফিকেশন দেখানো হচ্ছে
            createNotificationChannel()
            setForeground(createForegroundInfo())

            try {
                val account = GoogleSignIn.getLastSignedInAccount(context)
                if (account == null) {
                    return@withContext Result.failure()
                }

                val driveHelper = GoogleDriveHelper(context)
                val backupManager = BackupRestoreManager(context)
                val driveService = driveHelper.getDriveService(account)

                val jsonData = backupManager.createBackupJson()
                val isSuccess = driveHelper.uploadBackup(driveService, jsonData)

                if (isSuccess) {
                    // ২. ব্যাকআপ সফল হলে প্রোগ্রেস বার সরিয়ে Success নোটিফিকেশন দেওয়া হচ্ছে
                    showSuccessNotification()
                    Result.success()
                } else {
                    Result.retry()
                }

            } catch (e: GoogleJsonResponseException) {
                if (e.statusCode == 403 || e.statusCode == 429) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Result.retry()
            }
        }
    }

    // 🔥 প্রোগ্রেস বারসহ নোটিফিকেশন তৈরি করার ফাংশন
    private fun createForegroundInfo(): ForegroundInfo {
        val title = context.getString(R.string.backup_notification_title)
        val desc = context.getString(R.string.backup_notification_desc)

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(desc)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // আপনার অ্যাপের আইকন দিন
            .setOngoing(true)
            // 🔥 setProgress(max, progress, isIndeterminate) -> true দিলে অনির্দিষ্ট প্রোগ্রেস বার দেখাবে
            .setProgress(0, 0, true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        return ForegroundInfo(notificationId, notification)
    }

    // 🔥 ব্যাকআপ সফল হওয়ার পর নোটিফিকেশন আপডেট করার ফাংশন
    private fun showSuccessNotification() {
        val successTitle = context.getString(R.string.backup_success_title)
        val successDesc = context.getString(R.string.backup_success_desc)

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(successTitle)
            .setContentText(successDesc)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            // 🔥 ব্যাকআপ শেষ তাই প্রোগ্রেস বার সরিয়ে দেওয়া হলো (0, 0, false)
            .setProgress(0, 0, false)
            .setOngoing(false) // নোটিফিকেশনটি চাইলে ইউজার সোয়াইপ করে কেটে দিতে পারবে
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    // Android 8.0 (Oreo) বা তার উপরের সংস্করণের জন্য নোটিফিকেশন চ্যানেল তৈরি
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = context.getString(R.string.backup_notification_channel_name)
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }
}
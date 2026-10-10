package com.almamun252.nikhuthisab.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.almamun252.nikhuthisab.utils.NotificationAppearance
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.almamun252.nikhuthisab.R
import com.almamun252.nikhuthisab.utils.BackupRestoreManager
import com.almamun252.nikhuthisab.utils.GoogleDriveHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.drive.DriveScopes
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class AutoBackupWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    companion object {
        private const val TAG = "AutoBackupWorker"
        private const val CHANNEL_ID = "backup_channel"
        private const val MAX_ATTEMPTS = 5
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (!ReminderScheduler.isAutoBackupEnabled(context)) return@withContext Result.success()
        val user = FirebaseAuth.getInstance().currentUser ?: return@withContext Result.success()
        val expectedUid = inputData.getString("backup_uid") ?: user.uid
        if (expectedUid != user.uid) return@withContext Result.success()

        // Queue the next night even if this attempt later needs retries or renewed consent.
        ReminderScheduler.scheduleDailyAutoBackup(context)
        context.getSharedPreferences("nightly_backup_status", Context.MODE_PRIVATE).edit()
            .putLong("last_attempt_at", System.currentTimeMillis()).apply()
        Log.i(TAG, "Nightly backup started; attempt=${runAttemptCount + 1}")

        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
                ?: return@withContext failure("google_login_required")
            if (!GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_APPDATA))) {
                return@withContext failure("drive_consent_required")
            }
            if (account.id != user.providerData.firstOrNull { it.providerId == "google.com" }?.uid) {
                return@withContext failure("google_account_mismatch")
            }
            val helper = GoogleDriveHelper(context)
            val json = BackupRestoreManager(context).createBackupJson()
            if (!ReminderScheduler.isAutoBackupEnabled(context) ||
                FirebaseAuth.getInstance().currentUser?.uid != expectedUid) {
                return@withContext Result.success()
            }
            helper.uploadBackupOrThrow(helper.getDriveService(account), json)
            val completedAt = System.currentTimeMillis()
            context.getSharedPreferences("nightly_backup_status", Context.MODE_PRIVATE).edit()
                .putLong("last_success_at", completedAt).remove("last_error").apply()
            Log.i(TAG, "Nightly backup completed")
            // Notification permission must never decide whether backup itself succeeds.
            showSuccessNotification()
            Result.success(workDataOf("completed_at" to completedAt))
        } catch (e: CancellationException) {
            throw e
        } catch (e: GoogleJsonResponseException) {
            val reasons = e.details?.errors?.mapNotNull { it.reason }.orEmpty()
            val transient = e.statusCode == 429 || e.statusCode in 500..599 ||
                (e.statusCode == 403 && reasons.any {
                    it == "rateLimitExceeded" || it == "userRateLimitExceeded" || it == "backendError"
                })
            if (transient) retryOrFail("drive_http_${e.statusCode}")
            else failure("drive_http_${e.statusCode}")
        } catch (e: IOException) {
            retryOrFail("network_or_auth_unavailable")
        } catch (e: Exception) {
            // Record an error category without account details, tokens, or backup contents.
            failure("backup_${e.javaClass.simpleName}")
        }
    }

    private fun retryOrFail(reason: String): Result {
        recordFailure(reason)
        return if (runAttemptCount + 1 < MAX_ATTEMPTS) Result.retry()
        else Result.failure(workDataOf("error" to reason))
    }

    private fun failure(reason: String): Result {
        recordFailure(reason)
        return Result.failure(workDataOf("error" to reason))
    }

    private fun recordFailure(reason: String) {
        Log.w(TAG, "Nightly backup unavailable: $reason")
        context.getSharedPreferences("nightly_backup_status", Context.MODE_PRIVATE)
            .edit().putString("last_error", reason).apply()
    }

    private fun showSuccessNotification() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) return
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(NotificationChannel(
                CHANNEL_ID, context.getString(R.string.backup_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ))
            manager.notify(101, NotificationAppearance.apply(context, NotificationCompat.Builder(context, CHANNEL_ID))
                .setContentTitle(context.getString(R.string.backup_success_title))
                .setContentText(context.getString(R.string.backup_success_desc))
                .setAutoCancel(true)
                .build())
        } catch (e: RuntimeException) {
            Log.w(TAG, "Backup saved; success notification unavailable")
        }
    }
}

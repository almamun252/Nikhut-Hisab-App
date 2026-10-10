package com.almamun252.nikhuthisab.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.TimeUnit

// Debug-only, shell-permission-protected test of the real worker without waiting until midnight.
class AutoBackupTestReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null || !ReminderScheduler.isAutoBackupEnabled(context)) {
            Log.w("AutoBackupWorker", "Test requires Google login and auto backup enabled")
            return
        }
        val request = OneTimeWorkRequestBuilder<AutoBackupWorker>()
            .setInitialDelay(60, TimeUnit.SECONDS)
            .setInputData(workDataOf("backup_uid" to user.uid))
            .setConstraints(Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED).build())
            .addTag("nightly_backup_v2")
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "nightly_backup_debug_test", ExistingWorkPolicy.REPLACE, request
        )
        Log.i("AutoBackupWorker", "Test queued with a 60-second minimum delay")
    }
}

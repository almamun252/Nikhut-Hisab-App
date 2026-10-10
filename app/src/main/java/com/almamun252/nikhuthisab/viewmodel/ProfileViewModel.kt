package com.almamun252.nikhuthisab.viewmodel

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.almamun252.nikhuthisab.utils.NotificationAppearance
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.almamun252.nikhuthisab.R
import com.almamun252.nikhuthisab.repository.AuthRepository
import com.almamun252.nikhuthisab.repository.FirebaseAuthImpl
import com.almamun252.nikhuthisab.utils.BackupRestoreManager
import com.almamun252.nikhuthisab.worker.ReminderScheduler
import com.almamun252.nikhuthisab.utils.GoogleDriveHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ProfileUiState(
    val isLoggedIn: Boolean = false,
    val userName: String = "",
    val userEmail: String = "",
    val lastBackupTime: String = "",
    val isAutoBackupEnabled: Boolean = true,
    val isDarkModeEnabled: Boolean = false,
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false
)

class ProfileViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: AuthRepository = FirebaseAuthImpl(application)
    private val driveHelper = GoogleDriveHelper(application)
    private val backupManager = BackupRestoreManager(application)

    // --- নোটিফিকেশন সেটআপ ---
    private val notificationManager = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val channelId = "manual_backup_channel"
    private val notificationId = 102

    private val _uiState = MutableStateFlow(
        ProfileUiState(
            lastBackupTime = "Checking backup...",
            isAutoBackupEnabled = ReminderScheduler.isAutoBackupEnabled(application)
        )
    )
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        createNotificationChannel() // অ্যাপ চালুর সময় চ্যানেল তৈরি করে নেবে

        viewModelScope.launch {
            repository.currentUser.collect { user ->
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoggedIn = user != null,
                        userName = user?.displayName ?: application.getString(R.string.guest_user),
                        userEmail = user?.email ?: application.getString(R.string.login_desc)
                    )
                }

                if (user != null) {
                    fetchLastBackupTimeFromDrive()
                } else {
                    _uiState.update { it.copy(lastBackupTime = application.getString(R.string.never_backed_up)) }
                }
            }
        }
    }

    private fun fetchLastBackupTimeFromDrive() {
        viewModelScope.launch {
            try {
                val account = GoogleSignIn.getLastSignedInAccount(getApplication())
                if (account != null) {
                    val driveService = driveHelper.getDriveService(account)
                    val lastModifiedTimeMs = driveHelper.getLastBackupTime(driveService)

                    if (lastModifiedTimeMs != null) {
                        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                        val formattedTime = sdf.format(Date(lastModifiedTimeMs))
                        _uiState.update { it.copy(lastBackupTime = formattedTime) }
                    } else {
                        _uiState.update { it.copy(lastBackupTime = getApplication<Application>().getString(R.string.never_backed_up)) }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(lastBackupTime = "Sync Error") }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.signOut()
            val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
            ).build()
            val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(
                getApplication<Application>(), gso
            )
            googleSignInClient.signOut().addOnCompleteListener {
                _uiState.update { it.copy(lastBackupTime = getApplication<Application>().getString(R.string.never_backed_up)) }
            }
        }
    }

    // --- Backup & Restore Actions ---
    fun triggerBackup(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBackingUp = true) }
            showProgressNotification(getApplication<Application>().getString(R.string.backup_notification_title))

            try {
                val account = GoogleSignIn.getLastSignedInAccount(getApplication())

                if (account != null) {
                    val driveService = driveHelper.getDriveService(account)
                    val jsonData = backupManager.createBackupJson()
                    val isSuccess = driveHelper.uploadBackup(driveService, jsonData)

                    if (isSuccess) {
                        fetchLastBackupTimeFromDrive()
                        _uiState.update { it.copy(isBackingUp = false) }
                        showSuccessNotification(getApplication<Application>().getString(R.string.backup_success_title))
                        onResult(true, "Backup Successful!")
                    } else {
                        _uiState.update { it.copy(isBackingUp = false) }
                        notificationManager.cancel(notificationId) // ফেইল হলে নোটিফিকেশন মুছে যাবে
                        onResult(false, "Failed to upload backup.")
                    }
                } else {
                    _uiState.update { it.copy(isBackingUp = false) }
                    notificationManager.cancel(notificationId)
                    onResult(false, "Google account not found. Please log in again.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isBackingUp = false) }
                notificationManager.cancel(notificationId)
                onResult(false, "Error: ${e.message}")
            }
        }
    }

    fun triggerRestore(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoring = true) }
            showProgressNotification("Restoring data...")

            try {
                val account = GoogleSignIn.getLastSignedInAccount(getApplication())

                if (account != null) {
                    val driveService = driveHelper.getDriveService(account)
                    val jsonString = driveHelper.downloadBackup(driveService)

                    if (jsonString != null) {
                        val isSuccess = backupManager.restoreFromJson(jsonString)

                        if (isSuccess) {
                            _uiState.update { it.copy(isRestoring = false) }
                            showSuccessNotification("Restore Successful!")
                            onResult(true, "Restore Successful! All data recovered.")
                        } else {
                            _uiState.update { it.copy(isRestoring = false) }
                            notificationManager.cancel(notificationId)
                            onResult(false, "Failed to save restored data to database.")
                        }
                    } else {
                        _uiState.update { it.copy(isRestoring = false) }
                        notificationManager.cancel(notificationId)
                        onResult(false, "No backup file found on Google Drive.")
                    }
                } else {
                    _uiState.update { it.copy(isRestoring = false) }
                    notificationManager.cancel(notificationId)
                    onResult(false, "Google account not found. Please log in again.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isRestoring = false) }
                notificationManager.cancel(notificationId)
                onResult(false, "Error: ${e.message}")
            }
        }
    }

    // --- Notification Helpers ---
    private fun showProgressNotification(title: String) {
        val notification = NotificationAppearance.apply(getApplication(), NotificationCompat.Builder(getApplication(), channelId))
            .setContentTitle(title)
            .setContentText("Please wait...")
            .setOngoing(true)
            .setProgress(0, 0, true) // Indeterminate Progress
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    private fun showSuccessNotification(title: String) {
        val notification = NotificationAppearance.apply(getApplication(), NotificationCompat.Builder(getApplication(), channelId))
            .setContentTitle(title)
            .setProgress(0, 0, false) // Progress বার রিমুভ
            .setOngoing(false) // Swipe করে কাটা যাবে
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Manual Backup/Restore",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun toggleAutoBackup(enabled: Boolean) {
        ReminderScheduler.setAutoBackupEnabled(getApplication(), enabled)
        _uiState.update { it.copy(isAutoBackupEnabled = enabled) }
    }

    fun toggleDarkMode(enabled: Boolean) {
        _uiState.update { it.copy(isDarkModeEnabled = enabled) }
    }
}
package com.almamun252.nikhuthisab.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// প্রোফাইলের সমস্ত ডেটা একসাথে রাখার জন্য Data Class
data class ProfileUiState(
    val isLoggedIn: Boolean = false,
    val userName: String = "",
    val userEmail: String = "",
    val lastBackupTime: String = "কখনো ব্যাকআপ করা হয়নি",
    val isAutoBackupEnabled: Boolean = true,
    val isDarkModeEnabled: Boolean = false,
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false
)

class ProfileViewModel : ViewModel() {

    // UI State Flow (Compose এখান থেকে ডেটা পড়বে)
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        // TODO: DataStore বা SharedPreferences থেকে ইউজারের সেভ করা সেটিংস (ডার্ক মোড, লগইন স্ট্যাটাস) লোড করতে হবে
        loadUserSettings()
    }

    private fun loadUserSettings() {
        // আপাতত ডিফল্ট কিছু ডেটা লোড করা হচ্ছে।
        // ভবিষ্যতে এখানে Firebase Auth চেক করে লগইন স্ট্যাটাস আনবেন।
    }

    // --- Google Auth Actions ---
    fun loginWithGoogle() {
        // TODO: Google Sign-In API বা Firebase Auth কল করতে হবে
        // সফল হলে নিচের মতো করে স্টেট আপডেট হবে:
        _uiState.update { currentState ->
            currentState.copy(
                isLoggedIn = true,
                userName = "আল মামুন", // গুগল থেকে পাওয়া নাম
                userEmail = "almamun252@gmail.com" // গুগল থেকে পাওয়া ইমেইল
            )
        }
    }

    fun logout() {
        // TODO: Firebase/Google Sign out কল করতে হবে
        _uiState.update { currentState ->
            currentState.copy(
                isLoggedIn = false,
                userName = "",
                userEmail = ""
            )
        }
    }

    // --- Backup & Restore Actions ---
    fun triggerBackup(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBackingUp = true) }

            // TODO: Google Drive API তে ফাইল আপলোড করার কোড এখানে বসবে
            delay(2000) // ডেমো নেটওয়ার্ক ডিলে

            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale("bn", "BD"))
            val currentTime = sdf.format(Date())

            _uiState.update { it.copy(
                isBackingUp = false,
                lastBackupTime = currentTime
            ) }
            onSuccess()
        }
    }

    fun triggerRestore(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoring = true) }

            // TODO: Google Drive থেকে ফাইল ডাউনলোড এবং লোকাল ডাটাবেস রিপ্লেস করার কোড
            delay(3000) // ডেমো ডাউনলোড ডিলে

            _uiState.update { it.copy(isRestoring = false) }
            onSuccess()
        }
    }

    // --- App Settings Actions ---
    fun toggleAutoBackup(enabled: Boolean) {
        // TODO: DataStore এ Auto Backup সেটিং সেভ করতে হবে
        _uiState.update { it.copy(isAutoBackupEnabled = enabled) }
    }

    fun toggleDarkMode(enabled: Boolean) {
        // TODO: DataStore এ Dark Mode সেটিং সেভ করতে হবে
        _uiState.update { it.copy(isDarkModeEnabled = enabled) }
    }
}
package com.almamun252.nikhuthisab.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.almamun252.nikhuthisab.R
import com.almamun252.nikhuthisab.repository.AuthRepository
import com.almamun252.nikhuthisab.repository.FirebaseAuthImpl
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
    val lastBackupTime: String = "",
    val isAutoBackupEnabled: Boolean = true,
    val isDarkModeEnabled: Boolean = false,
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false
)

// ViewModel এর বদলে AndroidViewModel ব্যবহার করা হচ্ছে যাতে Application Context পাওয়া যায়
class ProfileViewModel(
    application: Application
) : AndroidViewModel(application) {

    // আসল ইউজারের ডেটা পাওয়ার জন্য AuthRepository কল করা হলো
    private val repository: AuthRepository = FirebaseAuthImpl(application)

    // UI State Flow (Compose এখান থেকে ডেটা পড়বে)
    private val _uiState = MutableStateFlow(
        ProfileUiState(
            // ডিফল্ট স্ট্রিং সেট করা হলো
            lastBackupTime = application.getString(R.string.never_backed_up)
        )
    )
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        // অ্যাপ চালু হলেই ফায়ারবেস থেকে আসল ইউজারের ডেটা (নাম, ইমেইল) নিয়ে আসবে
        viewModelScope.launch {
            repository.currentUser.collect { user ->
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoggedIn = user != null,
                        userName = user?.displayName ?: application.getString(R.string.default_user_name),
                        userEmail = user?.email ?: application.getString(R.string.default_email)
                    )
                }
            }
        }
    }

    // --- Google Auth Actions ---
    fun logout() {
        viewModelScope.launch {
            repository.signOut()
            // State অটোমেটিক আপডেট হবে কারণ আমরা repository.currentUser.collect করছি
        }
    }

    // --- Backup & Restore Actions ---
    fun triggerBackup(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBackingUp = true) }

            // TODO: Google Drive API তে ফাইল আপলোড করার কোড এখানে বসবে
            delay(2000) // ডেমো নেটওয়ার্ক ডিলে

            // Locale.getDefault() ব্যবহার করা হয়েছে যাতে টাইম ফরম্যাট ভাষা অনুযায়ী হয়
            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
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
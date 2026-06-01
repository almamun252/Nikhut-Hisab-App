package com.almamun252.nikhuthisab.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.almamun252.nikhuthisab.R
import com.almamun252.nikhuthisab.model.AppUser
import com.almamun252.nikhuthisab.repository.AuthRepository
import com.almamun252.nikhuthisab.repository.FirebaseAuthImpl
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ViewModel এর বদলে AndroidViewModel ব্যবহার করা হচ্ছে যাতে Application Context পাওয়া যায়
class AuthViewModel(
    application: Application
) : AndroidViewModel(application) {

    // FirebaseAuthImpl এ Application Context পাস করা হলো
    private val repository: AuthRepository = FirebaseAuthImpl(application)

    private val _currentUser = MutableStateFlow<AppUser?>(null)
    val currentUser: StateFlow<AppUser?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        // অ্যাপ চালু হলেই চেক করবে কেউ লগইন করা আছে কি না
        viewModelScope.launch {
            repository.currentUser.collect { user ->
                _currentUser.value = user
            }
        }
    }

    // 🔥 Google Drive এর স্কোপ সহ SignInClient পাওয়ার ফাংশন
    fun getGoogleSignInClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getApplication<Application>().getString(R.string.default_web_client_id))
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA)) // ড্রাইভের হিডেন (App Data) ফোল্ডারের পারমিশন
            .build()

        return GoogleSignIn.getClient(getApplication<Application>(), gso)
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = repository.signInWithGoogle(idToken)
            result.onFailure { exception ->
                // ডাইনামিক স্ট্রিং ব্যবহার করে এরর মেসেজ সেট করা হলো
                _errorMessage.value = exception.message ?: getApplication<Application>().getString(R.string.msg_login_failed)
            }

            _isLoading.value = false
        }
    }

    fun signOut() {
        viewModelScope.launch {
            // গুগল থেকেও সাইন-আউট করতে চাইলে নিচের লাইনটি ব্যবহার করতে পারেন
            // getGoogleSignInClient().signOut()
            repository.signOut()
        }
    }
}
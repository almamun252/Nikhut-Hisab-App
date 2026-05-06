package com.almamun252.nikhuthisab.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.almamun252.nikhuthisab.model.AppUser
import com.almamun252.nikhuthisab.repository.AuthRepository
import com.almamun252.nikhuthisab.repository.FirebaseAuthImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository = FirebaseAuthImpl()
) : ViewModel() {

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

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = repository.signInWithGoogle(idToken)
            result.onFailure { exception ->
                _errorMessage.value = exception.message ?: "লগইন ব্যর্থ হয়েছে!"
            }

            _isLoading.value = false
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
        }
    }
}
package com.almamun252.nikhuthisab.repository

import com.almamun252.nikhuthisab.model.AppUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    // বর্তমানে লগইন করা ইউজার
    val currentUser: Flow<AppUser?>

    // গুগল লগইন ফাংশন
    suspend fun signInWithGoogle(idToken: String): Result<AppUser>

    // লগআউট ফাংশন
    suspend fun signOut()
}